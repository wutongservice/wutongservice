package com.borqs.server.wutong.account2.user;


import com.borqs.server.ServerException;
import com.borqs.server.base.BaseErrors;
import com.borqs.server.base.data.Values;
import com.borqs.server.base.util.StringUtils2;
import com.borqs.server.wutong.account2.util.ClassHelper;
import com.borqs.server.wutong.account2.util.VfsHelper;
import com.borqs.server.wutong.account2.util.json.JsonHelper;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Schema {

    private static volatile List<Column> columns;

    static {
        load();
    }

    public static Column[] columns() {
        return columns != null ? columns.toArray(new Column[columns.size()]) : new Column[0];
    }

    public static boolean has(String col) {
        return column(col) != null;
    }

    public static Column column(String col) {
        if (columns == null)
            return null;

        for (Column col0 : columns) {
            if (col0.column.equals(col))
                return col0;
        }

        return null;
    }

    public static boolean has(int key) {
        return column(key) != null;
    }

    public static Column column(int key) {
        if (columns == null)
            return null;

        for (Column col0 : columns) {
            if (col0.key == key)
                return col0;
        }
        return null;
    }

    private static short parseKey(JsonNode keyNode) {
        if (keyNode.isNull())
            throw new IllegalArgumentException("Node 'key' is missing");

        if (keyNode.isIntegralNumber()) {
            return (short)keyNode.getIntValue();
        } else if (keyNode.isTextual()) {
            String s = keyNode.getTextValue();
            if (StringUtils.isNumeric(s)) {
                return Short.parseShort(s);
            } else if (s.startsWith("@")) {
                String className = StringUtils.removeStart(StringUtils.substringBeforeLast(s, "."), "@");
                String fieldName = StringUtils.substringAfterLast(s, ".");
                Class clazz = ClassHelper.forName(className);
                try {
                    Field field = clazz.getField(fieldName);
                    return ((Number)field.get(null)).shortValue();
                } catch (Exception e) {
                    throw new IllegalArgumentException("Read 'key' error (" + s + ")", e);
                }
            } else {
                throw new IllegalArgumentException("Node 'key' type error");
            }
        } else {
            throw new IllegalArgumentException("Node 'key' type error");
        }
    }

    private static String parseColumn(String s) {
        if (s.startsWith("@")) {
            String className = StringUtils.removeStart(StringUtils.substringBeforeLast(s, "."), "@");
            String fieldName = StringUtils.substringAfterLast(s, ".");
            Class clazz = ClassHelper.forName(className);
            try {
                Field field = clazz.getField(fieldName);
                return (String)field.get(null);
            } catch (Exception e) {
                throw new IllegalArgumentException("Read 'key' error (" + s + ")", e);
            }
        } else {
            return s;
        }
    }

    private static void load0(List<String> paths) {
        ArrayList<Column> cols = new ArrayList<Column>();
        for (String path : paths) {
            String s = VfsHelper.loadText(path);
            JsonNode root = JsonHelper.parse(s);
            for (int i = 0; i < root.size(); i++) {
                JsonNode colNode = root.get(i);
                String col = parseColumn(colNode.path("column").getTextValue());
                short key = parseKey(colNode.path("key"));
                String type = colNode.path("type").getTextValue();
                String clazz = colNode.path("class").getTextValue();
                String def = null;
                if (colNode.has("default"))
                    def = colNode.get("default").getValueAsText();
                cols.add(createColumn(col, key, type, clazz, def));
            }

        }
        columns = cols;
    }

    private static void load() {
        ArrayList<String> paths = new ArrayList<String>();
        paths.add(VfsHelper.classpathFileToPath(User.class, "user_schema.json"));
        String s = System.getProperty("user.schemas", null);
        if (s != null)
            paths.addAll(StringUtils2.splitList(s, ";", true));

        load0(paths);
    }

    public static Object parseSimpleValue(String simpleType, String s) {
        simpleType = StringUtils.trimToEmpty(simpleType);
        if (simpleType.equalsIgnoreCase("string")) {
            return s != null ? s : "";
        } else if (simpleType.equalsIgnoreCase("int")) {
            return s != null ? Integer.parseInt(s) : 0;
        } else if (simpleType.equalsIgnoreCase("boolean")) {
            return s != null && Boolean.parseBoolean(s);
        } else if (simpleType.equalsIgnoreCase("long")) {
            return s != null ? Long.parseLong(s) : 0L;
        } else if (simpleType.equalsIgnoreCase("double")) {
            return s != null ? Double.parseDouble(s) : 0.0;
        } else if (simpleType.equalsIgnoreCase("byte")) {
            return s != null ? Byte.parseByte(s) : (byte)0;
        } else if (simpleType.equalsIgnoreCase("short")) {
            return s != null ? Short.parseShort(s) : (short)0;
        } else if (simpleType.equalsIgnoreCase("float")) {
            return s != null ? Float.parseFloat(s) : 0.0f;
        } else {
            throw new IllegalArgumentException("Invalid simple type " + simpleType);
        }
    }
    @SuppressWarnings("unchecked")
    private static Column createColumn(String col, short key, String type, String clazz, String def) {
        if ("simple".equalsIgnoreCase(type))
            type = "simple.string";

        // column
        if (StringUtils.isBlank(col))
            throw new IllegalArgumentException("Invalid column name " + col);

        Object defValue = null;
        // type
        Column.Type t;
        if (StringUtils.startsWithIgnoreCase(type, "simple.")) {
            t = Column.Type.SIMPLE;
            defValue = parseSimpleValue(StringUtils.removeStartIgnoreCase(type, "simple."), def);
        } else if ("simpleArray".equalsIgnoreCase(type) || "simple_array".equalsIgnoreCase(type)) {
            t = Column.Type.SIMPLE_ARRAY;
        } else if ("object".equalsIgnoreCase(type)) {
            t = Column.Type.OBJECT;
        } else if ("objectArray".equalsIgnoreCase(type) || "object_array".equalsIgnoreCase(type)) {
            t = Column.Type.OBJECT_ARRAY;
        } else {
            throw new IllegalArgumentException("Invalid column type " + type);
        }

        // clazz
        Class clz = clazz != null ? ClassHelper.forName(clazz) : null;
        String simpleType;
        if (StringUtils.startsWithIgnoreCase(type, "simple."))
            simpleType = StringUtils.removeStartIgnoreCase(type, "simple.");
        else
            simpleType = null;

        return new Column(col, key, t, clz, simpleType, defValue);
    }

    @SuppressWarnings("unchecked")
    public static Object checkForSet(String col, Object v) {
        Column c = column(col);
        if (c == null)
            throw new IllegalArgumentException("Can't find column " + col);

        switch (c.type) {
            case SIMPLE:
                return Values.trimSimple(v);

            case SIMPLE_ARRAY: {
                ArrayList l = toArrayList(v);
                for (int i = 0; i < l.size(); i++)
                    l.set(i, Values.trimSimple(l.get(i)));
                return l;
            }

            case OBJECT: {
                if (!c.clazz.isInstance(v))
                    throw new IllegalArgumentException("Value is not a instance of " + c.clazz.getName());
                return v;
            }

            case OBJECT_ARRAY: {
                ArrayList l = toArrayList(v);
                for (Object o : l) {
                    if (!c.clazz.isInstance(o))
                        throw new IllegalArgumentException("Value is not a instance of " + c.clazz.getName());
                }
                return l;
            }
        }
        throw new IllegalStateException("Run here error");
    }

    @SuppressWarnings("unchecked")
    private static ArrayList toArrayList(Object o) {
        if (o == null) {
            return new ArrayList();
        } else if (o instanceof List) {
            return new ArrayList((List)o);
        } else if (o.getClass().isArray()) {
            ArrayList l = new ArrayList();
            Collections.addAll(l, (Object[])o);
            return l;
        } else {
            throw new IllegalArgumentException("Can't convert to list");
        }
    }

    public static class Column {
        public static enum Type {
            SIMPLE,
            SIMPLE_ARRAY,
            OBJECT,
            OBJECT_ARRAY,
        }

        public final String column;
        public final short key;
        public final Type type;
        public final Class<? extends PropertyBundle> clazz;
        public final Object defaultSimpleValue;
        public final String simpleType;

        private Column(String column, short key, Type type, Class<? extends PropertyBundle> clazz, String simpleType, Object defaultSimpleValue) {
            this.column = column;
            this.key = key;
            this.type = type;
            this.clazz = clazz;
            this.simpleType = simpleType;
            this.defaultSimpleValue = defaultSimpleValue;
        }

        public Object newDefaultValue() {
            if (type == Type.SIMPLE)
                return defaultSimpleValue;

            if (type == Type.SIMPLE_ARRAY || type == Type.OBJECT_ARRAY)
                return new ArrayList();

            if (type == Type.OBJECT) {
                Method method = ClassHelper.getMethodNoThrow(clazz, "newDefault");
                if (method != null) {
                    try {
                        method.invoke(null);
                    } catch (IllegalAccessException e) {
                        throw new ServerException(BaseErrors.PLATFORM_CLASS_NOT_FOUND_ERROR, e, "Create new default value for %s error", clazz.getName());
                    } catch (InvocationTargetException e) {
                        throw new ServerException(BaseErrors.PLATFORM_CLASS_NOT_FOUND_ERROR, e.getTargetException(), "Create new default value for %s error", clazz.getName());
                    }
                } else {
                    try {
                        return clazz.newInstance();
                    } catch (InstantiationException e) {
                        throw new ServerException(BaseErrors.PLATFORM_CLASS_NOT_FOUND_ERROR, e, "Create new default value for %s error", clazz.getName());
                    } catch (IllegalAccessException e) {
                        throw new ServerException(BaseErrors.PLATFORM_CLASS_NOT_FOUND_ERROR, e, "Create new default value for %s error", clazz.getName());
                    }
                }
            }

            throw new IllegalStateException("Can't run here");
        }
    }
}
