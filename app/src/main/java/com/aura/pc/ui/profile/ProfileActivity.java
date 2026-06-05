package com.aura.pc.ui.profile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aura.pc.ui.address.AddressBookActivity;
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

        // Mở "Quản lý sổ địa chỉ"
        View menuAddress = findViewById(R.id.menuAddress);
        if (menuAddress != null) {
            menuAddress.setOnClickListener(v ->
                    startActivity(new Intent(this, AddressBookActivity.class)));
        }

        // Các mục khác (sẽ phát triển sau) — hiện thông báo nhẹ để không gây nhầm
        setComingSoon(R.id.menuInfo);
        setComingSoon(R.id.menuOrders);
        setComingSoon(R.id.menuWishlist);

        View logout = findViewById(R.id.btnLogout);
        if (logout != null) {
            logout.setOnClickListener(v ->
                    Toast.makeText(this, getString(R.string.profile_logout), Toast.LENGTH_SHORT).show());
        }
    }

    private void setComingSoon(int id) {
        View v = findViewById(id);
        if (v != null) {
            v.setOnClickListener(view ->
                    Toast.makeText(this, "Sắp ra mắt", Toast.LENGTH_SHORT).show());
        }
    }
}
