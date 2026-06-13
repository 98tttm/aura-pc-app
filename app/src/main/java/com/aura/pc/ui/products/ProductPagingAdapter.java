package com.aura.pc.ui.products;

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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Adapter hiển thị danh sách sản phẩm dạng Grid 2 cột.
 * Sử dụng RecyclerView.Adapter thông thường (Java-compatible).
 */
public class ProductPagingAdapter extends RecyclerView.Adapter<ProductPagingAdapter.ProductViewHolder> {

    public interface OnProductClickListener {
        void onProductClick(Map<String, Object> product);
    }

    public interface OnAddToCartListener {
        void onAddToCart(Map<String, Object> product);
    }

    private final List<Map<String, Object>> items = new ArrayList<>();
    private OnProductClickListener productClickListener;
    private OnAddToCartListener addToCartListener;

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.productClickListener = listener;
    }

    public void setOnAddToCartListener(OnAddToCartListener listener) {
        this.addToCartListener = listener;
    }

    /**
     * Thay thế toàn bộ danh sách (cho trang 1 hoặc khi đổi filter).
     */
    public void submitList(List<Map<String, Object>> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_grid_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Map<String, Object> product = items.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView productImage;
        private final TextView productName;
        private final TextView productPrice;
        private final TextView productOldPrice;
        private final TextView productRating;
        private final TextView discountBadge;
        private final ImageButton btnAddToCart;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productOldPrice = itemView.findViewById(R.id.productOldPrice);
            productRating = itemView.findViewById(R.id.productRating);
            discountBadge = itemView.findViewById(R.id.discountBadge);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
        }

        void bind(Map<String, Object> product) {
            // Tên sản phẩm
            String name = "";
            Object nameObj = product.get("name");
            if (nameObj instanceof String) name = (String) nameObj;
            productName.setText(name);

            // Giá hiện tại
            double price = getNumber(product, "price");
            productPrice.setText(formatCurrency(price));

            // Giá cũ
            double oldPrice = getNumber(product, "old_price");
            if (oldPrice > 0 && oldPrice > price) {
                productOldPrice.setVisibility(View.VISIBLE);
                productOldPrice.setText(formatCurrency(oldPrice));
                productOldPrice.setPaintFlags(
                        productOldPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

                if (discountBadge != null) {
                    int discountPct = (int) Math.round((1.0 - (price / oldPrice)) * 100);
                    if (discountPct > 0) {
                        discountBadge.setVisibility(View.VISIBLE);
                        discountBadge.setText("-" + discountPct + "%");
                    } else {
                        discountBadge.setVisibility(View.GONE);
                    }
                }
            } else {
                productOldPrice.setVisibility(View.GONE);
                if (discountBadge != null) discountBadge.setVisibility(View.GONE);
            }

            // Rating
            if (productRating != null) {
                double rating = getNumber(product, "rating");
                if (rating > 0) {
                    productRating.setText(String.format("%.1f", rating));
                } else {
                    productRating.setText("4.9");
                }
                productRating.setVisibility(View.VISIBLE);
            }

            // Ảnh sản phẩm
            List<String> images = null;
            try {
                images = (List<String>) product.get("images");
            } catch (Exception ignored) {}

            if (images != null && !images.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(images.get(0))
                        .placeholder(R.drawable.aura_laptop)
                        .into(productImage);
            } else {
                productImage.setImageResource(R.drawable.aura_laptop);
            }

            // Click toàn bộ card
            itemView.setOnClickListener(v -> {
                if (productClickListener != null) {
                    productClickListener.onProductClick(product);
                }
            });

            // Click nút giỏ hàng
            if (btnAddToCart != null) {
                btnAddToCart.setOnClickListener(v -> {
                    if (addToCartListener != null) {
                        addToCartListener.onAddToCart(product);
                    }
                });
            }
        }

        private double getNumber(Map<String, Object> map, String key) {
            Object val = map.get(key);
            if (val instanceof Number) return ((Number) val).doubleValue();
            return 0;
        }

        private String formatCurrency(double amount) {
            DecimalFormat formatter = new DecimalFormat("#,###");
            return formatter.format(amount).replace(",", ".") + "đ";
        }
    }
}
