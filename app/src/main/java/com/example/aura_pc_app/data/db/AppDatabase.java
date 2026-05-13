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
}, version = 1, exportSchema = true)
// 2. Khai báo bộ chuyển đổi
@TypeConverters({AuraTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    // 3. Khai báo các DAO
    public abstract UserDao userDao();
    public abstract ProductDao productDao();
    public abstract CartDao cartDao();

    // 4. Khung Skeleton cho việc nâng cấp phiên bản (Migration)
    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Chuẩn bị cho các đợt update DB sau này
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
                            .addMigrations(MIGRATION_1_2) // 5. Gắn Migration vào Builder
                            .build();
                }
            }
        }
        return instance;
    }
}