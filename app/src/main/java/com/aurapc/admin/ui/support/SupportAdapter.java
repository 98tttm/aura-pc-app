package com.aurapc.admin.ui.support;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.SupportConversation;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SupportAdapter extends RecyclerView.Adapter<SupportAdapter.VH> {

    public interface OnClick {
        void onClick(SupportConversation conv);
    }

    private final List<SupportConversation> items = new ArrayList<>();
    private final OnClick onClick;

    public SupportAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void setConversations(List<SupportConversation> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_support, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SupportConversation c = items.get(pos);
        String name = extractUserName(c);
        h.name.setText(name);
        h.lastMessage.setText(c.lastMessagePreview != null ? c.lastMessagePreview : "—");
        h.time.setText(Formatters.formatDate(c.lastMessageAt != null ? c.lastMessageAt : c.updatedAt));
        int unread = c.unreadForAdmin != null ? c.unreadForAdmin : 0;
        h.unread.setText(String.valueOf(unread));
        h.unread.setVisibility(unread > 0 ? View.VISIBLE : View.GONE);

        h.initial.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());

        h.itemView.setOnClickListener(v -> onClick.onClick(c));
    }

    private String extractUserName(SupportConversation c) {
        if (c.user instanceof Map) {
            Map m = (Map) c.user;
            Object phone = m.get("phoneNumber");
            if (phone != null) return phone.toString();
            Object fn = m.get("username");
            if (fn != null) return fn.toString();
            Object profile = m.get("profile");
            if (profile instanceof Map) {
                Object fullName = ((Map) profile).get("fullName");
                if (fullName != null) return fullName.toString();
            }
        }
        return "Khách hàng";
    }

    @Override public int getItemCount() { return items.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView initial, name, lastMessage, time, unread;

        VH(View v) {
            super(v);
            initial = v.findViewById(R.id.tvInitial);
            name = v.findViewById(R.id.tvName);
            lastMessage = v.findViewById(R.id.tvLastMessage);
            time = v.findViewById(R.id.tvTime);
            unread = v.findViewById(R.id.tvUnread);
        }
    }
}