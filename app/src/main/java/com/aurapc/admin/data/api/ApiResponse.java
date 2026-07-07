package com.aurapc.admin.data.api;

public class ApiResponse {
    public boolean success;
    public String message;
    public String error;

    public String getError() {
        return error != null ? error : message;
    }
}