package com.aurapc.admin.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Order;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;

public class OrdersAdapter extends RecyclerView.Adapter<OrdersAdapter.VH> {

    private List<Order> orders = new ArrayList<>();
    private final OnOrderClick listener;

    public interface OnOrderClick { void onClick(Order order); }

    public OrdersAdapter(OnOrderClick listener) { this.listener = listener; }

    public void setOrders(List<Order> orders) {
        this.orders = orders != null ? orders : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Order o = orders.get(pos);
        h.tvOrderNumber.setText(o.orderNumber != null ? o.orderNumber : "#" + (o.id != null ? o.id : ""));
        String custName = "";
        if (o.shippingAddress != null) {
            if (o.shippingAddress.name != null) custName = o.shippingAddress.name;
            else if (o.shippingAddress.fullName != null) custName = o.shippingAddress.fullName;
        }
        h.tvCustomer.setText(custName.isEmpty() ? "Khách" : custName);
        Double total = o.totalAmount != null ? o.totalAmount : o.total;
        h.tvTotal.setText(Formatters.formatVnd(total));
        h.tvCreatedAt.setText(Formatters.formatDateTime(o.createdAt));
        h.tvItemCount.setText(o.items != null ? o.items.size() + " sản phẩm" : "");

        String status = o.status != null ? o.status : "pending";
        h.tvStatus.setText(getStatusLabel(status));
        h.tvStatus.setBackgroundResource(getStatusBg(status));
        h.tvStatus.setTextColor(getStatusTextColor(status));
        h.tvPayment.setText(o.paymentMethod != null ? o.paymentMethod.toUpperCase() : "COD");

        h.itemView.setOnClickListener(v -> listener.onClick(o));
    }

    private String getStatusLabel(String status) {
        switch (status) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "processing": return "Đang xử lý";
            case "shipping": return "Đang giao";
            case "delivered": return "Đã giao";
            case "cancelled": return "Đã hủy";
            case "refunded": return "Hoàn tiền";
            default: return status;
        }
    }

    private int getStatusBg(String status) {
        switch (status) {
            case "pending": return R.drawable.bg_badge_warning;
            case "confirmed": return R.drawable.bg_badge_accent;
            case "shipping": return R.drawable.bg_badge_info;
            case "delivered": return R.drawable.bg_badge_success;
            case "cancelled": case "refunded": return R.drawable.bg_badge_danger;
            default: return R.drawable.bg_badge_neutral;
        }
    }

    private int getStatusTextColor(String status) {
        switch (status) {
            case "pending": return 0xFFEAB308;
            case "confirmed": return 0xFFEA580C;
            case "shipping": return 0xFF0284C7;
            case "delivered": return 0xFF16A34A;
            case "cancelled": case "refunded": return 0xFFDC2626;
            default: return 0xFFFFFFFF;
        }
    }

    @Override public int getItemCount() { return orders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvOrderNumber, tvCustomer, tvTotal, tvStatus,
                tvItemCount, tvPayment, tvCreatedAt;
        View btnAction;

        VH(View v) {
            super(v);
            cbSelect = v.findViewById(R.id.cbSelect);
            tvOrderNumber = v.findViewById(R.id.tvOrderNumber);
            tvCustomer = v.findViewById(R.id.tvCustomer);
            tvTotal = v.findViewById(R.id.tvTotal);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvItemCount = v.findViewById(R.id.tvItemCount);
            tvPayment = v.findViewById(R.id.tvPayment);
            tvCreatedAt = v.findViewById(R.id.tvCreatedAt);
            btnAction = v.findViewById(R.id.btnAction);
        }
    }
}
