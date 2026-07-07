package com.aurapc.admin.ui.warranty;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.WarrantyItem;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;

public class WarrantyAdapter extends RecyclerView.Adapter<WarrantyAdapter.VH> {

    private final List<WarrantyItem> items = new ArrayList<>();

    public void setItems(List<WarrantyItem> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_warranty, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        WarrantyItem w = items.get(pos);
        h.serial.setText(w.serialNumber != null ? w.serialNumber : "—");
        h.product.setText(w.productName != null ? w.productName : "Sản phẩm");
        h.customer.setText(w.customerName != null ? w.customerName : "Khách hàng");
        h.expiry.setText(Formatters.formatDate(w.expiryDate));
        String status = w.status != null ? w.status : "active";
        h.status.setText(mapStatus(status));
        h.status.setBackgroundResource(getStatusBg(status));
        h.status.setTextColor(getStatusColor(status));
    }

    private String mapStatus(String s) {
        switch (s) {
            case "active": return "Còn hạn";
            case "expired": return "Hết hạn";
            case "used": return "Đã dùng";
            default: return s;
        }
    }

    private int getStatusBg(String s) {
        switch (s) {
            case "active": return R.drawable.bg_badge_success;
            case "expired": return R.drawable.bg_badge_danger;
            default: return R.drawable.bg_badge_neutral;
        }
    }

    private int getStatusColor(String s) {
        switch (s) {
            case "active": return 0xFF16A34A;
            case "expired": return 0xFFDC2626;
            default: return 0xFFFFFFFF;
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView serial, product, customer, expiry, status;

        VH(View v) {
            super(v);
            serial = v.findViewById(R.id.tvSerial);
            product = v.findViewById(R.id.tvProduct);
            customer = v.findViewById(R.id.tvCustomer);
            expiry = v.findViewById(R.id.tvExpiry);
            status = v.findViewById(R.id.tvStatus);
        }
    }
}