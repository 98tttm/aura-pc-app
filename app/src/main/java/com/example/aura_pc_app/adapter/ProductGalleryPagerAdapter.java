package com.example.aura_pc_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

public class ProductGalleryPagerAdapter
        extends RecyclerView.Adapter<ProductGalleryPagerAdapter.GalleryViewHolder> {

    public interface OnImageClickListener {
        void onImageClick(String imageSource);
    }

    private final List<String> images = new ArrayList<>();
    private final OnImageClickListener listener;

    public ProductGalleryPagerAdapter(List<String> images, OnImageClickListener listener) {
        if (images != null) {
            this.images.addAll(images);
        }
        this.listener = listener;
    }

    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product_gallery_page, parent, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder holder, int position) {
        String imageSource = images.get(position);
        Glide.with(holder.itemView)
                .load(imageSource)
                .placeholder(R.drawable.product_case)
                .error(R.drawable.product_case)
                .into(holder.image);
        holder.image.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(imageSource);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class GalleryViewHolder extends RecyclerView.ViewHolder {
        final PhotoView image;

        GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.galleryImage);
        }
    }
}
