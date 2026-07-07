package com.aurapc.admin.data.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aurapc.admin.data.local.TokenManager;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {

    private static volatile ApiClient instance;
    private final Retrofit retrofit;
    private final AuthApi authApi;
    private final DashboardApi dashboardApi;
    private final ProductApi productApi;
    private final OrderApi orderApi;
    private final UserApi userApi;
    private final ContentApi contentApi;

    private ApiClient(TokenManager tokenManager, String baseUrl) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(new AuthInterceptor(tokenManager))
                .addInterceptor(logging)
                .retryOnConnectionFailure(true)
                .build();

        retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        authApi = retrofit.create(AuthApi.class);
        dashboardApi = retrofit.create(DashboardApi.class);
        productApi = retrofit.create(ProductApi.class);
        orderApi = retrofit.create(OrderApi.class);
        userApi = retrofit.create(UserApi.class);
        contentApi = retrofit.create(ContentApi.class);
    }

    public static ApiClient get(@NonNull TokenManager tokenManager, @NonNull String baseUrl) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) instance = new ApiClient(tokenManager, baseUrl);
            }
        }
        return instance;
    }

    public AuthApi authApi() { return authApi; }
    public DashboardApi dashboardApi() { return dashboardApi; }
    public ProductApi productApi() { return productApi; }
    public OrderApi orderApi() { return orderApi; }
    public UserApi userApi() { return userApi; }
    public ContentApi contentApi() { return contentApi; }
    public Retrofit retrofit() { return retrofit; }

    @Nullable
    public <T> T create(Class<T> service) {
        try {
            return retrofit.create(service);
        } catch (Exception ex) {
            return null;
        }
    }
}
