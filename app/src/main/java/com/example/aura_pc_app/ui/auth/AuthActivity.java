package com.example.aura_pc_app.ui.auth;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.example.aura_pc_app.databinding.ActivityAuthBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;

public class AuthActivity extends BaseActivity<ActivityAuthBinding> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Set up auth UI
    }

    @Override
    protected ActivityAuthBinding inflateBinding() {
        return ActivityAuthBinding.inflate(getLayoutInflater());
    }
}
