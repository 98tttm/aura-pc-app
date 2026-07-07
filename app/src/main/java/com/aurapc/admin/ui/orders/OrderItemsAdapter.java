package com.aurapc.admin.ui.orders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Order;
import com.aurapc.admin.utils.Formatters;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.VH> {

    private List<Order.OrderItem> items = new ArrayList<>();

    public OrderItemsAdapter(List<Order.OrderItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Order.OrderItem item = items.get(pos);
        String name = item.name != null ? item.name : "Sản phẩm";
        h.tvName.setText(name);
        Integer qty = item.quantity != null ? item.quantity : (item.qty != null ? item.qty : 1);
        h.tvQty.setText("x" + qty);
        double price = item.price != null ? item.price : 0;
        double total = price * qty;
        h.tvPrice.setText(Formatters.formatVnd(total));
        if (item.serialNumber != null && !item.serialNumber.isEmpty()) {
            h.tvSerial.setText("SN: " + item.serialNumber);
            h.tvSerial.setVisibility(View.VISIBLE);
        } else {
            h.tvSerial.setVisibility(View.GONE);
        }
        if (item.image != null && !item.image.isEmpty()) {
            Glide.with(h.ivImage.getContext()).load(item.image).placeholder(R.drawable.ic_box).into(h.ivImage);
        } else {
            h.ivImage.setImageResource(R.drawable.ic_box);
        }
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvQty, tvSerial, tvPrice;

        VH(View v) {
            super(v);
            ivImage = v.findViewById(R.id.ivImage);
            tvName = v.findViewById(R.id.tvName);
            tvQty = v.findViewById(R.id.tvQty);
            tvSerial = v.findViewById(R.id.tvSerial);
            tvPrice = v.findViewById(R.id.tvPrice);
        }
    }
}
