package com.example.aura_pc_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.example.aura_pc_app.R;

import java.util.List;

public class ProductImageAdapter extends RecyclerView.Adapter<ProductImageAdapter.ViewHolder> {
    private List<Integer> images;
    private int selectedPosition = 0;
    private OnImageClickListener listener;

    public interface OnImageClickListener {
        void onImageClick(int imageRes);
    }

    public ProductImageAdapter(List<Integer> images) {
        this.images = images;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.imageView.setImageResource(images.get(position));
        
        if (selectedPosition == position) {
            holder.cardView.setStrokeWidth(4); // Thick border
            holder.cardView.setStrokeColor(holder.itemView.getContext().getColor(R.color.border_selected));
        } else {
            holder.cardView.setStrokeWidth(2); // Thin border
            holder.cardView.setStrokeColor(holder.itemView.getContext().getColor(R.color.border_light));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            
            if (listener != null) {
                listener.onImageClick(images.get(selectedPosition));
            }
        });
    }

    @Override
    public int getItemCount() {
        return images != null ? images.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        MaterialCardView cardView;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.thumbnailImage);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
