package com.aurapc.admin.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.DashboardTopProduct;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;

public class TopProductsAdapter extends RecyclerView.Adapter<TopProductsAdapter.VH> {

    public interface OnClick {
        void onClick(DashboardTopProduct item);
    }

    private final List<DashboardTopProduct> items = new ArrayList<>();
    private final OnClick onClick;

    public TopProductsAdapter(List<DashboardTopProduct> data) {
        this(data, null);
    }

    public TopProductsAdapter(List<DashboardTopProduct> data, OnClick onClick) {
        this.onClick = onClick;
        if (data != null) items.addAll(data);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_top_product, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        DashboardTopProduct p = items.get(position);
        h.name.setText(p.name != null ? p.name : "—");
        h.qty.setText((p.totalQty != null ? p.totalQty : 0) + " đã bán");
        h.revenue.setText(Formatters.formatVnd(p.totalRevenue));
        h.position.setText("#" + (position + 1));
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView position, name, qty, revenue;
        ImageView icon;

        VH(View v) {
            super(v);
            position = v.findViewById(R.id.tvPosition);
            name = v.findViewById(R.id.tvName);
            qty = v.findViewById(R.id.tvQty);
            revenue = v.findViewById(R.id.tvRevenue);
            icon = v.findViewById(R.id.ivIcon);
        }
    }
}
