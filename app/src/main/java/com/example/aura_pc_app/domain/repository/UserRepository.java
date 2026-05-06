package com.example.aura_pc_app.domain.repository;

import androidx.lifecycle.LiveData;
import com.example.aura_pc_app.domain.model.User;

public interface UserRepository {
    LiveData<User> getUserById(int id);
    void saveUser(User user);
    void deleteAll();
}
