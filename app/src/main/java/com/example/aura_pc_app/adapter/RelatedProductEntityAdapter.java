package com.example.aura_pc_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.entity.ProductEntity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class RelatedProductEntityAdapter
        extends RecyclerView.Adapter<RelatedProductEntityAdapter.ViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(ProductEntity product);
    }

    private final List<ProductEntity> products;
    private final OnProductClickListener listener;
    private final NumberFormat currency = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    public RelatedProductEntityAdapter(List<ProductEntity> products, OnProductClickListener listener) {
        this.products = products == null ? Collections.emptyList() : products;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_related_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductEntity product = products.get(position);
        holder.name.setText(product.name);
        double displayPrice = product.salePrice != null && product.salePrice > 0
                ? product.salePrice : product.price;
        holder.price.setText(currency.format(displayPrice) + "đ");
        Glide.with(holder.itemView)
                .load(firstImage(product.images))
                .placeholder(R.drawable.product_case)
                .error(R.drawable.product_case)
                .into(holder.image);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onProductClick(product);
            }
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private String firstImage(String imagesJson) {
        if (imagesJson == null || imagesJson.trim().isEmpty()) {
            return null;
        }
        try {
            Type type = new TypeToken<List<String>>() {}.getType();
            List<String> images = new Gson().fromJson(imagesJson, type);
            return images == null || images.isEmpty() ? null : images.get(0);
        } catch (RuntimeException ignored) {
            return imagesJson.startsWith("http") ? imagesJson : null;
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView name;
        final TextView price;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.productImage);
            name = itemView.findViewById(R.id.productName);
            price = itemView.findViewById(R.id.productPrice);
        }
    }
}
