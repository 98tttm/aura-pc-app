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
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.entity.WishlistEntity;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

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
    private final Set<String> favoriteIds = new HashSet<>();
    private OnProductClickListener productClickListener;
    private OnAddToCartListener addToCartListener;

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.productClickListener = listener;
    }

    public void setOnAddToCartListener(OnAddToCartListener listener) {
        this.addToCartListener = listener;
    }

    /**
     * Load danh sách productId yêu thích từ DB để hiển thị trạng thái tim.
     */
    public void loadFavoriteIds(android.content.Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<WishlistEntity> all = AppDatabase.getInstance(context).wishlistDao().getAllSync();
            favoriteIds.clear();
            for (WishlistEntity e : all) {
                favoriteIds.add(e.productId);
            }
        });
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
        private final ImageButton btnFavorite;
        private final TextView specTag1;
        private final TextView specTag2;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.productImage);
            productName = itemView.findViewById(R.id.productName);
            productPrice = itemView.findViewById(R.id.productPrice);
            productOldPrice = itemView.findViewById(R.id.productOldPrice);
            productRating = itemView.findViewById(R.id.productRating);
            discountBadge = itemView.findViewById(R.id.discountBadge);
            btnAddToCart = itemView.findViewById(R.id.btnAddToCart);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
            specTag1 = itemView.findViewById(R.id.specTag1);
            specTag2 = itemView.findViewById(R.id.specTag2);
        }

        void bind(Map<String, Object> product) {
            // Tên sản phẩm và bóc tách tags
            String name = "";
            Object nameObj = product.get("name");
            if (nameObj instanceof String) name = (String) nameObj;
            
            // Tìm ngoặc tròn để lấy cấu hình
            int openParen = name.indexOf("(");
            int closeParen = name.indexOf(")");
            if (openParen > 0 && closeParen > openParen) {
                String specsString = name.substring(openParen + 1, closeParen);
                String cleanName = name.substring(0, openParen).trim();
                productName.setText(cleanName);
                
                String[] parts = specsString.split("\\|");
                if (parts.length > 0 && specTag1 != null) {
                    // Cố gắng tìm phần chứa RTX, GTX, Core, Ryzen, RAM
                    String tag1 = "";
                    String tag2 = "";
                    for (String p : parts) {
                        p = p.trim();
                        if (p.contains("RTX") || p.contains("GTX") || p.contains("Core") || p.contains("Ryzen")) {
                            if (tag1.isEmpty()) tag1 = p;
                        } else if (p.contains("GB") || p.contains("RAM")) {
                            if (tag2.isEmpty()) tag2 = p;
                        }
                    }
                    if (tag1.isEmpty() && parts.length > 0) tag1 = parts[0].trim();
                    if (tag2.isEmpty() && parts.length > 1) tag2 = parts[1].trim();
                    
                    if (!tag1.isEmpty()) {
                        specTag1.setText(tag1);
                        specTag1.setVisibility(View.VISIBLE);
                    } else {
                        specTag1.setVisibility(View.GONE);
                    }
                    
                    if (!tag2.isEmpty()) {
                        specTag2.setText(tag2);
                        specTag2.setVisibility(View.VISIBLE);
                    } else {
                        specTag2.setVisibility(View.GONE);
                    }
                } else {
                    if (specTag1 != null) specTag1.setVisibility(View.GONE);
                    if (specTag2 != null) specTag2.setVisibility(View.GONE);
                }
            } else {
                productName.setText(name);
                if (specTag1 != null) specTag1.setVisibility(View.GONE);
                if (specTag2 != null) specTag2.setVisibility(View.GONE);
            }

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
                    productRating.setText(itemView.getContext().getString(R.string.label_default_rating));
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

            // Wishlist toggle
            if (btnFavorite != null) {
                String pid = "";
                Object idObj = product.get("_id");
                if (idObj instanceof String) pid = (String) idObj;

                boolean isFav = favoriteIds.contains(pid);
                btnFavorite.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                btnFavorite.setColorFilter(isFav
                        ? itemView.getContext().getColor(R.color.orange_primary)
                        : android.graphics.Color.parseColor("#AAAAAA"));

                final String finalPid = pid;
                btnFavorite.setOnClickListener(v -> {
                    boolean currentlyFav = favoriteIds.contains(finalPid);
                    android.content.Context ctx = itemView.getContext();
                    Executors.newSingleThreadExecutor().execute(() -> {
                        if (currentlyFav) {
                            AppDatabase.getInstance(ctx).wishlistDao().deleteByProductId(finalPid);
                            favoriteIds.remove(finalPid);
                        } else {
                            String n = "";
                            Object nObj = product.get("name");
                            if (nObj instanceof String) n = (String) nObj;
                            double p = getNumber(product, "price");
                            double op = getNumber(product, "old_price");
                            String img = "";
                            try {
                                List<String> imgs = (List<String>) product.get("images");
                                if (imgs != null && !imgs.isEmpty()) img = imgs.get(0);
                            } catch (Exception ignored) {}
                            AppDatabase.getInstance(ctx).wishlistDao().insert(
                                    new WishlistEntity(finalPid, n, p, op, img));
                            favoriteIds.add(finalPid);
                        }
                        ((android.app.Activity) ctx).runOnUiThread(() -> {
                            boolean nowFav = favoriteIds.contains(finalPid);
                            btnFavorite.setImageResource(nowFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                            btnFavorite.setColorFilter(nowFav
                                    ? ctx.getColor(R.color.orange_primary)
                                    : android.graphics.Color.parseColor("#AAAAAA"));
                            android.widget.Toast.makeText(ctx,
                                    nowFav ? ctx.getString(R.string.wishlist_added) : ctx.getString(R.string.wishlist_removed),
                                    android.widget.Toast.LENGTH_SHORT).show();
                        });
                    });
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
