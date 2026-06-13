package com.aura.pc.ui.products;

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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

class SearchSuggestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    interface SuggestionClickListener {
        void onSuggestionClick(String keyword);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PRODUCT = 1;

    private final List<ProductEntity> products = new ArrayList<>();
    private final SuggestionClickListener listener;
    private final Gson gson = new Gson();
    private final Type imageListType = new TypeToken<List<String>>() {}.getType();
    private String query = "";

    SearchSuggestionAdapter(SuggestionClickListener listener) {
        this.listener = listener;
    }

    void submitList(List<ProductEntity> newProducts, String query) {
        products.clear();
        if (newProducts != null) products.addAll(newProducts);
        this.query = query == null ? "" : query.trim();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? TYPE_HEADER : TYPE_PRODUCT;
    }

    @Override
    public int getItemCount() {
        return query.isEmpty() ? 0 : 1 + products.size();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_search_suggestion_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_search_suggestion, parent, false);
            return new ProductViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (position == 0) {
            HeaderViewHolder h = (HeaderViewHolder) holder;
            h.text.setText("Xem tất cả kết quả cho \"" + query + "\"");
            h.itemView.setOnClickListener(v -> listener.onSuggestionClick(query));
        } else {
            ProductEntity product = products.get(position - 1);
            ProductViewHolder p = (ProductViewHolder) holder;

            p.nameText.setText(product.name != null ? product.name : "");

            boolean hasSale = product.salePrice != null && product.salePrice > 0;
            double displayPrice = hasSale ? product.salePrice : product.price;
            if (displayPrice > 0) {
                p.priceText.setText(formatPrice(displayPrice));
            } else {
                p.priceText.setText("Liên hệ");
            }

            String imageUrl = parseFirstImage(product.images);
            Glide.with(p.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_categories)
                    .error(R.drawable.ic_categories)
                    .centerCrop()
                    .into(p.imageView);

            p.itemView.setOnClickListener(v -> {
                String name = product.name != null ? product.name.trim() : "";
                listener.onSuggestionClick(name.isEmpty() ? query : name);
            });
        }
    }

    private String formatPrice(double price) {
        return String.format(Locale.US, "%,d", Math.round(price)).replace(',', '.') + "đ";
    }

    private String parseFirstImage(String imagesJson) {
        if (imagesJson == null || imagesJson.trim().isEmpty()) return null;
        try {
            List<String> list = gson.fromJson(imagesJson, imageListType);
            if (list != null && !list.isEmpty()) return list.get(0);
        } catch (Exception ignored) {}
        return null;
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView text;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.headerText);
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView nameText;
        final TextView priceText;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.suggestionImage);
            nameText = itemView.findViewById(R.id.suggestionText);
            priceText = itemView.findViewById(R.id.suggestionPrice);
        }
    }
}
