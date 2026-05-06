package com.example.aura_pc_app.di;

import android.content.Context;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.ApiService;
import com.example.aura_pc_app.data.db.AppDatabase;

/**
 * Manual dependency injection module.
 * Provides app-scoped singleton instances.
 */
public class AppModule {
    private final Context context;

    public AppModule(Context context) {
        this.context = context.getApplicationContext();
    }

    public AppDatabase provideDatabase() {
        return AppDatabase.getInstance(context);
    }

    public ApiService provideApiService() {
        return ApiClient.getInstance().getApiService();
    }
}
