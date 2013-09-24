package com.borqs.server.base.data;

import com.borqs.server.base.io.TextLoader;
import com.borqs.server.base.util.Copyable;
import com.borqs.server.base.util.ImmutableNamed;
import com.borqs.server.base.util.json.JsonGenerateHandler;
import com.borqs.server.base.util.json.JsonUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;

import java.io.IOException;
import java.util.*;

import static com.borqs.server.base.data.DataType.*;


public class Schema extends ImmutableNamed implements Copyable<Schema> {
    public static final String NO_GROUP = "_no_group_";

    private final Map<String, Column> columns = new LinkedHashMap<String, Column>();
    private final Map<String, Set<String>> groups = new LinkedHashMap<String, Set<String>>();

    public Schema(String name) {
        super(name);
    }

    public boolean isEmpty() {
        return columns.isEmpty();
    }

    public boolean has(String col) {
        return columns.containsKey(col);
    }

    public boolean hasColumnInGroup(String col, String group) {
        Set<String> cols = groups.get(group);
        return cols != null && cols.contains(col);
    }

    public Column getColumn(String col) {
        return columns.get(col);
    }

    public Map<String, String> getAliases(String group) {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<String, String>();
        Set<String> cols = groups.get(group);
        for (String col : cols) {
            Column c = getColumn(col);
            aliases.put(c.getName(), c.getAlias());
        }
        return aliases;
    }

    public Map<String, String> getAllAliases() {
        LinkedHashMap<String, String> aliases = new LinkedHashMap<String, String>();
        for (Column c : columns.values()) {
            aliases.put(c.getName(), c.getAlias());
        }
        return aliases;
    }



    public Column addColumn(String col, DataType type) {
        return addColumn(col, type, null);
    }

    public Column addColumn(String col, DataType type, String alias) {
        Validate.isTrue(!has(col));
        Column c = new Column(col, type, alias);
        columns.put(col, c);
        return c;
    }

    public void removeColumn(String col) {
        columns.remove(col);
    }

    public void clearColumns() {
        columns.clear();
    }

    public void removeGroup(String group) {
        groups.remove(group);
    }

    public void clearGroups() {
        groups.clear();
    }

    public List<String> getNames() {
        return new ArrayList<String>(columns.keySet());
    }

    public DataType getType(String col) {
        Column c = getColumn(col);
        return c != null ? c.getType() : null;
    }

    public String getAlias(String col) {
        Column c = getColumn(col);
        return c != null ? c.getAlias() : null;
    }

    public Schema setAlias(String col, String alias) {
        Column c = getColumn(col);
        if (c != null)
            c.setAlias(alias);
        return this;
    }

    public String[] getGroupNames() {
        Set<String> s = groups.keySet();
        return s.toArray(new String[s.size()]);
    }

    public String[] getColumnsGroups(String... cols) {
        if (cols == null)
            return new String[0];

        LinkedHashSet<String> gs = new LinkedHashSet<String>();
        for (Map.Entry<String, Set<String>> e : groups.entrySet()) {
            for (String col : cols) {
                if (e.getValue().contains(col))
                    gs.add(e.getKey());
            }
        }
        return gs.toArray(new String[gs.size()]);
    }

    public Schema setGroup(String col, String group) {
        if (group == null)
            return this;

        Validate.isTrue(columns.containsKey(col));
        Set<String> g = groups.get(group);
        if (g == null) {
            g = new LinkedHashSet<String>();
            groups.put(group, g);
        }
        g.add(col);
        return this;
    }

    public String[] getColumnNames() {
        ArrayList<String> l = new ArrayList<String>();
        for (Column c : columns.values())
            l.add(c.getName());
        return l.toArray(new String[l.size()]);
    }

    public String[] getColumnNames(String group) {
        Set<String> cols = groups.get(group);
        return cols != null ? cols.toArray(new String[cols.size()]) : new String[0];
    }

    @SuppressWarnings("unchecked")
    public String[] getColumnNamesIn(String group, String... cols) {
        String[] groupCols = getColumnNames(group);
        Collection<String> c = CollectionUtils.intersection(Arrays.asList(groupCols), Arrays.asList(cols));
        return c.toArray(new String[c.size()]);
    }

    public static Schema parse(String json) {
        JsonNode jn = JsonUtils.parse(json);
        String name = jn.path("name").getTextValue();
        Schema schema = new Schema(name);
        JsonNode cols = jn.path("columns");
        Iterator<Map.Entry<String, JsonNode>> iter = cols.getFields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> e = iter.next();
            String cn = e.getKey();
            JsonNode col = e.getValue();
            if (col.isObject()) {
                String type = col.path("type").getTextValue().trim().toUpperCase();
                String alias = col.has("alias") ? col.path("alias").getTextValue().trim() : null;
                schema.addColumn(cn, DataType.valueOf(type), alias);
            } else {
                String type = col.getValueAsText().trim().toUpperCase();
                schema.addColumn(cn, DataType.valueOf(type));
            }
        }

        JsonNode groups = jn.path("groups");
        if (groups.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> iter2 = groups.getFields();
            while (iter2.hasNext()) {
                Map.Entry<String, JsonNode> e = iter2.next();
                String gn = e.getKey();
                JsonNode gv = e.getValue();
                List<String> l = new ArrayList<String>();
                if (gv.isArray()) {
                    for (int i = 0; i < gv.size(); i++)
                        l.add(gv.get(i).getTextValue());
                } else if (gv.isTextual()) {
                    l = Arrays.asList(StringUtils.split(gv.getTextValue(), ','));
                }
                for (String col : l) {
                    schema.setGroup(col.trim(), gn);
                }
            }
        }
        return schema;
    }

    public static Schema load(String path) {
        return parse(TextLoader.load(path));
    }

    public static Schema loadClassPath(Class clazz, String f) {
        return parse(TextLoader.loadClassPath(clazz, f));
    }

    public Schema loadAliases(String json) {
        if (json == null)
            return this;

        JsonNode jn = JsonUtils.parse(json);

        Iterator<Map.Entry<String, JsonNode>> iter = jn.getFields();
        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> e = iter.next();
            String col = e.getKey().trim();
            String groupsAndAlias = e.getValue().getTextValue().trim();
            if (StringUtils.contains(groupsAndAlias, '@')) {
                String alias = StringUtils.substringBefore(groupsAndAlias, "@");
                String groups = StringUtils.substringAfter(groupsAndAlias, "@");
                if (StringUtils.isNotBlank(alias)) {
                    setAlias(col, alias.trim());
                }

                for (String group : StringUtils.split(groups, ',')) {
                    setGroup(col, group.trim());
                }

            } else {
                setAlias(col, groupsAndAlias);
            }
        }

        return this;
    }

    public Schema appendAliases(Class clazz, String f) {
        return f != null ? loadAliases(TextLoader.loadClassPath(clazz, f)) : null;
    }

    public Schema appendAliases(String path) {
        return path != null ? loadAliases(TextLoader.load(path)) : this;
    }

    @Override
    public String toString() {
        return JsonUtils.toJson(new JsonGenerateHandler() {
            @Override
            public void generate(JsonGenerator jg) throws IOException {
                jg.writeStartObject();
                jg.writeStringField("name", name);
                jg.writeFieldName("columns");
                jg.writeStartObject();
                for (Column c : columns.values()) {
                    jg.writeFieldName(c.getName());
                    if (c.aliasExists()) {
                        jg.writeStartObject();
                        jg.writeStringField("type", c.getType().toString().toLowerCase());
                        jg.writeStringField("alias", c.getAlias());
                        jg.writeEndObject();
                    } else {
                        jg.writeString(c.getType().toString().toLowerCase());
                    }
                }
                jg.writeEndObject();

                if (!groups.isEmpty()) {
                    jg.writeFieldName("groups");
                    jg.writeStartObject();
                    for (Map.Entry<String, Set<String>> e : groups.entrySet()) {
                        jg.writeFieldName(e.getKey());
                        jg.writeStartArray();
                        for (String col : e.getValue()) {
                            jg.writeString(col);
                        }
                        jg.writeEndArray();
                    }
                    jg.writeEndObject();
                }

                jg.writeEndObject();
            }
        }, true);
    }

    @Override
    public Schema copy() {
        Schema s = new Schema(this.name);
        for (Map.Entry<String, Column> e : columns.entrySet()) {
            s.columns.put(e.getKey(), e.getValue().copy());
        }
        return s;
    }

    public String toDocument() {
        StringBuilder buff = new StringBuilder();
        buff.append(String.format("<schema name=\"%s\">\n", StringEscapeUtils.escapeXml(name)));
        buff.append("  <description></description>\n");
        buff.append("  <columns>\n");
        for (Column c : columns.values()) {
            buff.append(String.format("    <column name=\"%s\" type=\"%s\"></column>\n", StringEscapeUtils.escapeXml(c.getName()), StringEscapeUtils.escapeXml(c.getType().toString().toLowerCase())));
        }
        buff.append("  </columns>\n");
        buff.append("</schema>\n");
        return buff.toString();
    }

    public static class Column extends ImmutableNamed implements Copyable<Column> {
        private final DataType type;
        private String alias;


        private Column(String name, DataType type, String alias) {
            super(name);
            Validate.notNull(type);
            Validate.isTrue(ArrayUtils.contains(new Object[]{BOOLEAN, INT, FLOAT, STRING, JSON}, type));
            this.type = type;
            this.alias = alias != null ? alias : name;
        }

        public DataType getType() {
            return type;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias != null ? alias : this.name;
        }

        public boolean aliasExists() {
            return !StringUtils.equals(name, alias);
        }

        @Override
        public Column copy() {
            return new Column(this.name, this.type, this.alias);
        }
    }
}
