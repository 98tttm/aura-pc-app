package com.aurapc.admin.ui.categories;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Category;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.VH> {

    private final List<Category> items = new ArrayList<>();

    public void setCategories(List<Category> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_category, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Category c = items.get(pos);
        h.name.setText(c.name != null ? c.name : "—");
        h.slug.setText(c.slug != null ? c.slug : "");
        if (c.image != null && !c.image.isEmpty()) {
            Glide.with(h.icon.getContext()).load(c.image).placeholder(R.drawable.ic_inventory).into(h.icon);
        } else {
            h.icon.setImageResource(R.drawable.ic_inventory);
        }
        int level = c.level != null ? c.level : 1;
        h.level.setText("Cấp " + level);
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, slug, level;

        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.ivIcon);
            name = v.findViewById(R.id.tvName);
            slug = v.findViewById(R.id.tvSlug);
            level = v.findViewById(R.id.tvLevel);
        }
    }
}