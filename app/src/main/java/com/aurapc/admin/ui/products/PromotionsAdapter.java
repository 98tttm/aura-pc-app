package com.aurapc.admin.ui.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Promotion;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;

public class PromotionsAdapter extends RecyclerView.Adapter<PromotionsAdapter.VH> {

    private final List<Promotion> items = new ArrayList<>();

    public void setPromotions(List<Promotion> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promotion, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Promotion p = items.get(pos);
        h.code.setText(p.code != null ? p.code : "—");
        h.name.setText(p.name != null ? p.name : "Khuyến mãi");
        h.discount.setText(formatDiscount(p));
        h.usage.setText("Đã dùng " + (p.usedCount != null ? p.usedCount : 0));
        boolean active = p.isActive != null ? p.isActive : true;
        h.status.setText(active ? "Đang kích hoạt" : "Tạm dừng");
        h.status.setBackgroundResource(active ? R.drawable.bg_badge_success : R.drawable.bg_badge_neutral);
        h.status.setTextColor(active ? 0xFF16A34A : 0xFFFFFFFF);
        h.expiry.setText(Formatters.formatDate(p.endDate != null ? p.endDate : p.expiresAt));
    }

    private String formatDiscount(Promotion p) {
        if (p.discountType == null) return "—";
        switch (p.discountType) {
            case "percent":
            case "percentage":
                return "-" + (p.discountValue != null ? p.discountValue : 0) + "%";
            case "fixed":
                return "-" + Formatters.formatVnd(p.discountValue);
            default:
                return p.discountType;
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView code, name, discount, usage, status, expiry;

        VH(View v) {
            super(v);
            code = v.findViewById(R.id.tvCode);
            name = v.findViewById(R.id.tvName);
            discount = v.findViewById(R.id.tvDiscount);
            usage = v.findViewById(R.id.tvUsage);
            status = v.findViewById(R.id.tvStatus);
            expiry = v.findViewById(R.id.tvExpiry);
        }
    }
}