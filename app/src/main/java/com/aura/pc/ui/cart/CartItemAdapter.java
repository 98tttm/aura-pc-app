package com.aura.pc.ui.cart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {
    public interface Listener {
        void onIncrease(CartItemEntity item);
        void onDecrease(CartItemEntity item);
        void onRemove(CartItemEntity item);
    }

    private final List<CartItemEntity> items = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final Listener listener;

    public CartItemAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<CartItemEntity> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cart_product, parent, false);
        return new CartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartItemViewHolder holder, int position) {
        CartItemEntity item = items.get(position);
        int quantity = Math.max(1, item.quantity);
        holder.name.setText(firstNonEmpty(item.name, item.productId));
        holder.specs.setText(firstNonEmpty(item.specs, holder.itemView.getContext().getString(R.string.cart_item_specs_fallback)));
        holder.price.setText(formatPrice(item.unitPrice));
        holder.quantity.setText(String.valueOf(quantity));

        if (item.imageUrl != null && !item.imageUrl.trim().isEmpty()) {
            Glide.with(holder.itemView)
                    .load(firstImage(item.imageUrl))
                    .placeholder(R.drawable.figma_cart_product)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.figma_cart_product);
        }

        holder.plus.setOnClickListener(v -> listener.onIncrease(item));
        holder.minus.setOnClickListener(v -> listener.onDecrease(item));
        holder.remove.setOnClickListener(v -> listener.onRemove(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String formatPrice(double price) {
        if (price <= 0) {
            return "";
        }
        return currencyFormat.format(price) + "đ";
    }

    private String firstNonEmpty(String first, String second) {
        if (first != null && !first.trim().isEmpty()) return first;
        return second == null ? "" : second;
    }

    private String firstImage(String raw) {
        String value = raw.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            value = value.substring(1, value.length() - 1);
        }
        int comma = value.indexOf(',');
        if (comma >= 0) {
            value = value.substring(0, comma);
        }
        return value.replace("\"", "").trim();
    }

    static class CartItemViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView specs;
        final TextView price;
        final TextView quantity;
        final ImageView image;
        final View minus;
        final View plus;
        final View remove;

        CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            specs = itemView.findViewById(R.id.productSpecs);
            price = itemView.findViewById(R.id.productPrice);
            quantity = itemView.findViewById(R.id.quantityText);
            image = itemView.findViewById(R.id.productImage);
            minus = itemView.findViewById(R.id.btnMinus);
            plus = itemView.findViewById(R.id.btnPlus);
            remove = itemView.findViewById(R.id.cartItemCheckbox);
        }
    }
}
