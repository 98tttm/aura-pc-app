package com.aura.pc.ui.profile;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

public class ProfileActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.requireLogin(this, ProfileActivity.class)) {
            finish();
            return;
        }

        setContentView(R.layout.activity_profile);
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_PROFILE);
        BottomNavigationHelper.setupHeader(this);
    }
}
