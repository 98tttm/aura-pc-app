package com.aurapc.admin.ui.products;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Product;
import com.aurapc.admin.utils.Formatters;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.VH> {

    public interface OnClick {
        void onClick(Product p);
    }

    private List<Product> products = new ArrayList<>();
    private OnClick onClick;

    public ProductsAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public ProductsAdapter() {
        this(null);
    }

    public void setProducts(List<Product> products) {
        this.products = products != null ? products : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Product p = products.get(pos);
        h.tvName.setText(p.name != null ? p.name : "");
        h.tvCategory.setText(extractCategory(p));
        h.tvBrand.setText(p.brand != null ? p.brand : "");
        h.tvPrice.setText(Formatters.formatVnd(p.price));
        h.tvSalePrice.setPaintFlags(h.tvSalePrice.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);

        double salePrice = p.salePrice != null ? p.salePrice : 0;
        if (salePrice > 0 && p.price != null && salePrice < p.price) {
            h.tvPrice.setText(Formatters.formatVnd(salePrice));
            h.tvSalePrice.setText(Formatters.formatVnd(p.price));
            h.tvSalePrice.setPaintFlags(h.tvSalePrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            h.tvSalePrice.setVisibility(View.VISIBLE);
        } else {
            h.tvSalePrice.setVisibility(View.GONE);
        }

        int stock = p.stock != null ? p.stock : 0;
        h.tvStock.setText(String.valueOf(stock));
        h.stockBar.setProgress(stock);
        h.stockBar.setProgressTintList(ColorStateList.valueOf(
                stock <= 0 ? Color.parseColor("#DC2626") :
                stock <= 10 ? Color.parseColor("#EAB308") : Color.parseColor("#16A34A")));

        boolean active = p.active();
        h.tvStatus.setText(active ? "Đang bán" : "Tạm ẩn");
        h.tvStatus.setBackgroundResource(active ? R.drawable.bg_badge_success : R.drawable.bg_badge_danger);
        h.tvStatus.setTextColor(active ? 0xFF16A34A : 0xFFDC2626);

        String imageUrl = p.primaryImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_box)
                    .into(h.ivImage);
        } else {
            h.ivImage.setImageResource(R.drawable.ic_box);
        }

        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(p);
        });
        h.btnEdit.setOnClickListener(v -> {
            if (onClick != null) onClick.onClick(p);
        });
    }

    private String extractCategory(Product p) {
        if (p.category instanceof Map) {
            Object name = ((Map) p.category).get("name");
            if (name != null) return name.toString();
        } else if (p.category != null) {
            return p.category.toString();
        }
        return "";
    }

    @Override public int getItemCount() { return products.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvName, tvCategory, tvBrand, tvPrice, tvSalePrice,
                tvStatus, tvStock;
        ProgressBar stockBar;
        ImageButton btnEdit;

        VH(View v) {
            super(v);
            ivImage = v.findViewById(R.id.ivImage);
            tvName = v.findViewById(R.id.tvName);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvBrand = v.findViewById(R.id.tvBrand);
            tvPrice = v.findViewById(R.id.tvPrice);
            tvSalePrice = v.findViewById(R.id.tvSalePrice);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvStock = v.findViewById(R.id.tvStock);
            stockBar = v.findViewById(R.id.stockBar);
            btnEdit = v.findViewById(R.id.btnEdit);
        }
    }
}
