package com.aura.pc.ui.wishlist;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.entity.WishlistEntity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.ViewHolder> {

    public interface OnRemoveListener {
        void onRemove(WishlistEntity item, int position);
    }

    public interface OnAddToCartListener {
        void onAddToCart(WishlistEntity item);
    }

    public interface OnItemClickListener {
        void onItemClick(WishlistEntity item);
    }

    private final List<WishlistEntity> items = new ArrayList<>();
    private OnRemoveListener removeListener;
    private OnAddToCartListener addToCartListener;
    private OnItemClickListener itemClickListener;

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.removeListener = listener;
    }

    public void setOnAddToCartListener(OnAddToCartListener listener) {
        this.addToCartListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void submitList(List<WishlistEntity> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void removeAt(int position) {
        if (position >= 0 && position < items.size()) {
            items.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, items.size());
        }
    }

    public int getItemCount() {
        return items.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_wishlist, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WishlistEntity item = items.get(position);
        holder.bind(item, position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivProductImage;
        private final TextView tvProductName;
        private final TextView tvProductPrice;
        private final TextView btnAddToCart;
        private final ImageButton btnRemove;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProductImage = itemView.findViewById(R.id.ivProductImage);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvProductPrice = itemView.findViewById(R.id.tvProductPrice);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }

        void bind(WishlistEntity item, int position) {
            tvProductName.setText(item.name);
            tvProductPrice.setText(formatCurrency(item.price));

            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.imageUrl)
                        .placeholder(R.drawable.aura_laptop)
                        .into(ivProductImage);
            } else {
                ivProductImage.setImageResource(R.drawable.aura_laptop);
            }

            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) itemClickListener.onItemClick(item);
            });

            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onRemove(item, position);
            });

            btnAddToCart.setOnClickListener(v -> {
                if (addToCartListener != null) addToCartListener.onAddToCart(item);
            });
        }

        private String formatCurrency(double amount) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            return formatter.format(amount).replace(",", ".") + "đ";
        }
    }
}
