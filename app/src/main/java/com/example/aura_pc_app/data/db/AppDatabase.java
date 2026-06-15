package com.example.aura_pc_app.data.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.aura_pc_app.data.db.dao.CartDao;
import com.example.aura_pc_app.data.db.dao.ProductDao;
import com.example.aura_pc_app.data.db.dao.SearchHistoryDao;
import com.example.aura_pc_app.data.db.dao.UserDao;
import com.example.aura_pc_app.data.db.dao.WishlistDao;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.data.db.entity.DeviceProfileEntity;
import com.example.aura_pc_app.data.db.entity.PriceHistoryEntity;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.db.entity.SearchHistoryEntity;
import com.example.aura_pc_app.data.db.entity.UserEntity;
import com.example.aura_pc_app.data.db.entity.WishlistEntity;
import com.example.aura_pc_app.utils.Constants;

@Database(entities = {
        UserEntity.class,
        ProductEntity.class,
        CartItemEntity.class,
        DeviceProfileEntity.class,
        PriceHistoryEntity.class,
        SearchHistoryEntity.class,
        WishlistEntity.class
}, version = 3, exportSchema = true)
@TypeConverters({AuraTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract UserDao userDao();
    public abstract ProductDao productDao();
    public abstract CartDao cartDao();
    public abstract WishlistDao wishlistDao();
    public abstract SearchHistoryDao searchHistoryDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS cart_items_new ("
                    + "productId TEXT NOT NULL, "
                    + "variantId TEXT NOT NULL DEFAULT '', "
                    + "name TEXT, "
                    + "specs TEXT, "
                    + "imageUrl TEXT, "
                    + "unitPrice REAL NOT NULL DEFAULT 0, "
                    + "quantity INTEGER NOT NULL, "
                    + "synced INTEGER NOT NULL DEFAULT 0, "
                    + "updatedAt INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY(productId, variantId))");
            database.execSQL("INSERT INTO cart_items_new (productId, variantId, quantity) "
                    + "SELECT productId, '', quantity FROM cart_items");
            database.execSQL("DROP TABLE cart_items");
            database.execSQL("ALTER TABLE cart_items_new RENAME TO cart_items");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS wishlist_items ("
                    + "productId TEXT NOT NULL, "
                    + "name TEXT, "
                    + "price REAL NOT NULL, "
                    + "oldPrice REAL NOT NULL, "
                    + "imageUrl TEXT, "
                    + "addedAt INTEGER NOT NULL, "
                    + "PRIMARY KEY(productId))");
            database.execSQL("CREATE TABLE IF NOT EXISTS search_history ("
                    + "normalizedKeyword TEXT NOT NULL, "
                    + "keyword TEXT, "
                    + "updatedAt INTEGER NOT NULL, "
                    + "PRIMARY KEY(normalizedKeyword))");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    Constants.DB_NAME
                            )
                            .addMigrations(MIGRATION_1_2)
                            .addMigrations(MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance;
    }
}
