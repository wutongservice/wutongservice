package com.borqs.server.platform.sql;


import com.borqs.server.ServerException;
import com.borqs.server.platform.E;
import com.borqs.server.platform.data.Page;
import com.borqs.server.platform.util.CollectionsHelper;
import com.borqs.server.platform.util.StringHelper;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.CharUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class Sql {
    private final StringBuilder buffer = new StringBuilder();

    public Sql() {
    }

    private Sql(String s) {
        buffer.append(s);
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

    public static Sql union(Sql... sqls) {
        return union0(false, Arrays.asList(sqls));
    }

    public static Sql union(Collection<?> sqls) {
        return union0(false, sqls);
    }

    public static Sql unionAll(Sql... sqls) {
        return union0(true, Arrays.asList(sqls));
    }

    public static Sql unionAll(Collection<?> sqls) {
        return union0(true, sqls);
    }

    public static Sql union0(boolean all, Collection<?> sqls) {
        Sql sql = new Sql();
        if (!CollectionUtils.isEmpty(sqls)) {
            int n = 0;
            for (Object o : sqls) {
                if (n > 0)
                    sql.append(all ? " UNION ALL " : " UNION ");

                sql.append("(");
                sql.append(o);
                sql.append(")");
                n++;
            }
        }
        return sql;
    }


    private StringBuilder append0(Object o) {
        buffer.append(o);
        return buffer;
    }

    public Sql select(Object... fields) {
        return select(Arrays.asList(fields));
    }

    public Sql select(Collection<Object> fields) {
        append0("SELECT ");
        int i = 0;
        for (Object o : fields) {
            String field = ObjectUtils.toString(o, null);
            if (StringUtils.isBlank(field))
                continue;

            if (i > 0)
                append0(", ");

            append0(field);
            i++;
        }
        append0(" ");
        return this;
    }

    public Sql useIndex(Object... indexes) {
        return useIndex(Arrays.asList(indexes));
    }

    public Sql useIndex(Collection<Object> indexes) {
        if (CollectionUtils.isNotEmpty(indexes)) {
            append0("USE INDEX (");
            int i = 0;
            for (Object o : indexes) {
                String index = ObjectUtils.toString(o, null);
                if (StringUtils.isBlank(index))
                    continue;

                if (i > 0)
                    append0(", ");

                append0(index);
                i++;
            }
            append0(") ");
        }
        return this;
    }

    public Sql from(Object table) {
        append0("FROM ").append(table).append(" ");
        return this;
    }

    public Sql from(Object table1, Object table2) {
        append0("FROM ").append(table1).append(", ").append(table2).append(" ");
        return this;
    }

    public Sql from(Object... tables) {
        return from(Arrays.asList(tables));
    }

    public Sql from(Collection<?> tables) {
        append0("FROM ").append(StringUtils.join(tables, ", ")).append(" ");
        return this;
    }

    public Sql where(Object cond) {
        append0("WHERE (").append(cond).append(") ");
        return this;
    }

    public Sql where(Object cond, Map<String, Object> args) {
        return where(format0(ObjectUtils.toString(cond), args));
    }

    public Sql where(Object cond, String a1, Object v1) {
        return where(cond, CollectionsHelper.of(a1, v1));
    }

    public Sql where(Object cond, String a1, Object v1, String a2, Object v2) {
        return where(cond, CollectionsHelper.of(a1, v1, a2, v2));
    }

    public Sql where(Object cond, String a1, Object v1, String a2, Object v2, String a3, Object v3) {
        return where(cond, CollectionsHelper.of(a1, v1, a2, v2, a3, v3));
    }

    public Sql where(Object cond, String a1, Object v1, String a2, Object v2, String a3, Object v3, String a4, Object v4) {
        return where(cond, CollectionsHelper.of(a1, v1, a2, v2, a3, v3, a4, v4));
    }

    public Sql and(Object cond) {
        append0("AND (").append(cond).append(") ");
        return this;
    }

    public Sql and(Object cond, Map<String, Object> args) {
        return and(format0(ObjectUtils.toString(cond), args));
    }

    public Sql and(Object cond, String a1, Object v1) {
        return and(cond, CollectionsHelper.of(a1, v1));
    }

    public Sql and(Object cond, String a1, Object v1, String a2, Object v2) {
        return and(cond, CollectionsHelper.of(a1, v1, a2, v2));
    }

    public Sql and(Object cond, String a1, Object v1, String a2, Object v2, String a3, Object v3) {
        return and(cond, CollectionsHelper.of(a1, v1, a2, v2, a3, v3));
    }

    public Sql and(Object cond, String a1, Object v1, String a2, Object v2, String a3, Object v3, String a4, Object v4) {
        return and(cond, CollectionsHelper.of(a1, v1, a2, v2, a3, v3, a4, v4));
    }

    public Sql orderBy(Object f) {
        return orderBy(f, "ASC");
    }

    public Sql orderBy(Object f, Object order) {
        append0("ORDER BY ").append(f).append(" ").append(order).append(" ");
        return this;
    }

    public Sql groupBy(Object f) {
        append0("GROUP BY ").append(f).append(" ");
        return this;
    }

    public Sql limit(long count) {
        append0("LIMIT ");
        append0(count).append(" ");
        return this;
    }

    public Sql limit(long offset, long count) {
        append0("LIMIT ").append(offset).append(", ").append(count).append(" ");
        return this;
    }

    public Sql page(long page, long count) {
        return limit(pageToOffset(page, count), count);
    }

    public Sql page(Page page) {
        return page != null ? page(page.page, page.count) : this;
    }

    public Sql insertInto(Object table) {
        append0("INSERT INTO ").append(table).append(" ");
        return this;
    }

    public Sql replaceInto(Object table) {
        append0("REPLACE INTO ").append(table).append(" ");
        return this;
    }

    public Sql insertIgnoreInto(Object table) {
        append0("INSERT IGNORE INTO ").append(table).append(" ");
        return this;
    }

    public Sql deleteFrom(Object table) {
        append0("DELETE FROM ").append(table).append(" ");
        return this;
    }

    public Sql update(Object table) {
        append0("UPDATE ").append(table).append(" ");
        return this;
    }

    public Sql update() {
        append0(" UPDATE ");
        return this;
    }

    public Sql onDuplicateKey() {
        append0(" ON DUPLICATE KEY ");
        return this;
    }

    public Sql values(ValuePair... values) {
        return values(Arrays.asList(values));
    }

    public Sql values(Collection<ValuePair> values) {
        Validate.notEmpty(values);
        StringBuilder fieldsBuff = new StringBuilder();
        StringBuilder valuesBuff = new StringBuilder();
        int i = 0;
        for (ValuePair value : values) {
            if (value == null)
                continue;

            if (i > 0) {
                fieldsBuff.append(", ");
                valuesBuff.append(", ");
            }

            fieldsBuff.append(value.field);
            valuesBuff.append(sqlValue(value.value));

            i++;
        }
        buffer.append("(").append(fieldsBuff).append(") VALUES (").append(valuesBuff).append(") ");
        return this;
    }

    public Sql pairValues(ValuePair... values) {
        return pairValues(Arrays.asList(values));
    }

    public Sql setValues(ValuePair... values) {
        return setValues(Arrays.asList(values));
    }

    private Sql pairValues(Collection<ValuePair> values) {
        Validate.notEmpty(values);
        int i = 0;
        for (ValuePair value : values) {
            if (value == null)
                continue;

            if (i > 0)
                append0(", ");

            append0(value.field).append("=").append(sqlValue(value.value));
            i++;
        }
        append0(" ");
        return this;
    }

    public Sql setValues(Collection<ValuePair> values) {
        append0("SET ");
        return pairValues(values);
    }

    public Sql append(Object v) {
        append0(ObjectUtils.toString(v));
        return this;
    }

    public Sql append(Object format, String a1, Object v1) {
        return append(format0(format, CollectionsHelper.of(a1, v1)));
    }

    public Sql append(Object format, String a1, Object v1, String a2, Object v2) {
        return append(format0(format, CollectionsHelper.of(a1, v1, a2, v2)));
    }

    public Sql append(Object format, String a1, Object v1, String a2, Object v2, String a3, Object v3) {
        return append(format0(format, CollectionsHelper.of(a1, v1, a2, v2, a3, v3)));
    }

    public Sql append(Object format, String a1, Object v1, String a2, Object v2, String a3, Object v3, String a4, Object v4) {
        return append(format0(format, CollectionsHelper.of(a1, v1, a2, v2, a3, v3, a4, v4)));
    }

    public Sql append(Object format, String a1, Object v1, String a2, Object v2, String a3, Object v3, String a4, Object v4, String a5, Object v5) {
        return append(format0(format, CollectionsHelper.of(a1, v1, a2, v2, a3, v3, a4, v4, a5, v5)));
    }

    public Sql append(Object format, Object[][] args) {
        return append(format0(format, CollectionsHelper.arraysToMap(args)));
    }

    public static Sql format(Object format, String a1, Object v1) {
        return new Sql(format0(format, CollectionsHelper.of(a1, v1)));
    }

    public static Sql format(Object format, String a1, Object v1, String a2, Object v2) {
        return new Sql(format0(format, CollectionsHelper.of(a1, v1, a2, v2)));
    }

    public static Sql format(Object format, String a1, Object v1, String a2, Object v2, String a3, Object v3) {
        return new Sql(format0(format, CollectionsHelper.of(a1, v1, a2, v2, a3, v3)));
    }

    public static Sql format(Object format, String a1, Object v1, String a2, Object v2, String a3, Object v3, String a4, Object v4) {
        return new Sql(format0(format, CollectionsHelper.of(a1, v1, a2, v2, a3, v3, a4, v4)));
    }

    public static Sql format(Object format, String a1, Object v1, String a2, Object v2, String a3, Object v3, String a4, Object v4, String a5, Object v5) {
        return new Sql(format0(format, CollectionsHelper.of(a1, v1, a2, v2, a3, v3, a4, v4, a5, v5)));
    }

    public static Sql format(Object format, Object[][] args) {
        return new Sql(format0(format, CollectionsHelper.arraysToMap(args)));
    }

    public static String format0(Object format, Map<String, Object> args) {
        Validate.notNull(format);
        Validate.notNull(args);

        String formatStr = ObjectUtils.toString(format);
        StringBuilder buff = new StringBuilder();
        StringBuilder argNameBuff = new StringBuilder();

        int s = 0; // 0 -> plain, 1 -> read ':', 2 -> read '$', 3 -> reading arg name
        int len = formatStr.length();
        for (int i = 0; i < len; i++) {
            char c = formatStr.charAt(i);
            switch (s) {
                case 0: {
                    if (c == ':') {
                        s = 1;
                    } else if (c == '$') {
                        s = 2;
                    } else {
                        buff.append(c);
                    }
                }
                break;

                case 1: {
                    if (c == ':') {
                        buff.append(c);
                        s = 0;
                    } else {
                        argNameBuff.append(':').append(c);
                        s = 3;
                    }
                }
                break;

                case 2: {
                    if (c == '$') {
                        buff.append(c);
                        s = 0;
                    } else {
                        argNameBuff.append('$').append(c);
                        s = 3;
                    }
                }
                break;

                case 3: {
                    if (CharUtils.isAsciiAlphanumeric(c) || c == '_') {
                        argNameBuff.append(c);
                    } else {
                        appendArg(buff, argNameBuff, args);
                        buff.append(c);
                        s = 0;
                    }
                }
                break;

            }
        }

        if (s != 0) {
            if (s == 3 && argNameBuff.length() > 0)
                appendArg(buff, argNameBuff, args);
            else
                throw new ServerException(E.SQL, "Unclosed SQL template");
        }

        return buff.toString();
    }


    private static void appendArg(StringBuilder buff, StringBuilder argNameBuff, Map<String, Object> args) {
        if (argNameBuff.charAt(0) == ':') {
            String argName = argNameBuff.substring(1);
            buff.append(sqlValue(args.get(argName)));
        } else if (argNameBuff.charAt(0) == '$') {
            String argName = argNameBuff.substring(1);
            buff.append(ObjectUtils.toString(args.get(argName)));
        }
        argNameBuff.setLength(0);
    }

    public static String sqlValue(Object o) {
        if (o == null) {
            return "null";
        } if (o instanceof CharSequence || o instanceof JsonNode) {
            return "'" + sqlStringEscape(o.toString()) + "'";
        } else if (o instanceof Raw) {
            return ObjectUtils.toString(((Raw) o).text);
        } else {
            return ObjectUtils.toString(o, "null");
        }
    }

    public static String joinSqlValues(Object[] objs, String sep) {
        return joinSqlValues(Arrays.asList(objs), sep);
    }

    public static String joinSqlValues(Collection<?> objs, String sep) {
        if (CollectionUtils.isEmpty(objs))
            return "";

        if (sep == null)
            sep = "";

        StringBuilder buff = new StringBuilder();
        int i = 0;
        for (Object o : objs) {
            if (i > 0)
                buff.append(sep);

            buff.append(sqlValue(o));
            i++;
        }
        return buff.toString();
    }

    public static long pageToOffset(long page, long count) {
        return page * count;
    }

    public static String sqlStringEscape(String s) {
        if (s == null)
            return null;

        StringBuilder buff = new StringBuilder();
        int len = s.length();
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            switch (c) {
                case 0:
                    buff.append("\\0");
                    break;
                case '\'':
                    buff.append("\\\'");
                    break;
                case '\"':
                    buff.append("\\\"");
                    break;
                case '\b':
                    buff.append("\\\b");
                    break;
                case '\n':
                    buff.append("\\\n");
                    break;
                case '\r':
                    buff.append("\\\r");
                    break;
                case '\t':
                    buff.append("\\\t");
                    break;
                case 26:
                    buff.append("\\Z");
                    break;
                case '\\':
                    buff.append("\\\\");
                    break;
                //case '%': buff.append0("\\%"); break;
                //case '_': buff.append0("\\_"); break;
                default:
                    buff.append(c);
            }
        }
        return buff.toString();
    }

    public static Field field(String field) {
        return field(field, (String) null);
    }

    public static Field field(String field, String as) {
        return new Field(field, as);
    }

    public static Field fieldIf(String field, boolean pred) {
        return pred ? field(field) : null;
    }

    public static Field fieldIf(String field, String as, boolean pred) {
        return pred ? field(field, as != null ? as : null) : null;
    }

    public static class Field {
        public final String field;
        public final String as;

        public Field(String field, String as) {
            Validate.isTrue(StringUtils.isNotBlank(field));
            this.field = field;
            this.as = as;
        }

        @Override
        public String toString() {
            return (StringUtils.isBlank(as) || StringUtils.equals(field, as))
                    ? field
                    : StringHelper.join(new Object[]{field, " AS ", sqlValue(as)});
        }
    }


    public static ValuePair value(String field, Object value) {
        return new ValuePair(field, value);
    }

    public static ValuePair valueIf(String field, Object value, boolean pred) {
        return pred ? value(field, value) : null;
    }

    public static Raw raw(String text) {
        return new Raw(text);
    }

    public static class ValuePair {
        public final String field;
        public final Object value;

        public ValuePair(String field, Object value) {
            Validate.isTrue(StringUtils.isNotBlank(field));
            this.field = field;
            this.value = value;
        }

        @Override
        public String toString() {
            return StringHelper.join(new Object[]{field, "=", sqlValue(value)});
        }
    }

    public static class Raw {
        public final Object text;

        public Raw(Object text) {
            this.text = text;
        }
    }


    public static class Entry {
        public final Object sql;
        public Object tag;
        public long count = 0;

        public Entry(Object sql, Object tag) {
            this.sql = sql;
            this.tag = tag;
        }

        @Override
        public String toString() {
            return ObjectUtils.toString(sql);
        }
    }

    public static Entry entry(Object sql, Object tag) {
        return new Entry(sql, tag);
    }

    public static Entry entry(Object sql) {
        return new Entry(sql, null);
    }
}
