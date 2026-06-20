package com.aura.pc.ui.address;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;

import java.util.ArrayList;
import java.util.List;

/** Adapter hiển thị danh sách thẻ địa chỉ. */
public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.VH> {

    public interface Listener {
        void onSelect(Address address);
        void onEdit(Address address);
        void onDelete(Address address);
        void onSetDefault(Address address);
    }

    private final List<Address> items = new ArrayList<>();
    private final Listener listener;

    public AddressAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Address> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Address a = items.get(position);

        h.name.setText(a.fullName);
        h.phone.setText(a.phone);
        h.address.setText(a.formattedAddress());

        // Badge mặc định
        h.badgeDefault.setVisibility(a.isDefault ? View.VISIBLE : View.GONE);

        // Badge nhãn
        if (a.label != null && !a.label.trim().isEmpty()) {
            h.badgeLabel.setVisibility(View.VISIBLE);
            h.badgeLabel.setText(a.label);
        } else {
            h.badgeLabel.setVisibility(View.GONE);
        }

        // Nút "Đặt mặc định" chỉ hiện khi địa chỉ chưa phải mặc định
        h.setDefault.setVisibility(a.isDefault ? View.GONE : View.VISIBLE);

        h.setDefault.setOnClickListener(v -> listener.onSetDefault(a));
        h.edit.setOnClickListener(v -> listener.onEdit(a));
        h.delete.setOnClickListener(v -> listener.onDelete(a));
        h.itemView.setOnClickListener(v -> listener.onSelect(a));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, phone, address, badgeDefault, badgeLabel, setDefault, edit, delete;

        VH(@NonNull View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            phone = v.findViewById(R.id.tvPhone);
            address = v.findViewById(R.id.tvAddress);
            badgeDefault = v.findViewById(R.id.badgeDefault);
            badgeLabel = v.findViewById(R.id.badgeLabel);
            setDefault = v.findViewById(R.id.btnSetDefault);
            edit = v.findViewById(R.id.btnEdit);
            delete = v.findViewById(R.id.btnDelete);
        }
    }
}
