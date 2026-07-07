package com.aurapc.admin.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * In-memory + SharedPreferences cache for offline support. Replaces Room so the
 * project doesn't need an annotation processor (Room generates *_Impl at compile
 * time which doesn't work in pure Java AGP 9.x without kapt/KSP).
 *
 * Stores dashboard stats, recent orders and recent products so the admin can
 * quickly see the state of the shop without an internet connection.
 *
 * The class signatures intentionally match the previous Room-generated classes
 * (DashboardCacheEntity, OrderCacheEntity, ProductCacheEntity, DashboardDao,
 * OrderCacheDao, ProductCacheDao) so callers don't have to change anything.
 */
public class AppDatabase {

    private static final String PREF = "aurapc_admin_cache";
    private static final String KEY_DASHBOARD = "dashboard_json";
    private static final String KEY_DASHBOARD_TS = "dashboard_ts";
    private static final String PREFIX_ORDER = "order:";
    private static final String PREFIX_PRODUCT = "product:";
    private static final long ONE_DAY = 24L * 60 * 60 * 1000;

    private static volatile AppDatabase INSTANCE;

    private final SharedPreferences prefs;
    private final DashboardDao dashboardDao;
    private final OrderCacheDao orderCacheDao;
    private final ProductCacheDao productCacheDao;

    public static AppDatabase get(Context ctx) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AppDatabase(ctx.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private AppDatabase(Context ctx) {
        this.prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        this.dashboardDao = new DashboardDao();
        this.orderCacheDao = new OrderCacheDao();
        this.productCacheDao = new ProductCacheDao();
    }

    public DashboardDao dashboardDao() { return dashboardDao; }
    public OrderCacheDao orderCacheDao() { return orderCacheDao; }
    public ProductCacheDao productCacheDao() { return productCacheDao; }

    // ----- Entities (kept as POJOs so existing callers compile unchanged) -----

    public static class DashboardCacheEntity {
        public int id; // singleton row
        public String dataJson;
        public long updatedAt;
    }

    public static class OrderCacheEntity {
        public String orderNumber;
        public String dataJson;
        public long updatedAt;
    }

    public static class ProductCacheEntity {
        public String productId;
        public String dataJson;
        public long updatedAt;
    }

    // ----- DAOs -----

    public class DashboardDao {
        private final MutableLiveData<DashboardCacheEntity> live = new MutableLiveData<>();

        DashboardDao() {
            DashboardCacheEntity e = new DashboardCacheEntity();
            e.id = 1;
            e.dataJson = prefs.getString(KEY_DASHBOARD, null);
            e.updatedAt = prefs.getLong(KEY_DASHBOARD_TS, 0L);
            live.postValue(e.dataJson != null ? e : null);
        }

        public LiveData<DashboardCacheEntity> observe() {
            return live;
        }

        public void upsert(DashboardCacheEntity e) {
            prefs.edit()
                    .putString(KEY_DASHBOARD, e.dataJson)
                    .putLong(KEY_DASHBOARD_TS, e.updatedAt)
                    .apply();
            live.postValue(e);
        }
    }

    public class OrderCacheDao {
        OrderCacheDao() { prune(System.currentTimeMillis() - ONE_DAY * 7); }

        public LiveData<List<OrderCacheEntity>> observeAll() {
            List<OrderCacheEntity> list = new ArrayList<>(loadAll(PREFIX_ORDER).values());
            Collections.sort(list, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
            if (list.size() > 100) list = list.subList(0, 100);
            MutableLiveData<List<OrderCacheEntity>> live = new MutableLiveData<>();
            live.postValue(list);
            return live;
        }

        public OrderCacheEntity get(String n) {
            Map<String, OrderCacheEntity> all = loadAll(PREFIX_ORDER);
            return all.get(n);
        }

        public void upsert(OrderCacheEntity e) {
            prefs.edit()
                    .putString(PREFIX_ORDER + e.orderNumber + ":json", e.dataJson)
                    .putLong(PREFIX_ORDER + e.orderNumber + ":ts", e.updatedAt)
                    .apply();
        }

        public void prune(long before) {
            SharedPreferences.Editor editor = prefs.edit();
            for (OrderCacheEntity e : loadAll(PREFIX_ORDER).values()) {
                if (e.updatedAt < before) {
                    editor.remove(PREFIX_ORDER + e.orderNumber + ":json");
                    editor.remove(PREFIX_ORDER + e.orderNumber + ":ts");
                }
            }
            editor.apply();
        }
    }

    public class ProductCacheDao {
        ProductCacheDao() { prune(System.currentTimeMillis() - ONE_DAY * 7); }

        public LiveData<List<ProductCacheEntity>> observeAll() {
            List<ProductCacheEntity> list = new ArrayList<>(loadAllProducts().values());
            Collections.sort(list, (a, b) -> Long.compare(b.updatedAt, a.updatedAt));
            if (list.size() > 100) list = list.subList(0, 100);
            MutableLiveData<List<ProductCacheEntity>> live = new MutableLiveData<>();
            live.postValue(list);
            return live;
        }

        public void upsert(ProductCacheEntity e) {
            prefs.edit()
                    .putString(PREFIX_PRODUCT + e.productId + ":json", e.dataJson)
                    .putLong(PREFIX_PRODUCT + e.productId + ":ts", e.updatedAt)
                    .apply();
        }

        public void prune(long before) {
            SharedPreferences.Editor editor = prefs.edit();
            for (ProductCacheEntity e : loadAllProducts().values()) {
                if (e.updatedAt < before) {
                    editor.remove(PREFIX_PRODUCT + e.productId + ":json");
                    editor.remove(PREFIX_PRODUCT + e.productId + ":ts");
                }
            }
            editor.apply();
        }
    }

    // ----- Helpers -----

    private Map<String, OrderCacheEntity> loadAll(String prefix) {
        Map<String, OrderCacheEntity> out = new HashMap<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(PREFIX_ORDER) && key.endsWith(":json")) {
                String orderNumber = key.substring(PREFIX_ORDER.length(), key.length() - ":json".length());
                OrderCacheEntity e = new OrderCacheEntity();
                e.orderNumber = orderNumber;
                e.dataJson = (String) entry.getValue();
                e.updatedAt = prefs.getLong(PREFIX_ORDER + orderNumber + ":ts", 0L);
                out.put(orderNumber, e);
            }
        }
        return out;
    }

    private Map<String, ProductCacheEntity> loadAllProducts() {
        Map<String, ProductCacheEntity> out = new HashMap<>();
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(PREFIX_PRODUCT) && key.endsWith(":json")) {
                String productId = key.substring(PREFIX_PRODUCT.length(), key.length() - ":json".length());
                ProductCacheEntity e = new ProductCacheEntity();
                e.productId = productId;
                e.dataJson = (String) entry.getValue();
                e.updatedAt = prefs.getLong(PREFIX_PRODUCT + productId + ":ts", 0L);
                out.put(productId, e);
            }
        }
        return out;
    }
}