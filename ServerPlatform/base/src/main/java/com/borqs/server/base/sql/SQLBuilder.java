package com.borqs.server.base.sql;


import com.borqs.server.base.data.Record;
import com.borqs.server.base.data.Schema;
import com.borqs.server.base.util.CollectionUtils2;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SQLBuilder {
    protected final Schema schema;

    public SQLBuilder(Schema schema) {
        this.schema = schema;
    }

    public Schema getSchema() {
        return schema;
    }

    public boolean hasSchema() {
        return schema != null;
    }

    protected String getAlias(String col) {
        return schema != null ? schema.getAlias(col) : col;
    }

    protected Map<String, String> getAllAliases() {
        return schema != null ? schema.getAllAliases() : new HashMap<String, String>();
    }

    protected boolean columnInGroup(String col, String group) {
        return schema != null && schema.hasColumnInGroup(col, group);
    }

    public static String forInsert(Schema schema, String table, Record rec) {
        return new Insert(schema).insertInto(table).values(rec).toString();
    }

    public static String forInsert(String table, Record rec) {
        return forInsert(null, table, rec);
    }

    public static String forInsertIgnore(Schema schema, String table, Record rec) {
        return new Insert(schema).insertIgnoreInto(table).values(rec).toString();
    }

    public static String forInsertIgnore(String table, Record rec) {
        return forInsertIgnore(null, table, rec);
    }

    public static String forReplace(Schema schema, String table, Record rec) {
        return new Replace(schema).replaceInto(table).values(rec).toString();
    }

    public static String forReplace(String table, Record rec) {
        return forReplace(null, table, rec);
    }

    public static class Select extends SQLBuilder {
        protected final StringBuilder buffer = new StringBuilder();

        public Select() {
            this(null);
        }

        public Select(Schema schema) {
            super(schema);
        }

        @Override
        public String toString() {
            return buffer.toString();
        }

        public Select append(String text) {
            buffer.append(text);
            return this;
        }

        public Select select(String... cols) {
            return selectWithPrefix(null, cols);
        }

        public Select select(Collection<String> cols) {
            return select(cols.toArray(new String[cols.size()]));
        }

        public Select selectWithPrefix(Map<String, String> groupPrefixes, String... cols) {
            buffer.append("SELECT ");
            Set<String> groups = groupPrefixes != null ? groupPrefixes.keySet() : null;
            for (int i = 0; i < cols.length; i++) {
                String col = cols[i];
                if (i > 0) {
                    buffer.append(", ");
                }
                String alias = getAlias(col);
                if (groups != null) {
                    for (String group : groups) {
                        if (columnInGroup(col, group)) {
                            alias = groupPrefixes.get(group) + alias;
                            break;
                        }
                    }
                }
                if (StringUtils.equals(alias, col)) {
                    buffer.append(col);
                } else {
                    buffer.append(alias).append(" AS ").append(SQLUtils.toSql(col));
                }
            }
            return this;
        }

        public Select selectDirect(String text) {
            buffer.append("SELECT ").append(text).append(" ");
            return this;
        }

        public Select selectCount() {
            buffer.append("SELECT COUNT(*) ");
            return this;
        }

        public Select from(String table) {
            buffer.append(" FROM ").append(table);
            return this;
        }

        public Select from(String... tables) {
            buffer.append(" FROM ").append(StringUtils.join(tables, ","));
            return this;
        }

        public Select where(String s) {
            buffer.append(" WHERE (").append(s).append(")");
            return this;
        }

        public Select where(String template, String k1, Object v1) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Select where(String template, String k1, Object v1, String k2, Object v2) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Select where(String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Select where(String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return where(SQLTemplate.merge(template, m));
        }

        public Select where(String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return where(SQLTemplate.merge(template, m));
        }

        public Select and(String s) {
            buffer.append(" AND (").append(SQLTemplate.merge(s, "alias", getAllAliases())).append(")");
            return this;
        }

        public Select and(String template, String k1, Object v1) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Select and(String template, String k1, Object v1, String k2, Object v2) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Select and(String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Select and(String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return and(SQLTemplate.merge(template, m));
        }

        public Select and(String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return and(SQLTemplate.merge(template, m));
        }



        public Select andIf(boolean b, String s) {
            if (b) {
                and(s);
            }
            return this;
        }

        public Select andIf(boolean b, String template, String k1, Object v1) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Select andIf(boolean b, String template, String k1, Object v1, String k2, Object v2) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Select andIf(boolean b, String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Select andIf(boolean b, String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return andIf(b, SQLTemplate.merge(template, m));
        }

        public Select andIf(boolean b, String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return andIf(b, SQLTemplate.merge(template, m));
        }


        public Select orderBy(String col, String ascOrDesc) {
            buffer.append(" ORDER BY ").append(getAlias(col)).append(" ").append(ascOrDesc);
            return this;
        }

        public Select limitByPage(int page, int count) {
            return limit(SQLUtils.pageToOffset(page, count), count);
        }

        public Select limit(int offset, int count) {
            buffer.append(" LIMIT ").append(offset).append(", ").append(count);
            return this;
        }

        public Select limit(int count) {
            buffer.append(" LIMIT ").append(count);
            return this;
        }

        public Select page(int page, int count) {
            buffer.append(" ").append(SQLUtils.pageToLimit(page, count));
            return this;
        }

        public Select groupBy(String col) {
            buffer.append(" GROUP BY ").append(getAlias(col));
            return this;
        }
    }

    public static class Insert extends SQLBuilder {
        private String table = "";
        private boolean ignore = false;
        private final StringBuilder fieldsBuffer = new StringBuilder();
        private final StringBuilder valuesBuffer = new StringBuilder();

        public Insert() {
            this(null);
        }

        public Insert(Schema schema) {
            super(schema);
        }

        @Override
        public String toString() {
            return String.format("INSERT %s INTO %s (%s) VALUES (%s)", ignore ? "IGNORE" : "", table, fieldsBuffer, valuesBuffer);
        }

        public Insert insertInto(String table) {
            this.table = table;
            this.ignore = false;
            return this;
        }

        public Insert insertIgnoreInto(String table) {
            this.table = table;
            this.ignore = true;
            return this;
        }

        public Insert values(Map<String, Object> values, String... excludedCols) {
            for (Map.Entry<String, Object> e : values.entrySet()) {
                if (!ArrayUtils.contains(excludedCols, e.getKey())) {
                    value(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        public Insert value(String col, Object val) {
            String alias = getAlias(col);
            return directValue(alias, val);
        }

        public Insert directValue(String field, Object val) {
            if (fieldsBuffer.length() > 0)
                fieldsBuffer.append(", ");
            if (valuesBuffer.length() > 0)
                valuesBuffer.append(", ");

            fieldsBuffer.append(field);
            valuesBuffer.append(SQLUtils.toSql(val));
            return this;
        }
    }

    public static class Replace extends SQLBuilder {
        private String table = "";
        private final StringBuilder fieldsBuffer = new StringBuilder();
        private final StringBuilder valuesBuffer = new StringBuilder();

        public Replace() {
            this(null);
        }

        public Replace(Schema schema) {
            super(schema);
        }

        @Override
        public String toString() {
            return String.format("REPLACE INTO %s (%s) VALUES (%s)", table, fieldsBuffer, valuesBuffer);
        }

        public Replace replaceInto(String table) {
            this.table = table;
            return this;
        }

        public Replace values(Map<String, Object> values, String... excludedCols) {
            for (Map.Entry<String, Object> e : values.entrySet()) {
                if (!ArrayUtils.contains(excludedCols, e.getKey())) {
                    value(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        public Replace value(String col, Object val) {
            String alias = getAlias(col);
            return directValue(alias, val);
        }

        public Replace directValue(String field, Object val) {
            if (fieldsBuffer.length() > 0)
                fieldsBuffer.append(", ");
            if (valuesBuffer.length() > 0)
                valuesBuffer.append(", ");

            fieldsBuffer.append(field);
            valuesBuffer.append(SQLUtils.toSql(val));
            return this;
        }
    }

    public static class Update extends SQLBuilder {
        private String table;
        private final StringBuilder valuesBuffer = new StringBuilder();
        private final StringBuilder whereBuffer = new StringBuilder();

        public Update() {
            this(null);
        }

        public Update(Schema schema) {
            super(schema);
        }

        @Override
        public String toString() {
            return String.format("UPDATE %s SET %s %s", table, valuesBuffer, whereBuffer);
        }

        public Update append(String text) {
            whereBuffer.append(text);
            return this;
        }

        public Update update(String table) {
            this.table = table;
            return this;
        }

        public Update values(Map<String, Object> values, String... excludedCols) {
            for (Map.Entry<String, Object> e : values.entrySet()) {
                if (!ArrayUtils.contains(excludedCols, e.getKey())) {
                    value(e.getKey(), e.getValue());
                }
            }
            return this;
        }

        public Update value(String col, Object val) {
            String alias = getAlias(col);
            return directValue(alias, val);
        }

        public Update directValue(String field, Object val) {
            if (valuesBuffer.length() > 0)
                valuesBuffer.append(", ");

            valuesBuffer.append(field).append("=").append(SQLUtils.toSql(val));
            return this;
        }

        public Update valueIf(boolean b, String col, Object val) {
            if (b) {
                value(col, val);
            }
            return this;
        }

        public Update directValueIf(boolean b, String field, Object val) {
            if (b) {
                directValue(field, val);
            }
            return this;
        }

        public Update where(String s) {
            whereBuffer.append(" WHERE (").append(s).append(")");
            return this;
        }

        public Update where(String template, String k1, Object v1) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Update where(String template, String k1, Object v1, String k2, Object v2) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Update where(String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Update where(String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return where(SQLTemplate.merge(template, m));
        }

        public Update where(String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return where(SQLTemplate.merge(template, m));
        }

        public Update and(String s) {
            whereBuffer.append(" AND (").append(s).append(")");
            return this;
        }

        public Update and(String template, String k1, Object v1) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Update and(String template, String k1, Object v1, String k2, Object v2) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Update and(String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Update and(String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return and(SQLTemplate.merge(template, m));
        }

        public Update and(String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return and(SQLTemplate.merge(template, m));
        }



        public Update andIf(boolean b, String s) {
            if (b) {
                and(s);
            }
            return this;
        }

        public Update andIf(boolean b, String template, String k1, Object v1) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Update andIf(boolean b, String template, String k1, Object v1, String k2, Object v2) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Update andIf(boolean b, String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Update andIf(boolean b, String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return andIf(b, SQLTemplate.merge(template, m));
        }

        public Update andIf(boolean b, String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return andIf(b, SQLTemplate.merge(template, m));
        }
    }

    public static class Delete extends SQLBuilder {
        private final StringBuilder buffer = new StringBuilder();

        public Delete() {
            this(null);
        }

        public Delete(Schema schema) {
            super(schema);
        }

        @Override
        public String toString() {
            return buffer.toString();
        }

        public Delete append(String text) {
            buffer.append(text);
            return this;
        }

        public Delete deleteFrom(String table) {
            buffer.append("DELETE FROM ").append(table);
            return this;
        }

        public Delete where(String s) {
            buffer.append(" WHERE (").append(s).append(")");
            return this;
        }

        public Delete where(String template, String k1, Object v1) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Delete where(String template, String k1, Object v1, String k2, Object v2) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Delete where(String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return where(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Delete where(String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return where(SQLTemplate.merge(template, m));
        }

        public Delete where(String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return where(SQLTemplate.merge(template, m));
        }

        public Delete and(String s) {
            buffer.append(" AND (").append(s).append(")");
            return this;
        }

        public Delete and(String template, String k1, Object v1) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Delete and(String template, String k1, Object v1, String k2, Object v2) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Delete and(String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return and(SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Delete and(String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return and(SQLTemplate.merge(template, m));
        }

        public Delete and(String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return and(SQLTemplate.merge(template, m));
        }



        public Delete andIf(boolean b, String s) {
            if (b) {
                and(s);
            }
            return this;
        }

        public Delete andIf(boolean b, String template, String k1, Object v1) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1));
        }

        public Delete andIf(boolean b, String template, String k1, Object v1, String k2, Object v2) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2));
        }

        public Delete andIf(boolean b, String template, String k1, Object v1, String k2, Object v2, String k3, Object v3) {
            return andIf(b, SQLTemplate.merge(template, "alias", getAllAliases(), k1, v1, k2, v2, k3, v3));
        }

        public Delete andIf(boolean b, String template, Map<String, Object> data) {
            HashMap<String, Object> m = new HashMap<String, Object>(data);
            m.put("alias", getAllAliases());
            return andIf(b, SQLTemplate.merge(template, m));
        }

        public Delete andIf(boolean b, String template, Object[][] args) {
            Map<String, Object> m = CollectionUtils2.arraysToMap(args);
            m.put("alias", getAllAliases());
            return andIf(b, SQLTemplate.merge(template, m));
        }
    }
}
