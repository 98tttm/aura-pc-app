package com.example.aura_pc_app;

import android.app.Application;

import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.db.AppDatabase;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.getInstance(this);
        AppDatabase.getInstance(this);
    }
}
