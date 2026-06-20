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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {
    public interface Listener {
        void onIncrease(CartItemEntity item);
        void onDecrease(CartItemEntity item);
        void onRemove(CartItemEntity item);
        void onSelectionChanged();
    }

    private final List<CartItemEntity> items = new ArrayList<>();
    private final Set<String> selectedKeys = new HashSet<>();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final Listener listener;

    public CartItemAdapter(Listener listener) {
        this.listener = listener;
    }

    public void setItems(List<CartItemEntity> newItems) {
        Set<String> incomingKeys = new HashSet<>();
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
            for (CartItemEntity item : newItems) {
                String key = cartKey(item);
                incomingKeys.add(key);
                if (!selectedKeys.contains(key)) {
                    selectedKeys.add(key);
                }
            }
        }
        selectedKeys.retainAll(incomingKeys);
        notifyDataSetChanged();
    }

    public void setAllSelected(boolean selected) {
        selectedKeys.clear();
        if (selected) {
            for (CartItemEntity item : items) {
                selectedKeys.add(cartKey(item));
            }
        }
        notifyDataSetChanged();
        listener.onSelectionChanged();
    }

    public List<CartItemEntity> getSelectedItems() {
        List<CartItemEntity> selected = new ArrayList<>();
        for (CartItemEntity item : items) {
            if (selectedKeys.contains(cartKey(item))) {
                selected.add(item);
            }
        }
        return selected;
    }

    public ArrayList<String> getSelectedKeys() {
        return new ArrayList<>(selectedKeys);
    }

    public ArrayList<String> getSelectedProductIds() {
        ArrayList<String> productIds = new ArrayList<>();
        for (CartItemEntity item : items) {
            if (selectedKeys.contains(cartKey(item)) && item.productId != null && !item.productId.trim().isEmpty()) {
                productIds.add(item.productId.trim());
            }
        }
        return productIds;
    }

    public boolean areAllItemsSelected() {
        return !items.isEmpty() && selectedKeys.size() == items.size();
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
        boolean selected = selectedKeys.contains(cartKey(item));
        holder.checkbox.setImageResource(selected
                ? R.drawable.ic_cart_checkbox_selected
                : R.drawable.ic_figma_checkbox);

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
        holder.checkbox.setOnClickListener(v -> {
            String key = cartKey(item);
            if (selectedKeys.contains(key)) {
                selectedKeys.remove(key);
            } else {
                selectedKeys.add(key);
            }
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(adapterPosition);
            } else {
                notifyDataSetChanged();
            }
            listener.onSelectionChanged();
        });
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

    public static String cartKey(CartItemEntity item) {
        String productId = item == null || item.productId == null ? "" : item.productId.trim();
        String variantId = item == null || item.variantId == null ? "" : item.variantId.trim();
        return productId + "\n" + variantId;
    }

    static class CartItemViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView specs;
        final TextView price;
        final TextView quantity;
        final ImageView image;
        final View minus;
        final View plus;
        final ImageView checkbox;

        CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            specs = itemView.findViewById(R.id.productSpecs);
            price = itemView.findViewById(R.id.productPrice);
            quantity = itemView.findViewById(R.id.quantityText);
            image = itemView.findViewById(R.id.productImage);
            minus = itemView.findViewById(R.id.btnMinus);
            plus = itemView.findViewById(R.id.btnPlus);
            checkbox = itemView.findViewById(R.id.cartItemCheckbox);
        }
    }
}
