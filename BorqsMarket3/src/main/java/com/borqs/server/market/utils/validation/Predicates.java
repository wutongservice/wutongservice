package com.borqs.server.market.utils.validation;


import com.borqs.server.market.utils.CC;
import com.borqs.server.market.utils.JsonUtils;
import com.borqs.server.market.utils.PrimitiveTypeConverter;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.codehaus.jackson.JsonNode;
import org.mvel2.MVEL;

import java.io.IOException;

public class Predicates {

    public static Predicate and(Predicate... preds) {
        return and(null, preds);
    }

    @SuppressWarnings("unchecked")
    public static Predicate and(final String errorMessage, final Predicate... preds) {
        return new Predicate() {
            @Override
            public Result predicate(Object value) {
                Object newValue = value;
                for (Predicate pred : preds) {
                    if (pred == null)
                        continue;

                    Result r;
                    try {
                        r = pred.predicate(newValue);
                    } catch (Exception e) {
                        return Result.error(errorMessage);
                    }
                    if (r.ok()) {
                        newValue = r.newValue();
                    } else {
                        return Result.error(StringUtils.isEmpty(errorMessage) ? r.errorMessage() : errorMessage);
                    }
                }
                return Result.ok(newValue);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Predicate or(final String errorMessage, final Predicate... preds) {
        return new Predicate() {
            @Override
            public Result predicate(Object value) {
                Object newValue = value;
                boolean ok = false;
                for (Predicate pred : preds) {
                    if (pred == null)
                        continue;

                    Result r = pred.predicate(newValue);
                    if (r.ok()) {
                        newValue = r.newValue();
                        if (!(pred instanceof ValueConverter))
                            ok = true;
                    }
                }
                return ok
                        ? Result.ok(newValue)
                        : Result.error(ObjectUtils.toString(errorMessage));
            }
        };
    }


    private static interface ValueConverterPredicate<T> extends Predicate<T>, ValueConverter {
    }

    public static Predicate asInt() {
        return new ValueConverterPredicate() {
            @Override
            public Result predicate(Object value) {
                try {
                    return Result.ok(PrimitiveTypeConverter.toInt(value));
                } catch (Exception e) {
                    return Result.error(String.format("'%s' is not a integer", value));
                }
            }
        };
    }

    public static Predicate asBoolean() {
        return new ValueConverterPredicate() {
            @Override
            public Result predicate(Object value) {
                try {
                    return Result.ok(PrimitiveTypeConverter.toBoolean(value));
                } catch (Exception e) {
                    return Result.error(String.format("'%s' is not a boolean", value));
                }
            }
        };
    }

    public static Predicate asLong() {
        return new ValueConverterPredicate() {
            @Override
            public Result predicate(Object value) {
                try {
                    return Result.ok(PrimitiveTypeConverter.toLong(value));
                } catch (Exception e) {
                    return Result.error(String.format("'%s' is not a long", value));
                }
            }
        };
    }

    public static Predicate asDouble() {
        return new ValueConverterPredicate() {
            @Override
            public Result predicate(Object value) {
                try {
                    return Result.ok(PrimitiveTypeConverter.toDouble(value));
                } catch (Exception e) {
                    return Result.error(String.format("'%s' is not a double", value));
                }
            }
        };
    }

    public static Predicate asJson() {
        return new ValueConverterPredicate() {
            @Override
            public Result predicate(Object value) {
                try {
                    String json = ObjectUtils.toString(value);
                    JsonNode jn = StringUtils.isEmpty(json)
                            ? null
                            : JsonUtils.parseJson(json);
                    return Result.ok(jn);
                } catch (IOException e) {
                    return Result.error("Parse json error");
                }
            }
        };
    }

    public static Predicate expression(final String expr) {
        return expression(expr, null);
    }

    public static Predicate expression(final String expr, String errorMessage) {
        return new ValuePredicate(errorMessage) {
            @Override
            public boolean predicateValue(Object value) {
                try {
                    Object o = MVEL.executeExpression(MVEL.compileExpression(expr), CC.map("x=>", value));
                    return PrimitiveTypeConverter.toBoolean(o);
                } catch (Exception e) {
                    throw new ValidateException("Expression error '" + expr + "'");
                }
            }
        };
    }

    public static Predicate<String> notEmpty() {
        return notEmpty(null);
    }

    public static Predicate<String> notEmpty(String errorMessage) {
        return new ValuePredicate<String>(errorMessage) {
            @Override
            protected boolean predicateValue(String value) {
                return StringUtils.isNotEmpty(value);
            }
        };
    }

    public static Predicate<String> notBlank() {
        return notBlank(null);
    }

    public static Predicate<String> notBlank(String errorMessage) {
        return new ValuePredicate<String>() {
            @Override
            protected boolean predicateValue(String value) {
                return StringUtils.isNotBlank(value);
            }
        };
    }

    public static Predicate in(Object... options) {
        return inWithMessage(null, options);
    }

    public static Predicate inWithMessage(String errorMessage, final Object... options) {
        return new ValuePredicate(errorMessage) {
            @Override
            protected boolean predicateValue(Object value) {
                return ArrayUtils.contains(options, value);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static Predicate typeIs(final Class clazz) {
        Validate.notNull(clazz);
        return new ValueConverterPredicate() {
            @Override
            public Result predicate(Object value) {
                if (value == null)
                    return Result.error("value is null");

                if (!clazz.isAssignableFrom(value.getClass()))
                    return Result.error("value type error " + value.getClass().getName());

                return Result.ok(value);
            }
        };
    }
}
