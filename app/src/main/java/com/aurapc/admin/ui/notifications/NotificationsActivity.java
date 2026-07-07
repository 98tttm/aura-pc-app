package com.aurapc.admin.ui.notifications;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.ApiClient;
import com.aurapc.admin.data.api.ContentApi;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationsActivity extends AppCompatActivity {

    private ProgressBar progress;
    private View emptyState;
    private RecyclerView rv;
    private NotificationsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView title = findViewById(R.id.tvTitle);
        TextView btnMarkAll = findViewById(R.id.btnMarkAll);
        progress = findViewById(R.id.progress);
        emptyState = findViewById(R.id.emptyState);
        rv = findViewById(R.id.rvNotifications);

        title.setText("Thông báo");
        btnBack.setOnClickListener(v -> finish());
        btnMarkAll.setOnClickListener(v -> markAllRead());

        adapter = new NotificationsAdapter(this::markRead);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().listNotifications(50, false), (Resource<ContentApi.NotificationListResponse> result) -> {
            progress.setVisibility(View.GONE);
            if (result.isSuccess() && result.data != null) {
                List<Map<String, Object>> items = result.data.items;
                if (items == null || items.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    adapter.setItems(items);
                }
            } else {
                emptyState.setVisibility(View.VISIBLE);
            }
        });
    }

    private void markRead(String id) {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().markNotificationRead(id), r -> load());
    }

    private void markAllRead() {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().markAllNotificationsRead(), r -> load());
    }
}
