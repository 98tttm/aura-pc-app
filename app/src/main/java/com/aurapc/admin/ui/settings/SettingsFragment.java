package com.aurapc.admin.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.aurapc.admin.R;
import com.aurapc.admin.data.local.TokenManager;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.ui.auth.LoginActivity;

public class SettingsFragment extends Fragment {

    private TextView tvName, tvEmail, tvRole, tvAvatarInit;
    private View rowBiometric;
    private com.google.android.material.materialswitch.MaterialSwitch swBiometric;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_settings, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        TokenManager tm = ServiceLocator.get().tokenManager();
        tvName = v.findViewById(R.id.tvName);
        tvEmail = v.findViewById(R.id.tvEmail);
        tvRole = v.findViewById(R.id.tvRole);
        tvAvatarInit = v.findViewById(R.id.tvAvatarInit);
        swBiometric = v.findViewById(R.id.swBiometric);
        rowBiometric = v.findViewById(R.id.rowBiometric);
        View rowLogout = v.findViewById(R.id.rowLogout);
        View rowAccounts = v.findViewById(R.id.rowAdminAccounts);
        rowAccounts.setOnClickListener(x ->
                startActivity(new Intent(requireContext(), com.aurapc.admin.ui.settings.AdminAccountsActivity.class)));

        String name = tm.getAdminName() != null ? tm.getAdminName() : "Admin";
        tvName.setText(name);
        tvEmail.setText(tm.getAdminEmail() != null ? tm.getAdminEmail() : "");
        String role = tm.getAdminRole();
        tvRole.setText("Super Admin".equals(role) ? "Super Admin" :
                "order_manager".equals(role) ? "Quản lý đơn hàng" :
                "product_manager".equals(role) ? "Quản lý sản phẩm" :
                "support_agent".equals(role) ? "Hỗ trợ" : role);
        String firstChar = name.isEmpty() ? "A" : String.valueOf(name.charAt(0)).toUpperCase();
        tvAvatarInit.setText(firstChar);
        swBiometric.setChecked(tm.isBiometricEnabled());

        swBiometric.setOnCheckedChangeListener((b, checked) -> {
            tm.setBiometricEnabled(checked);
        });

        rowLogout.setOnClickListener(x -> {
            ServiceLocator.get().tokenManager().logout();
            Intent i = new Intent(requireContext(), LoginActivity.class);
            startActivity(i);
            requireActivity().finishAffinity();
        });
    }
}