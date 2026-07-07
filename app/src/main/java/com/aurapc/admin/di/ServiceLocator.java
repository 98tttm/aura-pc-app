package com.aurapc.admin.di;

import android.content.Context;

import com.aurapc.admin.Constants;
import com.aurapc.admin.data.api.ApiClient;
import com.aurapc.admin.data.local.LocalCache;
import com.aurapc.admin.data.local.TokenManager;
import com.aurapc.admin.data.socket.SocketManager;

public final class ServiceLocator {

    private static volatile ServiceLocator instance;

    private final Context appContext;
    private final TokenManager tokenManager;
    private final ApiClient apiClient;
    private final SocketManager socketManager;
    private final LocalCache localCache;

    private ServiceLocator(Context ctx) {
        this.appContext = ctx.getApplicationContext();
        this.tokenManager = new TokenManager(this.appContext);
        this.apiClient = ApiClient.get(tokenManager, Constants.BASE_URL);
        this.socketManager = SocketManager.get(this.appContext);
        this.localCache = new LocalCache(this.appContext);
    }

    public static void init(Context ctx) {
        if (instance == null) {
            synchronized (ServiceLocator.class) {
                if (instance == null) instance = new ServiceLocator(ctx);
            }
        }
    }

    public static ServiceLocator get() {
        if (instance == null) {
            throw new IllegalStateException("ServiceLocator not initialised; call init() in Application.onCreate()");
        }
        return instance;
    }

    public TokenManager tokenManager() { return tokenManager; }
    public ApiClient apiClient() { return apiClient; }
    public SocketManager socketManager() { return socketManager; }
    public LocalCache localCache() { return localCache; }
}
