package com.example.kyrgyzlearn.dto.common;

public final class Nullable<T> {
    private static final Nullable<?> UNDEFINED = new Nullable<>(null, false);

    private final T value;
    private final boolean present;

    private Nullable(T value, boolean present) {
        this.value = value;
        this.present = present;
    }

    @SuppressWarnings("unchecked")
    public static <T> Nullable<T> undefined() {
        return (Nullable<T>) UNDEFINED;
    }

    public static <T> Nullable<T> of(T value) {
        return new Nullable<>(value, true);
    }

    public boolean isPresent() {
        return present;
    }

    public T get() {
        return value;
    }
}
