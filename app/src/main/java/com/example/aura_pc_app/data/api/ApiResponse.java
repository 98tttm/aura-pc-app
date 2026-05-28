package com.example.aura_pc_app.data.api;

/**
 * Generic wrapper that standardises every API result into one of three states:
 * {@code LOADING}, {@code SUCCESS}, or {@code ERROR}.
 *
 * <p>Usage in UI:
 * <pre>
 *   switch (response.getStatus()) {
 *       case LOADING:  showProgress();       break;
 *       case SUCCESS:  showData(getData());  break;
 *       case ERROR:    showError(getMsg());  break;
 *   }
 * </pre>
 *
 * @param <T> The data type carried on success.
 */
public class ApiResponse<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    private final Status status;
    private final T data;
    private final String message;

    private ApiResponse(Status status, T data, String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    // ── Factory methods ──────────────────────────────────

    public static <T> ApiResponse<T> loading() {
        return new ApiResponse<>(Status.LOADING, null, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(Status.SUCCESS, data, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(Status.ERROR, null, message);
    }

    // ── Getters ──────────────────────────────────────────

    public Status getStatus() { return status; }
    public T getData()        { return data; }
    public String getMessage() { return message; }
}
