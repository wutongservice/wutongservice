package com.borqs.server.platform.sql;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.util.Initializable;
import com.borqs.server.platform.util.VfsHelper;
import com.borqs.server.platform.util.json.JsonHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShardedTable implements Table, Initializable {
    private ShardMethod shardMethod;
    private List<Shard> shards;
    private String file;

    public ShardedTable() {
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public void init() throws Exception {
        String json = VfsHelper.loadText(file);
        parse0(json, this);
    }

    @Override
    public void destroy() {
        shardMethod = null;
        shards = null;
    }

    @Override
    public int getShardCount() {
        return CollectionUtils.isEmpty(shards) ? 0 : shards.size();
    }

    @Override
    public ShardResult getShard(int index) {
        return getShard0(index).getResult();
    }

    private Shard getShard0(int index) {
        if (index < 0 || index >= getShardCount())
            throw new IndexOutOfBoundsException();

        return shards.get(index);
    }


    @Override
    public ShardResult shard(Object key) {
        long n = shardMethod.shard(key);
        for (Shard shard : shards) {
            for (Segment seg : shard.segments) {
                if (seg.in(n))
                    return shard.getResult();
            }
        }
        throw new ServerException(E.SQL, "Shard error key=%s", ObjectUtils.toString(key));
    }

    public static ShardedTable parse(String json) {
        return parse0(json, null);
    }

    private static ShardedTable parse0(String json, ShardedTable reuse) {
        JsonNode jn = JsonHelper.parse(json);
        ShardMethod shardMethod = parseShardMethod(jn.path("method").getTextValue());
        JsonNode shardsNode = jn.get("shards");
        ArrayList<Shard> shards = new ArrayList<Shard>();
        for (int i = 0; i < shardsNode.size(); i++) {
            JsonNode shardNode = shardsNode.get(i);
            String db = shardNode.path("db").getTextValue();
            String table = shardNode.path("table").getTextValue();
            List<Segment> segs = parseSegments(shardNode.path("rule").getTextValue());
            shards.add(new Shard(db, table, segs));
        }
        ShardedTable shardTable = reuse != null ? reuse : new ShardedTable();
        shardTable.shardMethod = shardMethod;
        shardTable.shards = shards;
        return shardTable;
    }

    private static final Pattern VALUE_SEGMENT_PATT = Pattern.compile("^ *((\\w|-|\\+)+) *$");
    private static final Pattern OPEN_OPEN_INTERVAL_PATT = Pattern.compile("^ *\\( *((\\w|-|\\+)+) *, *((\\w|-|\\+)+) *\\)*$");
    private static final Pattern OPEN_CLOSED_INTERVAL_PATT = Pattern.compile("^ *\\( *((\\w|-|\\+)+) *, *((\\w|-|\\+)+) *\\] *$");
    private static final Pattern CLOSED_OPEN_INTERVAL_PATT = Pattern.compile("^ *\\[ *((\\w|-|\\+)+) *, *((\\w|-|\\+)+) *\\) *$");
    private static final Pattern CLOSED_CLOSED_INTERVAL_PATT = Pattern.compile("^ *\\[ *((\\w|-|\\+)+) *, *((\\w|-|\\+)+) *\\] *$");
    private static Segment parseSegment(String s) {
        Matcher m;
        if ((m = match(VALUE_SEGMENT_PATT, s)) != null) {
            String vs = m.group(1);
            return new ValueSegment(parseLong(vs));
        } else if ((m = match(OPEN_OPEN_INTERVAL_PATT, s)) != null) {
            String mins = m.group(1);
            String maxs = m.group(3);
            return new OpenOpenInterval(parseLong(mins), parseLong(maxs));
        } else if ((m = match(OPEN_CLOSED_INTERVAL_PATT, s)) != null) {
            String mins = m.group(1);
            String maxs = m.group(3);
            return new OpenClosedInterval(parseLong(mins), parseLong(maxs));
        } else if ((m = match(CLOSED_OPEN_INTERVAL_PATT, s)) != null) {
            String mins = m.group(1);
            String maxs = m.group(3);
            return new ClosedOpenInterval(parseLong(mins), parseLong(maxs));
        } else if ((m = match(CLOSED_CLOSED_INTERVAL_PATT, s)) != null) {
            String mins = m.group(1);
            String maxs = m.group(3);
            return new ClosedClosedInterval(parseLong(mins), parseLong(maxs));
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static List<Segment> parseSegments(String s) {
        ArrayList<Segment> segs = new ArrayList<Segment>();
        for (String ss : StringUtils.split(s, ';'))
            segs.add(parseSegment(ss));
        return segs;
    }

    private static long parseLong(String s) {
        return Long.parseLong(StringUtils.remove(s, "+"));
    }

    private static ShardMethod parseShardMethod(String s) {
        if (StringUtils.startsWithIgnoreCase(s, "hash")) {
            long mod = parseLong(StringUtils.removeStartIgnoreCase(s, "hash").trim());
            return new HashShard(mod);
        } else if (StringUtils.equalsIgnoreCase(StringUtils.trimToEmpty(s), "range")) {
            return new RangeShard();
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static Matcher match(Pattern patt, String s) {
        Matcher m = patt.matcher(s);
        return m.matches() ? m : null;
    }

    private static interface ShardMethod {
        long shard(Object o);
    }

    private static class HashShard implements ShardMethod {
        final long mod;

        private HashShard(long mod) {
            this.mod = mod;
        }

        @Override
        public long shard(Object o) {
            long n = ObjectUtils.hashCode(o);
            return n % mod;
        }
    }

    private static class RangeShard implements ShardMethod {
        @Override
        public long shard(Object o) {
            return ObjectUtils.hashCode(o);
        }
    }

    private static interface Segment {
        boolean in(long value);
    }

    private static abstract class Interval implements Segment {
        protected final long min;
        protected final long max;

        protected Interval(long min, long max) {
            this.min = min;
            this.max = max;
        }
    }

    private static class OpenOpenInterval extends Interval {
        private OpenOpenInterval(long min, long max) {
            super(min, max);
        }

        @Override
        public boolean in(long value) {
            return value > min && value < max;
        }

        @Override
        public String toString() {
            return String.format("(%s, %s)", min, max);
        }
    }

    private static class OpenClosedInterval extends Interval {
        private OpenClosedInterval(long min, long max) {
            super(min, max);
        }

        @Override
        public boolean in(long value) {
            return value > min && value <= max;
        }

        @Override
        public String toString() {
            return String.format("(%s, %s]", min, max);
        }
    }

    private static class ClosedOpenInterval extends Interval {
        private ClosedOpenInterval(long min, long max) {
            super(min, max);
        }

        @Override
        public boolean in(long value) {
            return value >= min && value < max;
        }

        @Override
        public String toString() {
            return String.format("[%s, %s)", min, max);
        }
    }

    private static class ClosedClosedInterval extends Interval {
        private ClosedClosedInterval(long min, long max) {
            super(min, max);
        }

        @Override
        public boolean in(long value) {
            return value >= min && value <= max;
        }

        @Override
        public String toString() {
            return String.format("[%s, %s]", min, max);
        }
    }

    private static class ValueSegment implements Segment {
        final long value;

        private ValueSegment(long value) {
            this.value = value;
        }

        @Override
        public boolean in(long value) {
            return this.value == value;
        }

        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    private static class Shard {
        final String db;
        final String table;
        final List<Segment> segments;

        private Shard(String db, String table, List<Segment> segments) {
            this.db = db;
            this.table = table;
            this.segments = segments;
        }

        public ShardResult getResult() {
            return new ShardResult(db, table);
        }
    }
}
