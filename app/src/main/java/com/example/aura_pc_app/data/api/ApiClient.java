package com.example.aura_pc_app.data.api;

import android.content.Context;
import android.util.Log;

import com.example.aura_pc_app.BuildConfig;
import com.example.aura_pc_app.utils.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Singleton that assembles the full Retrofit + OkHttp stack:
 * <ul>
 *   <li>{@link AuthInterceptor} — attaches Bearer token to every request</li>
 *   <li>{@link TokenAuthenticator} — silently refreshes expired tokens on 401</li>
 *   <li>{@link HttpLoggingInterceptor} — logs request/response bodies in DEBUG only</li>
 * </ul>
 */
public class ApiClient {

    private static final String TAG = "ApiClient";
    private static ApiClient instance;

    private final ApiService apiService;
    private final TokenManager tokenManager;

    private ApiClient(Context context) {
        tokenManager = TokenManager.getInstance(context);

        // Concept 2 — Auth interceptor injects Bearer token
        AuthInterceptor authInterceptor = new AuthInterceptor(tokenManager);

        // Concept 4 — Authenticator handles 401 with refresh-token flow
        TokenAuthenticator tokenAuthenticator = new TokenAuthenticator(tokenManager);

        // Concept 3 — HTTP logging (BODY in debug, NONE in release)
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        if (BuildConfig.DEBUG) {
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        } else {
            logging.setLevel(HttpLoggingInterceptor.Level.NONE);
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(authInterceptor)
                .addInterceptor(logging)
                .authenticator(tokenAuthenticator)
                .build();

        Gson gson = new GsonBuilder().create();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(ApiService.class);

        Log.d(TAG, "ApiClient initialized — base URL: " + Constants.BASE_URL);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * @deprecated Use {@link #getInstance(Context)} instead.
     *             Kept temporarily for backward compatibility with existing callers.
     */
    @Deprecated
    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                    "ApiClient not initialized. Call getInstance(Context) first.");
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }
}
