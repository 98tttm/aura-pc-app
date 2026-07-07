package com.aurapc.admin.ui.users;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.CustomerUser;
import com.aurapc.admin.utils.Formatters;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.VH> {

    public interface OnClick {
        void onClick(CustomerUser user);
    }

    private final List<CustomerUser> users = new ArrayList<>();
    private final OnClick onClick;

    public UsersAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    public void setUsers(List<CustomerUser> list) {
        users.clear();
        if (list != null) users.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CustomerUser u = users.get(pos);
        String name = u.getDisplayName();
        h.tvName.setText(name);
        h.tvInitial.setText(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        h.tvPhone.setText(u.getPhone() != null ? u.getPhone() : "—");
        h.tvEmail.setText(u.email != null ? u.email : "—");
        int orders = u.orderCount != null ? u.orderCount : 0;
        h.tvOrders.setText(orders + " đơn");
        double spent = u.totalSpent != null ? u.totalSpent : 0;
        h.tvSpent.setText(Formatters.formatVnd(spent));
        h.tvStatus.setText(u.active() ? "Hoạt động" : "Đã khóa");
        h.tvStatus.setBackgroundResource(u.active() ? R.drawable.bg_badge_success : R.drawable.bg_badge_danger);
        h.tvStatus.setTextColor(u.active() ? 0xFF16A34A : 0xFFDC2626);

        if (u.avatar != null && !u.avatar.isEmpty()) {
            Glide.with(h.ivAvatar.getContext()).load(u.avatar).placeholder(R.drawable.ic_avatar).into(h.ivAvatar);
        } else {
            h.ivAvatar.setImageResource(R.drawable.ic_avatar);
        }

        h.itemView.setOnClickListener(v -> onClick.onClick(u));
    }

    @Override public int getItemCount() { return users.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvInitial, tvName, tvPhone, tvEmail, tvOrders, tvSpent, tvStatus;

        VH(View v) {
            super(v);
            ivAvatar = v.findViewById(R.id.ivAvatar);
            tvInitial = v.findViewById(R.id.tvInitial);
            tvName = v.findViewById(R.id.tvName);
            tvPhone = v.findViewById(R.id.tvPhone);
            tvEmail = v.findViewById(R.id.tvEmail);
            tvOrders = v.findViewById(R.id.tvOrders);
            tvSpent = v.findViewById(R.id.tvSpent);
            tvStatus = v.findViewById(R.id.tvStatus);
        }
    }
}