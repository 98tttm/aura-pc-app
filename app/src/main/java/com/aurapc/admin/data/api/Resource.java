package com.aurapc.admin.data.api;

public class Resource<T> {
    public final boolean loading;
    public final T data;
    public final String message;

    private Resource(boolean loading, T data, String message) {
        this.loading = loading;
        this.data = data;
        this.message = message;
    }

    public static <T> Resource<T> success(T data) {
        return new Resource<>(false, data, null);
    }

    public static <T> Resource<T> error(String msg, T data) {
        return new Resource<>(false, data, msg);
    }

    public static <T> Resource<T> loading(T data) {
        return new Resource<>(true, data, null);
    }

    public boolean isSuccess() { return !loading && message == null; }
    public boolean isLoading() { return loading; }
    public boolean isError() { return !loading && message != null; }
}
