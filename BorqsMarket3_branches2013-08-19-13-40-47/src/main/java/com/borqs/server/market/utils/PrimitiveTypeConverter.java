package com.borqs.server.market.utils;


import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.Validate;

public class PrimitiveTypeConverter {

    public static final int STRING = 1;
    public static final int CHAR = 2;
    public static final int BOOLEAN = 3;
    public static final int BYTE = 4;
    public static final int SHORT = 5;
    public static final int INT = 6;
    public static final int LONG = 7;
    public static final int FLOAT = 8;
    public static final int DOUBLE = 9;

    private static char firstChar(String s) {
        return s.isEmpty() ? '\0' : s.charAt(0);
    }

    private static boolean strToBool(String s) {
        if (s.equalsIgnoreCase("true")
                || s.equalsIgnoreCase("1")
                || s.equalsIgnoreCase("yes")
                || s.equalsIgnoreCase("y")
                || s.equalsIgnoreCase("t")) {
            return true;
        } else if (s.equalsIgnoreCase("false")
                || s.equalsIgnoreCase("0")
                || s.equalsIgnoreCase("no")
                || s.equalsIgnoreCase("n")
                || s.equalsIgnoreCase("f")) {
            return false;
        } else {
            throw new IllegalArgumentException("Str to boolean error");
        }
    }

    private static boolean charToBool(char c) {
        c = Character.toLowerCase(c);
        if (c == 'y' || c == '1' || c == 't' || c == (char) 1) {
            return true;
        } else if (c == 'n' || c == '0' || c == 'f' || c == '\0') {
            return false;
        } else {
            throw new IllegalArgumentException("Char to boolean error");
        }
    }

    public static Object convert(Object o, int to) {
        if (to != STRING
                && to != CHAR
                && to != BOOLEAN
                && to != BYTE
                && to != SHORT
                && to != INT
                && to != LONG
                && to != FLOAT
                && to != DOUBLE)
            throw new IllegalArgumentException("Illegal to type " + to);

        Object r = null;
        if (o == null) {
            r = null;
        } else if (o instanceof String) {
            switch (to) {
                case STRING:
                    r = o;
                    break;
                case CHAR:
                    r = firstChar((String) o);
                    break;
                case BOOLEAN:
                    r = strToBool((String) o);
                    break;
                case BYTE:
                    r = Byte.parseByte((String) o);
                    break;
                case SHORT:
                    r = Short.parseShort((String) o);
                    break;
                case INT:
                    r = Integer.parseInt((String) o);
                    break;
                case LONG:
                    r = Long.parseLong((String) o);
                    break;
                case FLOAT:
                    r = Float.parseFloat((String) o);
                    break;
                case DOUBLE:
                    r = Double.parseDouble((String) o);
                    break;
            }
        } else if (o instanceof Integer) {
            int v = ((Integer) o);
            switch (to) {
                case STRING:
                    r = o.toString();
                    break;
                case CHAR:
                    r = (char) v;
                    break;
                case BOOLEAN:
                    r = v != 0;
                    break;
                case BYTE:
                    r = (byte) v;
                    break;
                case SHORT:
                    r = (short) v;
                    break;
                case INT:
                    r = v;
                    break;
                case LONG:
                    r = v;
                    break;
                case FLOAT:
                    r = (float) v;
                    break;
                case DOUBLE:
                    r = (double) v;
                    break;
            }
        } else if (o instanceof Boolean) {
            boolean b = ((Boolean) o);
            switch (to) {
                case STRING:
                    r = o.toString();
                    break;
                case CHAR:
                    r = b ? '1' : '0';
                    break;
                case BOOLEAN:
                    r = b;
                    break;
                case BYTE:
                    r = (byte) (b ? 1 : 0);
                    break;
                case SHORT:
                    r = (short) (b ? 1 : 0);
                    break;
                case INT:
                    r = b ? 1 : 0;
                    break;
                case LONG:
                    r = b ? 1L : 0L;
                    break;
                case FLOAT:
                    r = b ? 1.0f : 0.0f;
                    break;
                case DOUBLE:
                    r = b ? 1.0 : 0.0;
                    break;
            }
        } else if (o instanceof Character) {
            char c = (Character) o;
            switch (to) {
                case STRING:
                    r = o.toString();
                    break;
                case CHAR:
                    r = c;
                    break;
                case BOOLEAN:
                    r = charToBool(c);
                    break;
                case BYTE:
                    r = (byte) c;
                    break;
                case SHORT:
                    r = c;
                    break;
                case INT:
                    r = (int) c;
                    break;
                case LONG:
                    r = (long) c;
                    break;
                case FLOAT:
                    r = (float) (int) c;
                    break;
                case DOUBLE:
                    r = (double) (int) c;
                    break;
            }
        } else if (o instanceof Byte) {
            byte v = ((Byte) o);
            switch (to) {
                case STRING:
                    r = o.toString();
                    break;
                case CHAR:
                    r = (char) v;
                    break;
                case BOOLEAN:
                    r = v != 0;
                    break;
                case BYTE:
                    r = v;
                    break;
                case SHORT:
                    r = (short) v;
                    break;
                case INT:
                    r = v;
                    break;
                case LONG:
                    r = (long) v;
                    break;
                case FLOAT:
                    r = (float) (int) v;
                    break;
                case DOUBLE:
                    r = (double) (int) v;
                    break;
            }
        } else if (o instanceof Short) {
            short v = ((Short) o);
            switch (to) {
                case STRING:
                    r = o.toString();
                    break;
                case CHAR:
                    r = v;
                    break;
                case BOOLEAN:
                    r = v != 0;
                    break;
                case BYTE:
                    r = (byte) v;
                    break;
                case SHORT:
                    r = v;
                    break;
                case INT:
                    r = (int) v;
                    break;
                case LONG:
                    r = (long) v;
                    break;
                case FLOAT:
                    r = (float) (int) v;
                    break;
                case DOUBLE:
                    r = (double) (int) v;
                    break;
            }
        } else if (o instanceof Long) {
            long v = ((Long) o);
            switch (to) {
                case STRING:
                    r = o.toString();
                    break;
                case CHAR:
                    r = (char) v;
                    break;
                case BOOLEAN:
                    r = v != 0L;
                    break;
                case BYTE:
                    r = (byte) v;
                    break;
                case SHORT:
                    r = (short) v;
                    break;
                case INT:
                    r = (int) v;
                    break;
                case LONG:
                    r = v;
                    break;
                case FLOAT:
                    r = (float) v;
                    break;
                case DOUBLE:
                    r = (double) v;
                    break;
            }
        } else if (o instanceof Float) {
            float v = ((Float) o);
            switch (to) {
                case STRING:
                    r = o.toString();
                    break;
                case CHAR:
                    r = (char) v;
                    break;
                case BOOLEAN:
                    r = !o.equals(0.0f);
                    break;
                case BYTE:
                    r = (byte) v;
                    break;
                case SHORT:
                    r = (short) v;
                    break;
                case INT:
                    r = (int) v;
                    break;
                case LONG:
                    r = (long) v;
                    break;
                case FLOAT:
                    r = v;
                    break;
                case DOUBLE:
                    r = (double) v;
                    break;
            }
        } else if (o instanceof Double) {
            double v = ((Double) o);
            switch (to) {
                case STRING:
                    r = o.toString();
                    break;
                case CHAR:
                    r = (char) v;
                    break;
                case BOOLEAN:
                    r = !o.equals(0.0);
                    break;
                case BYTE:
                    r = (byte) v;
                    break;
                case SHORT:
                    r = (short) v;
                    break;
                case INT:
                    r = (int) v;
                    break;
                case LONG:
                    r = (long) v;
                    break;
                case FLOAT:
                    r = (float) v;
                    break;
                case DOUBLE:
                    r = v;
                    break;
            }
        } else {
            r = convert(ObjectUtils.toString(o), to);
        }
        return r;
    }

    public static String toStr(Object o) {
        return (String) convert(o, STRING);
    }

    public static char toChar(Object o) {
        return (Character) convert(o, CHAR);
    }

    public static boolean toBoolean(Object o) {
        return (Boolean) convert(o, BOOLEAN);
    }

    public static byte toByte(Object o) {
        return (Byte) convert(o, BYTE);
    }

    public static short toShort(Object o) {
        return (Short) convert(o, SHORT);
    }

    public static int toInt(Object o) {
        return (Integer) convert(o, INT);
    }

    public static long toLong(Object o) {
        return (Long) convert(o, LONG);
    }

    public static float toFloat(Object o) {
        return (Float) convert(o, FLOAT);
    }

    public static double toDouble(Object o) {
        return (Double) convert(o, DOUBLE);
    }


    public static char toChar(Object o, char def) {
        try {
            return (Character) convert(o, CHAR);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static boolean toBoolean(Object o, boolean def) {
        try {
            return (Boolean) convert(o, BOOLEAN);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static byte toByte(Object o, byte def) {
        try {
            return (Byte) convert(o, BYTE);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static short toShort(Object o, short def) {
        try {
            return (Short) convert(o, SHORT);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static int toInt(Object o, int def) {
        try {
            return (Integer) convert(o, INT);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static long toLong(Object o, long def) {
        try {
            return (Long) convert(o, LONG);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static float toFloat(Object o, float def) {
        try {
            return (Float) convert(o, FLOAT);
        } catch (Exception ignored) {
            return def;
        }
    }

    public static double toDouble(Object o, double def) {
        try {
            return (Double) convert(o, DOUBLE);
        } catch (Exception ignored) {
            return def;
        }
    }
}
