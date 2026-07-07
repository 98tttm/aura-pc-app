package com.aurapc.admin.ui.hub;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.HubPost;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HubAdapter extends RecyclerView.Adapter<HubAdapter.VH> {

    public interface OnApprove {
        void onApprove(HubPost post);
    }
    public interface OnReject {
        void onReject(HubPost post);
    }
    public interface OnDelete {
        void onDelete(HubPost post);
    }

    private final List<HubPost> posts = new ArrayList<>();
    private final OnApprove onApprove;
    private final OnReject onReject;
    private final OnDelete onDelete;

    public HubAdapter(OnApprove onApprove, OnReject onReject, OnDelete onDelete) {
        this.onApprove = onApprove;
        this.onReject = onReject;
        this.onDelete = onDelete;
    }

    public void setPosts(List<HubPost> list) {
        posts.clear();
        if (list != null) posts.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hub_post, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        HubPost p = posts.get(pos);
        h.author.setText(extractAuthor(p));
        h.content.setText(p.content != null ? p.content : "");
        h.date.setText(Formatters.formatDate(p.createdAt));
        h.topic.setText(p.topic != null ? "#" + p.topic : "");
        h.likes.setText(String.valueOf(p.likeCount != null ? p.likeCount : 0));
        h.comments.setText(String.valueOf(p.commentCount != null ? p.commentCount : 0));

        String status = p.status != null ? p.status : "pending";
        h.status.setText(mapStatus(status));
        h.status.setBackgroundResource(getStatusBg(status));
        h.status.setTextColor(getStatusColor(status));

        boolean showApproveReject = "pending".equals(status);
        h.btnApprove.setVisibility(showApproveReject ? View.VISIBLE : View.GONE);
        h.btnReject.setVisibility(showApproveReject ? View.VISIBLE : View.GONE);

        h.btnApprove.setOnClickListener(v -> onApprove.onApprove(p));
        h.btnReject.setOnClickListener(v -> onReject.onReject(p));
        h.btnDelete.setOnClickListener(v -> onDelete.onDelete(p));
    }

    private String extractAuthor(HubPost p) {
        if (p.author instanceof Map) {
            Object fn = ((Map) p.author).get("username");
            if (fn != null) return fn.toString();
            Object fullName = ((Map) p.author).get("fullName");
            if (fullName != null) return fullName.toString();
        } else if (p.author != null) {
            return p.author.toString();
        }
        return "Ẩn danh";
    }

    private String mapStatus(String s) {
        switch (s) {
            case "approved": return "Đã duyệt";
            case "rejected": return "Bị từ chối";
            case "pending":
            default: return "Chờ duyệt";
        }
    }

    private int getStatusBg(String s) {
        switch (s) {
            case "approved": return R.drawable.bg_badge_success;
            case "rejected": return R.drawable.bg_badge_danger;
            default: return R.drawable.bg_badge_warning;
        }
    }

    private int getStatusColor(String s) {
        switch (s) {
            case "approved": return 0xFF16A34A;
            case "rejected": return 0xFFDC2626;
            default: return 0xFFEAB308;
        }
    }

    @Override public int getItemCount() { return posts.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView author, content, date, topic, status, likes, comments;
        View btnApprove, btnReject, btnDelete;

        VH(View v) {
            super(v);
            author = v.findViewById(R.id.tvAuthor);
            content = v.findViewById(R.id.tvContent);
            date = v.findViewById(R.id.tvDate);
            topic = v.findViewById(R.id.tvTopic);
            status = v.findViewById(R.id.tvStatus);
            likes = v.findViewById(R.id.tvLikes);
            comments = v.findViewById(R.id.tvComments);
            btnApprove = v.findViewById(R.id.btnApprove);
            btnReject = v.findViewById(R.id.btnReject);
            btnDelete = v.findViewById(R.id.btnDelete);
        }
    }
}
