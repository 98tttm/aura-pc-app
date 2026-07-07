package com.aurapc.admin.data.api;

import com.aurapc.admin.data.local.TokenManager;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that adds the JWT bearer header (when available) and
 * skips the Authorization header for /api/admin/auth/login.
 */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;

    public AuthInterceptor(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String path = original.url().encodedPath();
        boolean isLogin = path != null && path.endsWith("/api/admin/auth/login");

        Request.Builder builder = original.newBuilder();
        if (!isLogin) {
            String token = tokenManager.getToken();
            if (token != null && !token.trim().isEmpty()) {
                builder.header("Authorization", "Bearer " + token);
            }
        }
        builder.header("Accept", "application/json");
        return chain.proceed(builder.build());
    }
}