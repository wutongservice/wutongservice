package com.borqs.server.market.utils.validation;


public abstract class ValuePredicate<T> implements Predicate<T> {
    private final String errorMessage;

    protected ValuePredicate() {
        this(null);
    }

    protected ValuePredicate(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Result predicate(T value) {
        if (predicateValue(value)) {
            return Result.ok(value);
        } else {
            return Result.error(errorMessage);
        }
    }

    protected abstract boolean predicateValue(T value);
}
