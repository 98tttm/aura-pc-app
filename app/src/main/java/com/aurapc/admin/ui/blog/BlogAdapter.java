package com.aurapc.admin.ui.blog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.BlogPost;
import com.aurapc.admin.utils.Formatters;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class BlogAdapter extends RecyclerView.Adapter<BlogAdapter.VH> {

    public interface OnClick {
        void onClick(BlogPost post);
    }

    private final List<BlogPost> blogs = new ArrayList<>();
    private final OnClick onClick;

    public BlogAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void setBlogs(List<BlogPost> list) {
        blogs.clear();
        if (list != null) blogs.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_blog, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        BlogPost b = blogs.get(pos);
        h.title.setText(b.title != null ? b.title : "—");
        h.author.setText(b.author != null ? b.author : "Admin");
        h.excerpt.setText(b.excerpt != null ? b.excerpt : (b.content != null ? stripHtml(b.content) : ""));
        h.date.setText(Formatters.formatDate(b.createdAt));
        if (b.coverImage != null && !b.coverImage.isEmpty()) {
            Glide.with(h.cover.getContext()).load(b.coverImage).placeholder(R.drawable.ic_blog).into(h.cover);
        } else {
            h.cover.setImageResource(R.drawable.ic_blog);
        }
        h.itemView.setOnClickListener(v -> onClick.onClick(b));
    }

    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").substring(0, Math.min(120, html.length()));
    }

    @Override public int getItemCount() { return blogs.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, excerpt, author, date;

        VH(View v) {
            super(v);
            cover = v.findViewById(R.id.ivCover);
            title = v.findViewById(R.id.tvTitle);
            excerpt = v.findViewById(R.id.tvExcerpt);
            author = v.findViewById(R.id.tvAuthor);
            date = v.findViewById(R.id.tvDate);
        }
    }
}