package com.example.aura_pc_app;

import android.app.Application;
import android.content.Context;
import com.example.aura_pc_app.utils.LocaleManager;

public class App extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleManager.wrap(base));
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
