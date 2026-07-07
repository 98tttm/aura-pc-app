package com.aurapc.admin.ui.notifications;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NotificationsAdapter extends RecyclerView.Adapter<NotificationsAdapter.VH> {

    public interface OnClick {
        void onClick(String id);
    }

    private final List<Map<String, Object>> items = new ArrayList<>();
    private final OnClick onClick;

    public NotificationsAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void setItems(List<Map<String, Object>> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Map<String, Object> n = items.get(position);
        h.title.setText(safe(n, "title"));
        h.message.setText(safe(n, "message"));
        Object createdAt = n.get("createdAt");
        h.time.setText(createdAt != null ? Formatters.formatDateTime(createdAt.toString()) : "");

        Boolean read = (Boolean) n.get("read");
        if (read != null && read) {
            h.unread.setVisibility(View.GONE);
        } else {
            h.unread.setVisibility(View.VISIBLE);
        }

        Object id = n.get("_id");
        h.itemView.setOnClickListener(v -> {
            if (id != null && onClick != null) onClick.onClick(id.toString());
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String safe(Map<String, Object> n, String key) {
        Object v = n.get(key);
        return v != null ? v.toString() : "";
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, message, time, unread;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.tvTitle);
            message = v.findViewById(R.id.tvMessage);
            time = v.findViewById(R.id.tvTime);
            unread = v.findViewById(R.id.tvUnread);
        }
    }
}
