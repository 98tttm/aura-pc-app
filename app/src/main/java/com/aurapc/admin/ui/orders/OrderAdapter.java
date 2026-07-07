package com.aurapc.admin.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Order;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;

public class OrderAdapter extends ListAdapter<Order, OrderAdapter.VH> {

    private final OnOrderClickListener listener;
    private boolean selectionMode = false;
    private final ArrayList<String> selectedIds = new ArrayList<>();

    public OrderAdapter(OnOrderClickListener listener) {
        super(new DiffUtil.ItemCallback<Order>() {
            @Override
            public boolean areItemsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
                return oldItem.id.equals(newItem.id);
            }
            @Override
            public boolean areContentsTheSame(@NonNull Order oldItem, @NonNull Order newItem) {
                return oldItem.id.equals(newItem.id);
            }
        });
        this.listener = listener;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Order order = getItem(position);
        holder.bind(order, this);
    }

    public void toggleSelection(String id) {
        if (selectedIds.contains(id)) selectedIds.remove(id);
        else selectedIds.add(id);
        selectionMode = !selectedIds.isEmpty();
        notifyDataSetChanged();
    }

    public ArrayList<String> getSelectedIds() { return new ArrayList<>(selectedIds); }

    public void clearSelection() {
        selectedIds.clear();
        selectionMode = false;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() { return getCurrentList().size(); }

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    static class VH extends RecyclerView.ViewHolder {
        CheckBox cbSelect;
        TextView tvOrderNumber, tvCustomer, tvTotal, tvStatus,
                tvItemCount, tvPayment, tvCreatedAt;
        ImageButton btnAction;

        VH(View itemView) {
            super(itemView);
            cbSelect = itemView.findViewById(R.id.cbSelect);
            tvOrderNumber = itemView.findViewById(R.id.tvOrderNumber);
            tvCustomer = itemView.findViewById(R.id.tvCustomer);
            tvTotal = itemView.findViewById(R.id.tvTotal);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvItemCount = itemView.findViewById(R.id.tvItemCount);
            tvPayment = itemView.findViewById(R.id.tvPayment);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            btnAction = itemView.findViewById(R.id.btnAction);
        }

        void bind(Order order, OrderAdapter adapter) {
            tvOrderNumber.setText(order.orderNumber != null ? order.orderNumber : "#" + order.id);
            String custName = order.shippingAddress != null && order.shippingAddress.name != null ? order.shippingAddress.name : "";
            tvCustomer.setText(custName.isEmpty() ? "Khách hàng" : custName);
            tvTotal.setText(Formatters.formatVnd(order.totalAmount));
            tvStatus.setText(order.status != null ? getStatusLabel(order.status) : "");
            tvStatus.setBackgroundResource(getStatusBadge(order.status));
            tvStatus.setTextColor(getStatusColor(order.status));
            tvItemCount.setText(order.items != null ? order.items.size() + " sản phẩm" : "");
            tvPayment.setText(order.paymentMethod != null ? order.paymentMethod.toUpperCase() : "COD");
            tvCreatedAt.setText(Formatters.formatDate(order.createdAt));

            cbSelect.setVisibility(adapter.selectionMode ? View.VISIBLE : View.GONE);
            cbSelect.setChecked(adapter.selectedIds.contains(order.id));
            cbSelect.setOnClickListener(v -> adapter.toggleSelection(order.id));

            itemView.setOnClickListener(v -> {
                if (adapter.selectionMode) {
                    adapter.toggleSelection(order.id);
                } else {
                    ((OnOrderClickListener) itemView.getContext()).onOrderClick(order);
                }
            });
        }

        private String getStatusLabel(String status) {
            if (status == null) return "";
            switch (status) {
                case "pending": return "Chờ xác nhận";
                case "confirmed": return "Đã xác nhận";
                case "processing": return "Đang xử lý";
                case "shipping": return "Đang giao";
                case "delivered": return "Hoàn thành";
                case "cancelled": return "Đã hủy";
                default: return status;
            }
        }

        private int getStatusBadge(String status) {
            if (status == null) return R.drawable.bg_badge_neutral;
            switch (status) {
                case "pending": return R.drawable.bg_badge_warning;
                case "confirmed": return R.drawable.bg_badge_accent;
                case "processing": return R.drawable.bg_badge_info;
                case "shipping": return R.drawable.bg_badge_accent;
                case "delivered": return R.drawable.bg_badge_success;
                case "cancelled": return R.drawable.bg_badge_danger;
                default: return R.drawable.bg_badge_neutral;
            }
        }

        private int getStatusColor(String status) {
            if (status == null) return itemView.getContext().getColor(R.color.text_primary);
            switch (status) {
                case "pending": return itemView.getContext().getColor(R.color.status_warning);
                case "confirmed": return itemView.getContext().getColor(R.color.aura_orange);
                case "processing": return itemView.getContext().getColor(R.color.status_info);
                case "shipping": return itemView.getContext().getColor(R.color.aura_orange);
                case "delivered": return itemView.getContext().getColor(R.color.status_success);
                case "cancelled": return itemView.getContext().getColor(R.color.status_danger);
                default: return itemView.getContext().getColor(R.color.text_primary);
            }
        }
    }
}