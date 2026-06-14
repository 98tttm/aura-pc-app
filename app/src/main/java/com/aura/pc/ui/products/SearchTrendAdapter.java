package com.aura.pc.ui.products;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;

import java.util.ArrayList;
import java.util.List;

class SearchTrendAdapter extends RecyclerView.Adapter<SearchTrendAdapter.TrendViewHolder> {
    interface TrendClickListener {
        void onTrendClick(TrendItem item);
    }

    static class TrendItem {
        final String title;
        final int imageRes;

        TrendItem(String title, int imageRes) {
            this.title = title;
            this.imageRes = imageRes;
        }
    }

    private final List<TrendItem> items = new ArrayList<>();
    private final TrendClickListener listener;

    SearchTrendAdapter(TrendClickListener listener) {
        this.listener = listener;
    }

    void submitList(List<TrendItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_trend_category, parent, false);
        return new TrendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrendViewHolder holder, int position) {
        TrendItem item = items.get(position);
        holder.title.setText(item.title);
        holder.image.setImageResource(item.imageRes);
        holder.itemView.setOnClickListener(v -> listener.onTrendClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class TrendViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;
        final TextView title;

        TrendViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.trendCategoryImage);
            title = itemView.findViewById(R.id.trendCategoryTitle);
        }
    }
}
