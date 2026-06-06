package com.aura.pc.ui.cart;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import java.util.ArrayList;
import java.util.List;

public class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.CartItemViewHolder> {
    private final List<CartItemEntity> items = new ArrayList<>();

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
        holder.name.setText(item.productId == null || item.productId.isEmpty()
                ? holder.itemView.getContext().getString(R.string.cart_dummy_product_name)
                : item.productId);
        holder.specs.setText(holder.itemView.getContext().getString(R.string.cart_dummy_product_specs));
        holder.price.setText(holder.itemView.getContext().getString(R.string.cart_dummy_price));
        holder.quantity.setText(String.valueOf(quantity));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CartItemViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView specs;
        final TextView price;
        final TextView quantity;

        CartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.productName);
            specs = itemView.findViewById(R.id.productSpecs);
            price = itemView.findViewById(R.id.productPrice);
            quantity = itemView.findViewById(R.id.quantityText);
        }
    }
}
