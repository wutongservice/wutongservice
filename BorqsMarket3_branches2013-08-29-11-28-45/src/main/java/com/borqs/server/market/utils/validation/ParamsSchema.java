package com.borqs.server.market.utils.validation;


import com.borqs.server.market.ServiceException;
import com.borqs.server.market.utils.Params;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.ObjectUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.borqs.server.market.Errors.E_ILLEGAL_PARAM;

public class ParamsSchema {

    private boolean extra = true;
    private final Map<String, Entry> schemas = new LinkedHashMap<String, Entry>();

    public ParamsSchema() {
        this(true);
    }

    public ParamsSchema(boolean extra) {
        this.extra = extra;
    }

    public boolean isExtra() {
        return extra;
    }

    public void setExtra(boolean extra) {
        this.extra = extra;
    }

    public void clear() {
        schemas.clear();
    }

    public ParamsSchema optional(String name, Predicate... preds) {
        return optional(name, null, null, preds);
    }

    public ParamsSchema optional(String name, String errorMessage, Predicate... preds) {
        return optional(name, null, errorMessage, preds);
    }

    public ParamsSchema optional(String name, Object def, String errorMessage, Predicate... preds) {
        schemas.put(name, new Entry(true, def,
                ArrayUtils.isNotEmpty(preds)
                        ? Predicates.and(errorMessage, preds)
                        : null));
        return this;
    }

    public ParamsSchema required(String name, Predicate... preds) {
        return required(name, null, preds);
    }

    public ParamsSchema required(String name, String errorMessage, Predicate... preds) {
        schemas.put(name, new Entry(false, null,
                ArrayUtils.isNotEmpty(preds)
                        ? Predicates.and(errorMessage, preds)
                        : null));
        return this;
    }

    public <PS extends Map<String, Object>> ValidateResult validateQuietly(PS params) {
        return validateQuietly(params, params);
    }

    public <PS extends Params> ValidateResult validateQuietly(PS params, PS newParams) {
        return validateQuietly(params.getParams(), newParams.getParams());
    }

    @SuppressWarnings("unchecked")
    private static void predicateValue(String name,
                                       Object value,
                                       Predicate pred,
                                       Map<String, Object> newParams,
                                       Map<String, String> errorMessages) {
        if (pred != null) {
            Predicate.Result pr = pred.predicate(value);
            if (pr.ok()) {
                newParams.put(name, pr.newValue());
            } else {
                errorMessages.put(name, pr.errorMessage());
            }
        }
    }

    public <PS extends Map<String, Object>> ValidateResult validateQuietly(PS params, PS newParams) {
        if (params == null)
            return ValidateResult.ok(newParams);

        LinkedHashMap<String, String> errorMessages = new LinkedHashMap<String, String>();
        if (!extra) {
            for (String key : params.keySet()) {
                if (!schemas.containsKey(key))
                    errorMessages.put(key, "Unexpected");
            }
            if (!errorMessages.isEmpty())
                return ValidateResult.error(errorMessages);
        }

        for (Map.Entry<String, Entry> e : schemas.entrySet()) {
            String name = e.getKey();
            Entry entry = e.getValue();

            Object value = params.get(name);
            if (!entry.optional) {
                if (value == null) {
                    errorMessages.put(name, "Missing");
                } else {
                    predicateValue(name, value, entry.predicate, newParams, errorMessages);
                }
            } else {
                if (value == null) {
                    if (entry.defaultValue != null)
                        newParams.put(name, entry.defaultValue);
                } else {
                    predicateValue(name, value, entry.predicate, newParams, errorMessages);
                }
            }
        }

        return errorMessages.isEmpty()
                ? ValidateResult.ok(newParams)
                : ValidateResult.error(errorMessages);
    }

    public <PS extends Params> ValidateResult validateQuietly(PS params) {
        return validateQuietly(params.getParams());
    }

    public <PS extends Map<String, Object>> PS validate(PS params, PS newParams) {
        ValidateResult vr = validateQuietly(params, newParams);
        if (vr.error()) {
            throw new ServiceException(E_ILLEGAL_PARAM, "Params validation error")
                    .withDetails(vr.toDetails());
        }
        return vr.newParams();
    }

    public <PS extends Map<String, Object>> PS validate(PS params) {
        return validate(params, params);
    }


    public <PS extends Params> PS validate(PS params, PS newParams) {
        validate(params.getParams(), newParams.getParams());
        return newParams;
    }

    public <PS extends Params> PS validate(PS params) {
        return validate(params, params);
    }

    protected static class Entry {
        public final boolean optional;
        public final Object defaultValue;
        public final Predicate predicate;

        public Entry(boolean optional, Object defaultValue, Predicate predicate) {
            this.optional = optional;
            this.defaultValue = defaultValue;
            this.predicate = predicate;
        }
    }

    public static class ValidateResult {
        private final boolean ok;
        private final Map<String, Object> newParams;
        private final Map<String, String> errorMessages;

        private ValidateResult(boolean ok, Map<String, Object> newParams, Map<String, String> errorMessages) {
            this.ok = ok;
            this.newParams = newParams;
            this.errorMessages = errorMessages;
        }

        public static ValidateResult ok(Map<String, Object> newParams) {
            return new ValidateResult(true, newParams, null);
        }

        public static ValidateResult error(Map<String, String> errorMessages) {
            return new ValidateResult(false, null, errorMessages);
        }

        public boolean ok() {
            return ok;
        }

        public boolean error() {
            return !ok();
        }

        @SuppressWarnings("unchecked")
        public <PS extends Map<String, Object>> PS newParams() {
            return (PS) newParams;
        }

        public Map<String, String> errorMessages() {
            return errorMessages;
        }

        public String getErrorMessage(String sep) {
            if (MapUtils.isEmpty(errorMessages)) {
                return "";
            } else {
                StringBuilder buff = new StringBuilder();
                for (Map.Entry<String, String> e : errorMessages.entrySet()) {
                    buff.append(e.getKey())
                            .append(": ")
                            .append(ObjectUtils.toString(e.getValue()))
                            .append(ObjectUtils.toString(sep, "\n"));
                }
                return buff.toString();
            }
        }

        public String getErrorMessage() {
            return getErrorMessage("\n");
        }

        public String[] toDetails() {
            ArrayList<String> details = new ArrayList<String>();
            for (Map.Entry<String, String> e : errorMessages.entrySet()) {
                details.add(String.format("%s: %s", e.getKey(), e.getValue()));
            }
            return details.toArray(new String[details.size()]);
        }

        public Map<String, Object> getErrorModelForForm(Params params, String... names) {
            LinkedHashMap<String, Object> valuesWithErrors = new LinkedHashMap<String, Object>();
            for (String name : names) {
                String val = params.param(name).asString("");
                if (errorMessages.containsKey(name)) {
                    valuesWithErrors.put(name, val);
                    String errorMessage = errorMessages.get(name);
                    valuesWithErrors.put(name + "_errorMessage", errorMessage);
                    valuesWithErrors.put(name + "_errorClass", errorMessage != null ? "error" : "");
                } else {
                    valuesWithErrors.put(name, val);
                }
            }
            return valuesWithErrors;
        }
    }
}
