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
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.data.db.entity.DeviceProfileEntity;
import com.example.aura_pc_app.data.db.entity.PriceHistoryEntity;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.example.aura_pc_app.data.db.entity.SearchHistoryEntity;
import com.example.aura_pc_app.data.db.entity.UserEntity;
import com.example.aura_pc_app.utils.Constants;

@Database(entities = {
        UserEntity.class,
        ProductEntity.class,
        CartItemEntity.class,
        DeviceProfileEntity.class,
        PriceHistoryEntity.class,
        SearchHistoryEntity.class
}, version = 2, exportSchema = true)
@TypeConverters({AuraTypeConverters.class})
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;

    public abstract UserDao userDao();
    public abstract ProductDao productDao();
    public abstract CartDao cartDao();
    public abstract SearchHistoryDao searchHistoryDao();

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `search_history` (" +
                    "`normalizedKeyword` TEXT NOT NULL, " +
                    "`keyword` TEXT, " +
                    "`updatedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`normalizedKeyword`))");
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
                            .build();
                }
            }
        }
        return instance;
    }
}
