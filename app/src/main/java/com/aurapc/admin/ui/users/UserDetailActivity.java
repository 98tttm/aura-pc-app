package com.aurapc.admin.ui.users;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.ApiClient;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.CustomerUser;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.ui.orders.OrdersAdapter;
import com.aurapc.admin.utils.Formatters;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.List;
import java.util.Map;

public class UserDetailActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "user_id";

    private TextView tvName, tvPhone, tvEmail, tvOrderCount, tvTotalSpent, tvAvatarInit, tvStatus;
    private MaterialSwitch swActive;
    private ProgressBar progress;
    private RecyclerView rvOrders;
    private OrdersAdapter ordersAdapter;

    private CustomerUser user;

    public static void start(android.content.Context ctx, String userId) {
        android.content.Intent i = new android.content.Intent(ctx, UserDetailActivity.class);
        i.putExtra(EXTRA_USER_ID, userId);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvEmail = findViewById(R.id.tvEmail);
        tvOrderCount = findViewById(R.id.tvOrderCount);
        tvTotalSpent = findViewById(R.id.tvTotalSpent);
        tvAvatarInit = findViewById(R.id.tvAvatarInit);
        swActive = findViewById(R.id.swActive);
        progress = findViewById(R.id.progress);
        tvStatus = findViewById(R.id.tvStatus);
        rvOrders = findViewById(R.id.rvOrders);

        ordersAdapter = new OrdersAdapter(o -> {
            // ignore
        });
        rvOrders.setLayoutManager(new LinearLayoutManager(this));
        rvOrders.setAdapter(ordersAdapter);

        toolbar.setNavigationOnClickListener(v -> finish());

        swActive.setOnCheckedChangeListener((b, isChecked) -> {
            if (user != null && user.id != null) {
                toggleStatus(isChecked);
            }
        });

        loadUser();
    }

    private void toggleStatus(boolean isActive) {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.userApi().updateUserStatus(user.id, isActive), (Resource<CustomerUser> result) -> {
            if (result.isSuccess()) {
                Toast.makeText(this, "Đã cập nhật", Toast.LENGTH_SHORT).show();
                tvStatus.setText(isActive ? "Hoạt động" : "Đã khóa");
                tvStatus.setBackgroundResource(isActive ? R.drawable.bg_badge_success : R.drawable.bg_badge_danger);
                tvStatus.setTextColor(isActive ? 0xFF16A34A : 0xFFDC2626);
            } else {
                Toast.makeText(this, "Lỗi: " + (result.message != null ? result.message : "Không thể cập nhật"), Toast.LENGTH_LONG).show();
                swActive.setChecked(user.active());
            }
        });
    }

    private void loadUser() {
        String userId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (userId == null) { finish(); return; }
        progress.setVisibility(View.VISIBLE);
        LiveData<Resource<CustomerUser>> ld = NetworkHelper.toLiveData(
                ServiceLocator.get().apiClient().userApi().getUserDetail(userId));
        ld.observe(this, result -> {
            progress.setVisibility(View.GONE);
            if (result.isSuccess()) {
                user = result.data;
                bindUser(user);
            }
        });
    }

    private void bindUser(CustomerUser user) {
        if (user == null) return;
        String name = user.getDisplayName();
        tvName.setText(name);
        tvAvatarInit.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        tvPhone.setText(user.getPhone() != null ? user.getPhone() : "—");
        tvEmail.setText(user.email != null ? user.email : "—");
        int orders = user.orderCount != null ? user.orderCount : 0;
        tvOrderCount.setText(String.valueOf(orders));
        double spent = user.totalSpent != null ? user.totalSpent : 0;
        tvTotalSpent.setText(Formatters.formatVnd(spent));
        swActive.setChecked(user.active());
        tvStatus.setText(user.active() ? "Hoạt động" : "Đã khóa");
        tvStatus.setBackgroundResource(user.active() ? R.drawable.bg_badge_success : R.drawable.bg_badge_danger);
        tvStatus.setTextColor(user.active() ? 0xFF16A34A : 0xFFDC2626);

        if (user.recentOrders != null) {
            ordersAdapter.setOrders(user.recentOrders);
        }
    }
}