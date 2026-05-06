package com.example.aura_pc_app.domain.usecase;

import androidx.lifecycle.LiveData;
import com.example.aura_pc_app.domain.model.User;
import com.example.aura_pc_app.domain.repository.UserRepository;

public class GetUserUseCase {
    private final UserRepository repository;

    public GetUserUseCase(UserRepository repository) {
        this.repository = repository;
    }

    public LiveData<User> execute(int userId) {
        return repository.getUserById(userId);
    }
}
