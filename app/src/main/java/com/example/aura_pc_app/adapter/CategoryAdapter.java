package com.example.aura_pc_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.domain.model.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    private final List<Category> categories;
    private OnCategoryClickListener listener;

    // Icon và màu gradient cho từng danh mục cấp 1 (theo category_id)
    private static final int[][] CATEGORY_CONFIGS = {
        // {iconRes, colorStartRes, colorEndRes}
    };

    public CategoryAdapter(List<Category> categories) {
        this.categories = categories;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category cat = categories.get(position);

        holder.categoryName.setText(cat.getName());

        // Chọn icon phù hợp theo category_id
        holder.categoryIcon.setImageResource(getCategoryIcon(cat.getCategoryId()));

        // Màu nền icon thay đổi theo vị trí để tạo cảm giác đa dạng
        int[] bgColors = {
            R.color.orange_light,
            R.color.aura_soft_orange,
            R.color.gray_light,
            R.color.aura_soft_orange_strong,
            R.color.orange_light,
            R.color.aura_soft_orange,
            R.color.gray_light,
        };
        int bgColor = bgColors[position % bgColors.length];
        holder.categoryIcon.setBackgroundResource(bgColor);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onCategoryClick(cat);
        });
    }

    private int getCategoryIcon(String categoryId) {
        if (categoryId == null) return R.drawable.ic_grid;
        switch (categoryId) {
            case "laptop":          return R.drawable.ic_cpu;
            case "pc":              return R.drawable.ic_gpu;
            case "man-hinh":        return R.drawable.ic_categories;
            case "linh-kien":       return R.drawable.ic_ssd;
            case "gaming-gear":     return R.drawable.ic_grid;
            case "phu-kien":        return R.drawable.ic_bag;
            case "ban-ghe":         return R.drawable.ic_profile;
            default:                return R.drawable.ic_grid;
        }
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView categoryIcon;
        TextView categoryName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryIcon = itemView.findViewById(R.id.categoryIcon);
            categoryName = itemView.findViewById(R.id.categoryName);
        }
    }
}
