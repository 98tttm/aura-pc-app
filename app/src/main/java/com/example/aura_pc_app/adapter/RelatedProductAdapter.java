package com.example.aura_pc_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.domain.model.Product;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import com.bumptech.glide.Glide;

public class RelatedProductAdapter extends RecyclerView.Adapter<RelatedProductAdapter.ViewHolder> {
    private List<Map<String, Object>> products;
    private OnProductClickListener listener;

    public interface OnProductClickListener {
        void onProductClick(Map<String, Object> product);
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    public RelatedProductAdapter(List<Map<String, Object>> products) {
        this.products = products;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_related_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> product = products.get(position);
        
        String name = getStr(product, "name");
        holder.name.setText(name);
        
        double price = getNumber(product, "price");
        holder.price.setText(formatCurrency(price));
        
        List<String> images = null;
        try {
            images = (List<String>) product.get("images");
        } catch (Exception ignored) {}
        
        if (images != null && !images.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                 .load(images.get(0))
                 .placeholder(R.drawable.aura_laptop)
                 .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.aura_laptop);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
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

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof String ? (String) val : "";
    }

    private double getNumber(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.productImage);
            name = itemView.findViewById(R.id.productName);
            price = itemView.findViewById(R.id.productPrice);
        }
    }
}
