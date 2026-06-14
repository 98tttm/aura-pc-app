package com.example.aura_pc_app.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.entity.ProductEntity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeProductAdapter extends RecyclerView.Adapter<HomeProductAdapter.ProductViewHolder> {
    public interface ProductClickListener {
        void onProductClick(ProductEntity product);
        void onCartClick(ProductEntity product);
    }

    private final List<ProductEntity> products = new ArrayList<>();
    private final ProductClickListener listener;
    private final NumberFormat currencyFormat;

    public HomeProductAdapter(ProductClickListener listener) {
        this.listener = listener;
        currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    public void setProducts(List<ProductEntity> newProducts) {
        setProducts(newProducts, "");
    }

    public void setProducts(List<ProductEntity> newProducts, String query) {
        products.clear();
        if (newProducts != null) {
            products.addAll(newProducts);
        }
        highlightQuery = query == null ? "" : query.trim();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_product_grid_card, parent, false);
        int gap = Math.round(16 * parent.getResources().getDisplayMetrics().density);
        int parentWidth = parent.getMeasuredWidth();
        if (parentWidth <= 0) {
            parentWidth = parent.getResources().getDisplayMetrics().widthPixels
                    - Math.round(44 * parent.getResources().getDisplayMetrics().density);
        }
        int parentHeight = parent.getMeasuredHeight();
        if (parentHeight <= 0) {
            parentHeight = Math.round(468 * parent.getResources().getDisplayMetrics().density);
        }

        ViewGroup.LayoutParams currentParams = view.getLayoutParams();
        RecyclerView.LayoutParams params;
        if (currentParams instanceof RecyclerView.LayoutParams) {
            params = (RecyclerView.LayoutParams) currentParams;
        } else {
            params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        params.width = Math.max(1, (parentWidth - gap) / 2);
        params.height = Math.max(1, (parentHeight - gap) / 2);
        view.setLayoutParams(params);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity product = products.get(position);
        String productName = product.name == null || product.name.isEmpty()
                ? holder.itemView.getContext().getString(R.string.product_title_acer_nitro_v16s)
                : product.name);

        double currentPrice = currentPrice(product);
        double oldPriceValue = oldPrice(product, currentPrice);
        holder.price.setText(formatPrice(holder.itemView.getContext(), currentPrice));
        bindImage(holder, product);
        holder.rating.setText(R.string.home_product_rating_short);
        holder.oldPrice.setVisibility(View.GONE);
        holder.oldPrice.setText("");
        holder.oldPrice.setPaintFlags(holder.oldPrice.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        holder.saleBadge.setVisibility(View.GONE);
        holder.saleBadge.setText("");

        if (oldPriceValue > 0 && oldPriceValue > currentPrice) {
            holder.oldPrice.setVisibility(View.VISIBLE);
            holder.oldPrice.setText(formatPrice(holder.itemView.getContext(), oldPriceValue));
            holder.oldPrice.setPaintFlags(holder.oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.saleBadge.setVisibility(View.VISIBLE);
            holder.saleBadge.setText(discountLabel(oldPriceValue, currentPrice));
            holder.saleBadge.bringToFront();
        } else {
            holder.oldPrice.setVisibility(View.GONE);
            holder.saleBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
        holder.cartButton.setOnClickListener(v -> listener.onCartClick(product));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private void bindImage(ProductViewHolder holder, ProductEntity product) {
        if (product.imageUrl != null && !product.imageUrl.trim().isEmpty()) {
            Glide.with(holder.image)
                    .load(product.imageUrl)
                    .placeholder(R.drawable.figma_sale_case)
                    .error(R.drawable.figma_sale_case)
                    .into(holder.image);
        } else {
            holder.image.setImageResource(R.drawable.figma_sale_case);
        }
    }

    private String formatPrice(Context context, double price) {
        if (price <= 0) {
            return context.getString(R.string.product_contact_price);
        }
        return currencyFormat.format(price) + "\u0111";
    }

    private double currentPrice(ProductEntity product) {
        if (product.salePrice != null && product.salePrice > 0) {
            return product.salePrice;
        }
        return product.price;
    }

    private double oldPrice(ProductEntity product, double currentPrice) {
        if (product.oldPrice != null && product.oldPrice > currentPrice) {
            return product.oldPrice;
        }
        if (product.salePrice != null && product.salePrice > 0 && product.price > product.salePrice) {
            return product.price;
        }
        return 0;
    }

    private String discountLabel(double oldPrice, double currentPrice) {
        int percent = Math.max(1, (int) Math.round((oldPrice - currentPrice) * 100d / oldPrice));
        return "SALE -" + percent + "%";
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView oldPrice;
        final TextView price;
        final TextView rating;
        final TextView saleBadge;
        final ImageView image;
        final ImageButton cartButton;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.productTitle);
            oldPrice = itemView.findViewById(R.id.oldPriceText);
            price = itemView.findViewById(R.id.productPriceText);
            rating = itemView.findViewById(R.id.productRatingText);
            saleBadge = itemView.findViewById(R.id.productSaleBadge);
            image = itemView.findViewById(R.id.productImage);
            cartButton = itemView.findViewById(R.id.productBuyButton);
        }
    }
}
