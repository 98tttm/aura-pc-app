package com.example.aura_pc_app.adapter;

import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

/**
 * Adapter hiển thị danh sách sản phẩm theo danh mục (grid 2 cột).
 * Mỗi item hiển thị: ảnh, tên, giá hiện tại, giá cũ (gạch ngang).
 */
public class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Map<String, Object> product);
    }

    private final List<Map<String, Object>> products;
    private OnProductClickListener listener;

    public ProductListAdapter(List<Map<String, Object>> products) {
        this.products = products;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> product = products.get(position);

        // Tên sản phẩm
        String name = getStr(product, "name");
        holder.tvProductName.setText(name);

        // Giá hiện tại
        double price = getNumber(product, "price");
        holder.tvProductPrice.setText(formatCurrency(price));

        // Giá cũ (nếu có)
        double oldPrice = getNumber(product, "old_price");
        if (oldPrice > 0 && oldPrice > price) {
            holder.tvProductOldPrice.setText(formatCurrency(oldPrice));
            holder.tvProductOldPrice.setPaintFlags(
                    holder.tvProductOldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvProductOldPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvProductOldPrice.setVisibility(View.GONE);
        }

        // Ảnh sản phẩm (lấy ảnh đầu tiên từ mảng images)
        List<String> images = null;
        try {
            images = (List<String>) product.get("images");
        } catch (Exception ignored) {}

        if (images != null && !images.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(images.get(0))
                    .placeholder(R.drawable.aura_laptop)
                    .centerCrop()
                    .into(holder.ivProductImage);
        } else {
            holder.ivProductImage.setImageResource(R.drawable.aura_laptop);
        }

        // Click vào sản phẩm
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductClick(product);
        });
    }

    @Override
    public int getItemCount() {
        return products != null ? products.size() : 0;
    }

    private String formatCurrency(double amount) {
        DecimalFormat formatter = new DecimalFormat("#,###");
        return formatter.format(amount).replace(",", ".") + "đ";
    }

    private static String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : "";
    }

    private static double getNumber(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProductImage;
        TextView tvProductName;
        TextView tvProductPrice;
        TextView tvProductOldPrice;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            tvProductOldPrice = itemView.findViewById(R.id.tvProductOldPrice);
        }
    }
}
