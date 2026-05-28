package com.example.aura_pc_app.data.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.ApiService;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.dao.UserDao;
import com.example.aura_pc_app.data.db.entity.UserEntity;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UserRepository {
    private final UserDao userDao;
    private final ApiService apiService;
    private final ExecutorService executor;

    public UserRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        userDao = db.userDao();
        apiService = ApiClient.getInstance(application).getApiService();
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<UserEntity> getUserById(int id) {
        return userDao.getById(id);
    }

    public void insertUser(UserEntity user) {
        executor.execute(() -> userDao.insert(user));
    }

    public void clearAllUsers() {
        executor.execute(userDao::deleteAll);
    }
}
