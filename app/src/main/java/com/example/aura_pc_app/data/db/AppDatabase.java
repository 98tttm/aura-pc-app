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
import com.example.aura_pc_app.data.db.dao.UserDao;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.data.db.entity.DeviceProfileEntity;
import com.example.aura_pc_app.data.db.entity.PriceHistoryEntity;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.db.entity.UserEntity;
import com.example.aura_pc_app.utils.Constants;

// 1. Thêm các Entity mới vào danh sách
@Database(entities = {
        UserEntity.class,
        ProductEntity.class,
        CartItemEntity.class,
        DeviceProfileEntity.class,
        PriceHistoryEntity.class
}, version = 4, exportSchema = true)
// 2. Khai báo bộ chuyển đổi
@TypeConverters({AuraTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    // 3. Khai báo các DAO
    public abstract UserDao userDao();
    public abstract ProductDao productDao();
    public abstract CartDao cartDao();

    // Dev/demo migration: recreate local cache tables from device database version 3.
    public static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("DROP TABLE IF EXISTS `users`");
            database.execSQL("DROP TABLE IF EXISTS `products`");
            database.execSQL("DROP TABLE IF EXISTS `cart_items`");
            database.execSQL("DROP TABLE IF EXISTS `device_profile`");
            database.execSQL("DROP TABLE IF EXISTS `price_history`");

            database.execSQL("CREATE TABLE IF NOT EXISTS `users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `username` TEXT, `email` TEXT, `token` TEXT)");
            database.execSQL("CREATE TABLE IF NOT EXISTS `products` (`_id` TEXT NOT NULL, `name` TEXT, `slug` TEXT, `price` REAL NOT NULL, `salePrice` REAL, `category_id` TEXT, `category_ids` TEXT, `images` TEXT, `specs` TEXT, `brand` TEXT, `stock` INTEGER NOT NULL, `active` INTEGER NOT NULL, PRIMARY KEY(`_id`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `cart_items` (`productId` TEXT NOT NULL, `quantity` INTEGER NOT NULL, PRIMARY KEY(`productId`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `device_profile` (`deviceId` TEXT NOT NULL, `isDarkMode` INTEGER NOT NULL, PRIMARY KEY(`deviceId`))");
            database.execSQL("CREATE TABLE IF NOT EXISTS `price_history` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` TEXT, `oldPrice` REAL NOT NULL, `updateTimestamp` INTEGER)");
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
                            .addMigrations(MIGRATION_3_4)
                            // Dev/demo fallback: old local data may be cleared when no migration exists.
                            .fallbackToDestructiveMigration()
                            .fallbackToDestructiveMigrationOnDowngrade()
                            .build();
                }
            }
        }
        return instance;
    }
}
