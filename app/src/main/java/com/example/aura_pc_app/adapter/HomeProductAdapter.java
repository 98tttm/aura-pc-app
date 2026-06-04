package com.example.aura_pc_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
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
    private final int[] productImages = {
            R.drawable.figma_product_monitor,
            R.drawable.figma_product_mouse,
            R.drawable.figma_blog_laptop,
            R.drawable.figma_blog_rtx
    };

    public HomeProductAdapter(ProductClickListener listener) {
        this.listener = listener;
        currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    }

    public void setProducts(List<ProductEntity> newProducts) {
        products.clear();
        if (newProducts != null) {
            products.addAll(newProducts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_aura_product_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity product = products.get(position);
        holder.title.setText(product.name == null || product.name.isEmpty()
                ? holder.itemView.getContext().getString(R.string.product_title_acer_nitro_v16s)
                : product.name);
        holder.price.setText(formatPrice(product.salePrice != null ? product.salePrice : product.price));
        holder.image.setImageResource(productImages[position % productImages.length]);
        holder.rating.setText(position % 2 == 0
                ? R.string.home_product_rating_one
                : R.string.home_product_rating_two);
        if (product.salePrice != null && product.price > product.salePrice) {
            holder.oldPrice.setVisibility(View.GONE);
            holder.oldPrice.setText(formatPrice(product.price));
        } else {
            holder.oldPrice.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
        holder.cartButton.setOnClickListener(v -> listener.onCartClick(product));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private String formatPrice(double price) {
        if (price <= 0) {
            return "Liên hệ";
        }
        return currencyFormat.format(price) + "đ";
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView oldPrice;
        final TextView price;
        final TextView rating;
        final ImageView image;
        final ImageButton cartButton;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.productTitle);
            oldPrice = itemView.findViewById(R.id.oldPriceText);
            price = itemView.findViewById(R.id.productPriceText);
            rating = itemView.findViewById(R.id.productRatingText);
            image = itemView.findViewById(R.id.productImage);
            cartButton = itemView.findViewById(R.id.productBuyButton);
        }
    }
}
