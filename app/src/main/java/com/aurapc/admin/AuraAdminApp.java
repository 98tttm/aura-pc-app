package com.aurapc.admin;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.aurapc.admin.di.ServiceLocator;
import com.google.firebase.FirebaseApp;

public class AuraAdminApp extends Application {

    private static AuraAdminApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        FirebaseApp.initializeApp(this);
        ServiceLocator.init(this);
        createNotificationChannels();
    }

    public static Context appContext() {
        return instance;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm == null) return;

        NotificationChannel general = new NotificationChannel(
                Constants.CHANNEL_GENERAL,
                "Thông báo chung",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        general.setDescription("Các thông báo hệ thống từ AuraPC Admin");
        nm.createNotificationChannel(general);

        NotificationChannel orders = new NotificationChannel(
                Constants.CHANNEL_ORDERS,
                "Đơn hàng",
                NotificationManager.IMPORTANCE_HIGH
        );
        orders.setDescription("Cập nhật đơn hàng realtime");
        nm.createNotificationChannel(orders);

        NotificationChannel support = new NotificationChannel(
                Constants.CHANNEL_SUPPORT,
                "Hỗ trợ khách hàng",
                NotificationManager.IMPORTANCE_HIGH
        );
        support.setDescription("Tin nhắn hỗ trợ từ khách hàng");
        nm.createNotificationChannel(support);
    }
}
