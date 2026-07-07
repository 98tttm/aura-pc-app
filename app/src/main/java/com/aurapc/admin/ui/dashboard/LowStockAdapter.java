package com.aurapc.admin.ui.dashboard;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Product;
import com.aurapc.admin.utils.Formatters;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class LowStockAdapter extends RecyclerView.Adapter<LowStockAdapter.VH> {

    private final List<Product> items = new ArrayList<>();

    public LowStockAdapter(List<Product> data) {
        if (data != null) items.addAll(data);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_low_stock, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Product p = items.get(position);
        h.name.setText(p.name != null ? p.name : "—");
        int stock = p.stock != null ? p.stock : 0;
        h.stock.setText("Còn " + stock);
        h.stock.setTextColor(stock == 0 ? 0xFFE53935 : 0xFFFFA726);
        h.price.setText(Formatters.formatVnd(p.salePrice != null ? p.salePrice : p.price));

        if (p.thumbnail != null && !p.thumbnail.isEmpty()) {
            Glide.with(h.image.getContext()).load(p.thumbnail).placeholder(R.drawable.ic_box).into(h.image);
        } else {
            h.image.setImageResource(R.drawable.ic_box);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, stock, price;

        VH(View v) {
            super(v);
            image = v.findViewById(R.id.ivImage);
            name = v.findViewById(R.id.tvName);
            stock = v.findViewById(R.id.tvStock);
            price = v.findViewById(R.id.tvPrice);
        }
    }
}
