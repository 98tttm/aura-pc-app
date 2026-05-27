package com.example.aura_pc_app.ui.home;

import android.os.Bundle;
import androidx.annotation.Nullable;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.databinding.ActivityHomeBinding;
import com.example.aura_pc_app.ui.base.BaseActivity;

public class HomeActivity extends BaseActivity<ActivityHomeBinding> {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize Bottom Navigation
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_HOME);
    }

    @Override
    protected ActivityHomeBinding inflateBinding() {
        return ActivityHomeBinding.inflate(getLayoutInflater());
    }
}
