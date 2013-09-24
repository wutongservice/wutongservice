package com.borqs.server.market.utils.validation;


public interface Predicate<T> {

    Result predicate(T value);

    public static class Result {
        private final boolean ok;
        private final Object newValue;
        private final String errorMessage;

        private Result(boolean ok, Object newValue, String errorMessage) {
            this.ok = ok;
            this.newValue = newValue;
            this.errorMessage = errorMessage;
        }

        public static Result ok(Object newValue) {
            return new Result(true, newValue, null);
        }

        public static Result error(String message) {
            return new Result(false, null, message);
        }

        public boolean ok() {
            return this.ok;
        }

        public boolean error() {
            return !ok();
        }

        public Object newValue() {
            return this.newValue;
        }

        public String errorMessage() {
            return errorMessage;
        }
    }
}
