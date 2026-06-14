package com.example.aura_pc_app.adapter;

import android.content.Context;
import android.graphics.Paint;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HomeSaleProductAdapter extends RecyclerView.Adapter<HomeSaleProductAdapter.SaleViewHolder> {
    public interface ProductClickListener {
        void onProductClick(ProductEntity product);
    }

    private final List<ProductEntity> products = new ArrayList<>();
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final ProductClickListener listener;
    private int campaignIndex;
    private int dateIndex;

    public HomeSaleProductAdapter(ProductClickListener listener) {
        this.listener = listener;
    }

    public void setProducts(List<ProductEntity> newProducts, int campaignIndex, int dateIndex) {
        products.clear();
        if (newProducts != null) {
            products.addAll(newProducts);
        }
        this.campaignIndex = campaignIndex;
        this.dateIndex = dateIndex;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_sale_card, parent, false);
        return new SaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SaleViewHolder holder, int position) {
        ProductEntity product = products.get(position);
        double currentPrice = currentPrice(product);
        double oldPrice = oldPrice(product, currentPrice);

        holder.title.setText(product.name == null || product.name.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.home_product_case_name)
                : product.name);
        holder.price.setText(formatPrice(holder.itemView.getContext(), currentPrice));
        holder.oldPrice.setText(formatPrice(holder.itemView.getContext(), oldPrice));
        holder.oldPrice.setPaintFlags(holder.oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.discountBadge.setText(discountLabel(oldPrice, currentPrice));
        bindImage(holder, product);
        bindStock(holder, position);
        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private void bindImage(SaleViewHolder holder, ProductEntity product) {
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

    private void bindStock(SaleViewHolder holder, int position) {
        int sold = 3 + ((position * 5 + campaignIndex * 4 + dateIndex * 7) % 17);
        holder.stockText.setText(holder.itemView.getContext().getString(R.string.home_flash_stock_dynamic, sold));
        holder.stockFill.post(() -> {
            ViewGroup.LayoutParams params = holder.stockFill.getLayoutParams();
            int trackWidth = ((View) holder.stockFill.getParent()).getWidth();
            params.width = Math.max(1, Math.round(trackWidth * sold / 20f));
            holder.stockFill.setLayoutParams(params);
        });
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
        return currentPrice;
    }

    private String discountLabel(double oldPrice, double currentPrice) {
        int percent = Math.max(1, (int) Math.round((oldPrice - currentPrice) * 100d / oldPrice));
        return "-" + percent + "%";
    }

    static class SaleViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView discountBadge;
        final TextView title;
        final TextView price;
        final TextView oldPrice;
        final View stockFill;
        final TextView stockText;

        SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.saleProductImage);
            discountBadge = itemView.findViewById(R.id.saleDiscountBadge);
            title = itemView.findViewById(R.id.saleProductTitle);
            price = itemView.findViewById(R.id.saleProductPrice);
            oldPrice = itemView.findViewById(R.id.saleProductOldPrice);
            stockFill = itemView.findViewById(R.id.saleStockFill);
            stockText = itemView.findViewById(R.id.saleStockText);
        }
    }
}
