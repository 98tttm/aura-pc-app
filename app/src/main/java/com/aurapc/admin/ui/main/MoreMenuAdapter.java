package com.aurapc.admin.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;

import java.util.List;

public class MoreMenuAdapter extends RecyclerView.Adapter<MoreMenuAdapter.VH> {

    public interface OnClick { void onClick(); }
    public static class MenuItem {
        public final int icon;
        public final String title;
        public final String subtitle;
        public final OnClick onClick;
        public MenuItem(int icon, String title, String subtitle, OnClick onClick) {
            this.icon = icon; this.title = title; this.subtitle = subtitle; this.onClick = onClick;
        }
    }

    private final List<MenuItem> items;

    public MoreMenuAdapter(List<MenuItem> items) { this.items = items; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_more_menu, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        MenuItem item = items.get(pos);
        h.icon.setImageResource(item.icon);
        h.title.setText(item.title);
        h.subtitle.setText(item.subtitle);
        h.itemView.setOnClickListener(v -> item.onClick.onClick());
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title, subtitle;
        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.icon);
            title = v.findViewById(R.id.title);
            subtitle = v.findViewById(R.id.subtitle);
        }
    }
}