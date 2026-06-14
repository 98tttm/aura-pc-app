package com.example.aura_pc_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.domain.model.Review;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {
    private final List<Review> reviews = new ArrayList<>();
    private final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);

    public void submitList(List<Review> newReviews) {
        reviews.clear();
        if (newReviews != null) {
            reviews.addAll(newReviews);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.userName.setText(review.getUserName());
        String userName = review.getUserName();
        holder.avatar.setText(userName == null || userName.trim().isEmpty()
                ? "A" : userName.trim().substring(0, 1).toUpperCase());
        holder.rating.setRating(review.getRating());
        holder.date.setText(dateFormat.format(new Date(review.getCreatedAt())));
        holder.content.setText(review.getContent());
        holder.verified.setVisibility(review.isDeliveredPurchase() ? View.VISIBLE : View.GONE);

        List<String> images = review.getImageUris();
        holder.images.setVisibility(images.isEmpty() ? View.GONE : View.VISIBLE);
        holder.images.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));
        holder.images.setAdapter(new ReviewImageAdapter(images));

        Review.AdminReply reply = review.getAdminReply();
        holder.adminReplyContainer.setVisibility(reply == null ? View.GONE : View.VISIBLE);
        if (reply != null) {
            holder.adminReplyContent.setText(reply.getContent());
            holder.adminReplyDate.setText(dateFormat.format(new Date(reply.getCreatedAt())));
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView userName;
        final TextView avatar;
        final RatingBar rating;
        final TextView date;
        final TextView verified;
        final TextView content;
        final RecyclerView images;
        final View adminReplyContainer;
        final TextView adminReplyContent;
        final TextView adminReplyDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.reviewUserName);
            avatar = itemView.findViewById(R.id.reviewUserAvatar);
            rating = itemView.findViewById(R.id.reviewRating);
            date = itemView.findViewById(R.id.reviewDate);
            verified = itemView.findViewById(R.id.reviewVerifiedBadge);
            content = itemView.findViewById(R.id.reviewContentText);
            images = itemView.findViewById(R.id.reviewImagesRecycler);
            adminReplyContainer = itemView.findViewById(R.id.adminReplyContainer);
            adminReplyContent = itemView.findViewById(R.id.adminReplyContent);
            adminReplyDate = itemView.findViewById(R.id.adminReplyDate);
        }
    }
}
