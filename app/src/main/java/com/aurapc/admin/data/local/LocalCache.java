package com.aurapc.admin.data.local;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

import java.util.List;

/**
 * Thin wrapper around {@link AppDatabase} for offline caching of dashboard,
 * orders, and products. The Repository layer should call this on every
 * successful network response so that the next launch can re-hydrate the UI
 * immediately while a fresh fetch is in-flight.
 */
public final class LocalCache {

    private static final String TAG = "LocalCache";
    private static final long ONE_DAY = 24L * 60 * 60 * 1000;

    private final AppDatabase db;
    private final Gson gson = new Gson();

    public LocalCache(Context ctx) {
        this.db = AppDatabase.get(ctx);
    }

    public void putDashboard(Object data) {
        try {
            AppDatabase.DashboardCacheEntity e = new AppDatabase.DashboardCacheEntity();
            e.id = 1;
            e.dataJson = gson.toJson(data);
            e.updatedAt = System.currentTimeMillis();
            db.dashboardDao().upsert(e);
        } catch (Throwable t) {
            Log.w(TAG, "putDashboard failed", t);
        }
    }

    public String getDashboardJson() {
        androidx.lifecycle.LiveData<AppDatabase.DashboardCacheEntity> live = db.dashboardDao().observe();
        AppDatabase.DashboardCacheEntity e = live.getValue();
        return e != null ? e.dataJson : null;
    }

    public void putOrder(String orderNumber, Object data) {
        try {
            AppDatabase.OrderCacheEntity e = new AppDatabase.OrderCacheEntity();
            e.orderNumber = orderNumber;
            e.dataJson = gson.toJson(data);
            e.updatedAt = System.currentTimeMillis();
            db.orderCacheDao().upsert(e);
            db.orderCacheDao().prune(System.currentTimeMillis() - ONE_DAY * 7);
        } catch (Throwable t) {
            Log.w(TAG, "putOrder failed", t);
        }
    }

    public void putProduct(String productId, Object data) {
        try {
            AppDatabase.ProductCacheEntity e = new AppDatabase.ProductCacheEntity();
            e.productId = productId;
            e.dataJson = gson.toJson(data);
            e.updatedAt = System.currentTimeMillis();
            db.productCacheDao().upsert(e);
            db.productCacheDao().prune(System.currentTimeMillis() - ONE_DAY * 7);
        } catch (Throwable t) {
            Log.w(TAG, "putProduct failed", t);
        }
    }

    public androidx.lifecycle.LiveData<List<AppDatabase.OrderCacheEntity>> observeOrders() {
        return db.orderCacheDao().observeAll();
    }

    public androidx.lifecycle.LiveData<List<AppDatabase.ProductCacheEntity>> observeProducts() {
        return db.productCacheDao().observeAll();
    }
}