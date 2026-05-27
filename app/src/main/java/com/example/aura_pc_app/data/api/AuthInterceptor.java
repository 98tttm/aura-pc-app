package com.example.aura_pc_app.data.api;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor that automatically attaches the Bearer token
 * from a {@link TokenProvider} to every outgoing HTTP request.
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    public AuthInterceptor(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        String token = tokenProvider.getAccessToken();

        if (token != null && !token.isEmpty()) {
            Request authenticated = original.newBuilder()
                    .header(HEADER_AUTHORIZATION, TOKEN_PREFIX + token)
                    .build();
            Log.d(TAG, "Bearer token attached → " + original.url());
            return chain.proceed(authenticated);
        }

        Log.d(TAG, "No token, unauthenticated request → " + original.url());
        return chain.proceed(original);
    }
}
