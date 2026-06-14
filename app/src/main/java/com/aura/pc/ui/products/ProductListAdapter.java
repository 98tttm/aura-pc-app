package com.aura.pc.ui.products;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.entity.ProductEntity;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class ProductListAdapter extends RecyclerView.Adapter<ProductListAdapter.ProductViewHolder> {
    interface ProductActionListener {
        void onProductClick(ProductEntity product);
        void onCartClick(ProductEntity product);
    }

    private final List<ProductEntity> products = new ArrayList<>();
    private final ProductActionListener listener;
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private final int[] productImages = {
            R.drawable.figma_product_laptop_list,
            R.drawable.figma_product_monitor,
            R.drawable.figma_product_mouse,
            R.drawable.figma_blog_laptop,
            R.drawable.figma_blog_rtx,
            R.drawable.figma_cat_gaming_pc
    };
    private String highlightQuery = "";

    ProductListAdapter(ProductActionListener listener) {
        this.listener = listener;
    }

    void submitList(List<ProductEntity> newProducts, String query) {
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
                .inflate(R.layout.item_product_list_card, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity product = products.get(position);
        String productName = safeText(product.name, "Laptop Gaming Acer Nitro V 16S");
        String[] specs = buildSpecs(product);

        holder.image.setImageResource(productImages[position % productImages.length]);
        holder.image.setContentDescription(productName);
        holder.rating.setText(position % 2 == 0 ? "4.8" : "4.9");
        holder.review.setText(position % 2 == 0 ? "(124)" : "(89)");
        holder.title.setText(highlight(productName, highlightQuery, holder));
        holder.specPrimary.setText(highlight(specs[0], highlightQuery, holder));
        holder.specSecondary.setText(highlight(specs[1], highlightQuery, holder));
        holder.price.setText(formatPrice(product.salePrice != null ? product.salePrice : product.price));

        if (product.salePrice != null && product.price > product.salePrice) {
            holder.oldPrice.setVisibility(View.VISIBLE);
            holder.oldPrice.setPaintFlags(holder.oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.oldPrice.setText(formatPrice(product.price));
        } else {
            holder.oldPrice.setVisibility(View.INVISIBLE);
            holder.oldPrice.setText("");
        }

        holder.itemView.setOnClickListener(v -> listener.onProductClick(product));
        holder.cartButton.setOnClickListener(v -> listener.onCartClick(product));
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private CharSequence highlight(String text, String query, ProductViewHolder holder) {
        if (text == null || text.isEmpty() || query == null || query.trim().isEmpty()) {
            return text == null ? "" : text;
        }

        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerQuery = query.trim().toLowerCase(Locale.ROOT);
        SpannableString spannable = new SpannableString(text);
        int foreground = ContextCompat.getColor(holder.itemView.getContext(), R.color.aura_orange);
        int background = ContextCompat.getColor(holder.itemView.getContext(), R.color.aura_soft_orange_strong);

        int start = lowerText.indexOf(lowerQuery);
        while (start >= 0) {
            int end = start + lowerQuery.length();
            spannable.setSpan(new ForegroundColorSpan(foreground), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new BackgroundColorSpan(background), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = lowerText.indexOf(lowerQuery, end);
        }
        return spannable;
    }

    private String[] buildSpecs(ProductEntity product) {
        List<String> specs = new ArrayList<>();
        addIfPresent(specs, product.brand);

        String cleanedSpecs = cleanSpecs(product.specs);
        if (!cleanedSpecs.isEmpty()) {
            String[] pieces = cleanedSpecs.split(",");
            for (String piece : pieces) {
                addIfPresent(specs, piece);
                if (specs.size() >= 2) {
                    break;
                }
            }
        }

        addIfPresent(specs, product.slug);
        while (specs.size() < 2) {
            specs.add(specs.isEmpty() ? "RTX 4060" : "16GB RAM");
        }
        return new String[]{shorten(specs.get(0)), shorten(specs.get(1))};
    }

    private void addIfPresent(List<String> values, String value) {
        String safe = value == null ? "" : value.trim();
        if (!safe.isEmpty() && !values.contains(safe)) {
            values.add(safe);
        }
    }

    private String cleanSpecs(String rawSpecs) {
        if (rawSpecs == null) {
            return "";
        }
        return rawSpecs
                .replace("{", "")
                .replace("}", "")
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace(":", " ")
                .replace(";", ",")
                .replace("|", ",")
                .trim();
    }

    private String shorten(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() > 18 ? trimmed.substring(0, 18).trim() : trimmed;
    }

    private String safeText(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String formatPrice(double price) {
        if (price <= 0) {
            return "Lien he";
        }
        return currencyFormat.format(price) + "\u0111";
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView rating;
        final TextView review;
        final TextView title;
        final TextView specPrimary;
        final TextView specSecondary;
        final TextView oldPrice;
        final TextView price;
        final ImageButton cartButton;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.productImage);
            rating = itemView.findViewById(R.id.productRatingText);
            review = itemView.findViewById(R.id.productReviewText);
            title = itemView.findViewById(R.id.productTitle);
            specPrimary = itemView.findViewById(R.id.productSpecPrimary);
            specSecondary = itemView.findViewById(R.id.productSpecSecondary);
            oldPrice = itemView.findViewById(R.id.oldPriceText);
            price = itemView.findViewById(R.id.productPriceText);
            cartButton = itemView.findViewById(R.id.productBuyButton);
        }
    }
}
