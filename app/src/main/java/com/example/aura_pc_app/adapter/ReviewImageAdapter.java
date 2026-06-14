package com.example.aura_pc_app.adapter;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.ArrayList;
import java.util.List;

public class ReviewImageAdapter extends RecyclerView.Adapter<ReviewImageAdapter.ViewHolder> {
    private final List<String> images = new ArrayList<>();

    public ReviewImageAdapter(List<String> images) {
        setImages(images);
    }

    public void setImages(List<String> newImages) {
        images.clear();
        if (newImages != null) {
            images.addAll(newImages);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String imageSource = images.get(position);
        Glide.with(holder.itemView)
                .load(imageSource)
                .placeholder(R.drawable.product_case)
                .error(R.drawable.product_case)
                .into(holder.image);
        holder.image.setOnClickListener(v -> showImagePreview(holder.itemView, imageSource));
    }

    private void showImagePreview(View sourceView, String imageSource) {
        Dialog dialog = new Dialog(
                sourceView.getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        PhotoView photoView = new PhotoView(sourceView.getContext());
        photoView.setBackgroundColor(Color.BLACK);
        photoView.setContentDescription(
                sourceView.getContext().getString(R.string.review_image_description));
        Glide.with(sourceView)
                .load(imageSource)
                .placeholder(R.drawable.product_case)
                .error(R.drawable.product_case)
                .into(photoView);
        photoView.setOnClickListener(v -> dialog.dismiss());
        dialog.setContentView(photoView);
        dialog.setCanceledOnTouchOutside(true);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.BLACK));
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView image;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.reviewImage);
        }
    }
}
