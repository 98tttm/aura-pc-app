package com.example.aura_pc_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.domain.model.ProductSpec;

import java.util.List;

public class SpecAdapter extends RecyclerView.Adapter<SpecAdapter.ViewHolder> {
    private List<ProductSpec> specs;

    public SpecAdapter(List<ProductSpec> specs) {
        this.specs = specs;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spec, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ProductSpec spec = specs.get(position);
        holder.label.setText(spec.getLabel());
        holder.value.setText(spec.getValue());
        holder.icon.setImageResource(spec.getIconResId());
    }

    @Override
    public int getItemCount() {
        return specs != null ? specs.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView label, value;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.specIcon);
            label = itemView.findViewById(R.id.specLabel);
            value = itemView.findViewById(R.id.specValue);
        }
    }
}
