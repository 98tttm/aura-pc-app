package com.aurapc.admin.ui.support;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.SupportMessage;
import com.aurapc.admin.utils.Formatters;

import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<SupportMessage> messages = new ArrayList<>();

    public void setMessages(List<SupportMessage> list) {
        messages.clear();
        if (list != null) messages.addAll(list);
        notifyDataSetChanged();
    }

    public void addMessage(SupportMessage m) {
        messages.add(m);
        notifyItemInserted(messages.size() - 1);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        return new VH(v);
    }

    @Override
    public int getItemViewType(int position) {
        SupportMessage m = messages.get(position);
        boolean isAdmin = "admin".equals(m.senderType);
        return isAdmin ? R.layout.item_chat_admin : R.layout.item_chat_user;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        SupportMessage m = messages.get(pos);
        h.content.setText(m.content != null ? m.content : "");
        h.time.setText(Formatters.formatTime(m.createdAt));
        h.sender.setText(m.senderName());
    }

    @Override public int getItemCount() { return messages.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView content, time, sender;

        VH(View v) {
            super(v);
            content = v.findViewById(R.id.tvContent);
            time = v.findViewById(R.id.tvTime);
            sender = v.findViewById(R.id.tvSender);
        }
    }
}