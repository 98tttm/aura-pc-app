package com.aurapc.admin.ui.reviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.ProductReview;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;

public class ReviewsAdapter extends RecyclerView.Adapter<ReviewsAdapter.VH> {

    public interface OnHide { void onHide(ProductReview r); }
    public interface OnRestore { void onRestore(ProductReview r); }

    private final List<ProductReview> items = new ArrayList<>();
    private final OnHide onHide;
    private final OnRestore onRestore;

    public ReviewsAdapter(OnHide onHide, OnRestore onRestore) {
        this.onHide = onHide;
        this.onRestore = onRestore;
    }

    public void setReviews(List<ProductReview> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        ProductReview r = items.get(pos);
        h.author.setText(r.userName != null ? r.userName : (r.author != null ? r.author : "Khách hàng"));
        h.product.setText(r.productName != null ? r.productName : "Sản phẩm");
        h.content.setText(r.content != null ? r.content : "");
        h.rating.setRating(r.rating != null ? r.rating : 0);
        h.date.setText(Formatters.formatDate(r.createdAt));

        boolean hidden = r.hidden != null && r.hidden;
        h.status.setText(hidden ? "Đã ẩn" : "Bị báo cáo");
        h.status.setBackgroundResource(hidden ? R.drawable.bg_badge_neutral : R.drawable.bg_badge_warning);
        h.status.setTextColor(hidden ? 0xFFFFFFFF : 0xFFEAB308);

        h.btnHide.setVisibility(hidden ? View.GONE : View.VISIBLE);
        h.btnRestore.setVisibility(hidden ? View.VISIBLE : View.GONE);

        h.btnHide.setOnClickListener(v -> onHide.onHide(r));
        h.btnRestore.setOnClickListener(v -> onRestore.onRestore(r));
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView author, product, content, date, status, btnHide, btnRestore;
        RatingBar rating;

        VH(View v) {
            super(v);
            author = v.findViewById(R.id.tvAuthor);
            product = v.findViewById(R.id.tvProduct);
            content = v.findViewById(R.id.tvContent);
            rating = v.findViewById(R.id.ratingBar);
            date = v.findViewById(R.id.tvDate);
            status = v.findViewById(R.id.tvStatus);
            btnHide = v.findViewById(R.id.btnHide);
            btnRestore = v.findViewById(R.id.btnRestore);
        }
    }
}