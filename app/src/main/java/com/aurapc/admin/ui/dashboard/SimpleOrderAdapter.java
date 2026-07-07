package com.aurapc.admin.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Order;
import com.aurapc.admin.utils.Formatters;

import java.util.Collections;
import java.util.List;

public class SimpleOrderAdapter extends RecyclerView.Adapter<SimpleOrderAdapter.VH> {

    public interface OnClick {
        void onClick(Order order);
    }

    private final List<Order> orders;
    private final OnClick onClick;

    public SimpleOrderAdapter(List<Order> orders) {
        this(orders, null);
    }

    public SimpleOrderAdapter(List<Order> orders, OnClick onClick) {
        this.orders = orders != null ? orders : Collections.emptyList();
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Order o = orders.get(position);
        holder.tvOrderNumber.setText(o.orderNumber != null ? o.orderNumber : "#" + (o.id != null ? o.id : o._id != null ? o._id : "?"));
        String custName = "";
        if (o.shippingAddress != null) {
            if (o.shippingAddress.name != null) custName = o.shippingAddress.name;
            else if (o.shippingAddress.fullName != null) custName = o.shippingAddress.fullName;
        }
        holder.tvCustomer.setText(custName.isEmpty() ? "Khách" : custName);
        Double total = o.totalAmount != null ? o.totalAmount : o.total;
        holder.tvTotal.setText(Formatters.formatVnd(total));
        holder.tvCreatedAt.setText(Formatters.formatDate(o.createdAt));
        holder.tvItemCount.setText(o.items != null ? o.items.size() + " sản phẩm" : "");

        if (holder.tvStatus != null && o.status != null) {
            holder.tvStatus.setText(mapStatus(o.status));
        }
        holder.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(o);
        });
    }

    private String mapStatus(String s) {
        switch (s) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "processing": return "Đang xử lý";
            case "shipped": return "Đang giao";
            case "delivered": return "Đã giao";
            case "cancelled": return "Đã hủy";
            default: return s;
        }
    }

    @Override
    public int getItemCount() { return orders.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvOrderNumber, tvCustomer, tvTotal, tvStatus,
                tvItemCount, tvPayment, tvCreatedAt;

        VH(View itemView) {
            super(itemView);
            tvOrderNumber = itemView.findViewById(R.id.tvOrderNumber);
            tvCustomer = itemView.findViewById(R.id.tvCustomer);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            tvPayment = itemView.findViewById(R.id.tvPayment);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
        }
    }
}
