package com.aura.pc.ui.orders;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.aura_pc_app.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.Holder> {
    public interface RenameListener {
        void onRename(OrderHistoryItem order, String newName, RenameResult result);
    }

    public interface RenameResult {
        void onSuccess();
        void onError(String message);
    }

    private final List<OrderHistoryItem> orders = new ArrayList<>();
    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
    private final RenameListener renameListener;
    private String editingOrderKey;

    public OrderAdapter(RenameListener renameListener) {
        this.renameListener = renameListener;
    }

    public void submit(List<OrderHistoryItem> next) {
        orders.clear();
        if (next != null) {
            orders.addAll(next);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_history_card, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(orders.get(position));
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final View titleDisplayRow;
        private final View titleEditRow;
        private final EditText nameInput;
        private final ImageView editNameButton;
        private final TextView saveNameButton;
        private final TextView cancelNameButton;
        private final TextView orderCode;
        private final TextView status;
        private final ImageView image;
        private final TextView productName;
        private final TextView productPrice;
        private final TextView total;
        private final TextView otherItems;
        private final TextView cancelButton;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.orderCardTitle);
            titleDisplayRow = itemView.findViewById(R.id.orderTitleDisplayRow);
            titleEditRow = itemView.findViewById(R.id.orderTitleEditRow);
            nameInput = itemView.findViewById(R.id.orderNameInput);
            editNameButton = itemView.findViewById(R.id.orderEditNameButton);
            saveNameButton = itemView.findViewById(R.id.orderSaveNameButton);
            cancelNameButton = itemView.findViewById(R.id.orderCancelNameButton);
            orderCode = itemView.findViewById(R.id.orderCardCode);
            status = itemView.findViewById(R.id.orderCardStatus);
            image = itemView.findViewById(R.id.orderProductImage);
            productName = itemView.findViewById(R.id.orderProductName);
            productPrice = itemView.findViewById(R.id.orderProductPrice);
            total = itemView.findViewById(R.id.orderTotal);
            otherItems = itemView.findViewById(R.id.orderOtherItems);
            cancelButton = itemView.findViewById(R.id.orderCancelButton);
        }

        void bind(OrderHistoryItem order) {
            Context context = itemView.getContext();
            String defaultTitle = context.getString(R.string.orders_card_title, formatDate(order.createdAt));
            String displayName = TextUtils.isEmpty(order.name) ? defaultTitle : order.name;
            title.setText(displayName);
            orderCode.setText(context.getString(
                    R.string.orders_card_code,
                    TextUtils.isEmpty(order.code) ? context.getString(R.string.orders_code_missing) : order.code));

            String normalizedStatus = OrderRepository.normalizeStatus(order.status);
            boolean editing = TextUtils.equals(editingOrderKey, keyFor(order));
            titleDisplayRow.setVisibility(editing ? View.GONE : View.VISIBLE);
            titleEditRow.setVisibility(editing ? View.VISIBLE : View.GONE);
            status.setText(statusText(context, normalizedStatus, order.status));
            status.setTextColor(statusColor(normalizedStatus));
            status.setBackgroundResource(statusBackground(normalizedStatus));
            status.setVisibility(editing ? View.GONE : View.VISIBLE);
            nameInput.setText(displayName);
            editNameButton.setOnClickListener(v -> {
                editingOrderKey = keyFor(order);
                notifyCurrentItem();
            });
            cancelNameButton.setOnClickListener(v -> {
                editingOrderKey = null;
                notifyCurrentItem();
            });
            saveNameButton.setOnClickListener(v -> saveName(order));
            nameInput.setOnEditorActionListener((v, actionId, event) -> {
                saveName(order);
                return true;
            });
            cancelButton.setVisibility("processing".equals(normalizedStatus) || "pending".equals(normalizedStatus)
                    ? View.VISIBLE : View.GONE);

            OrderProduct first = order.products.isEmpty() ? null : order.products.get(0);
            if (first == null) {
                productName.setText(R.string.orders_product_missing);
                productPrice.setText("");
                image.setImageResource(R.drawable.aura_laptop);
                otherItems.setVisibility(View.GONE);
            } else {
                productName.setText(TextUtils.isEmpty(first.name)
                        ? context.getString(R.string.orders_product_missing)
                        : first.name);
                productPrice.setText(context.getString(
                        R.string.orders_product_price_qty,
                        currency.format(first.price),
                        Math.max(1, first.quantity)));
                if (TextUtils.isEmpty(first.imageUrl)) {
                    image.setImageResource(R.drawable.aura_laptop);
                } else {
                    Glide.with(image)
                            .load(first.imageUrl)
                            .placeholder(R.drawable.aura_laptop)
                            .error(R.drawable.aura_laptop)
                            .into(image);
                }
                int more = order.products.size() - 1;
                otherItems.setVisibility(more > 0 ? View.VISIBLE : View.GONE);
                otherItems.setText(context.getResources().getQuantityString(
                        R.plurals.orders_more_products, more, more));
            }
            total.setText(context.getString(R.string.orders_total, currency.format(order.total)));
        }

        private void saveName(OrderHistoryItem order) {
            String nextName = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
            if (TextUtils.isEmpty(nextName)) {
                nameInput.setError(itemView.getContext().getString(R.string.orders_name_hint));
                return;
            }
            saveNameButton.setEnabled(false);
            if (renameListener == null) {
                order.name = nextName;
                editingOrderKey = null;
                notifyCurrentItem();
                return;
            }
            renameListener.onRename(order, nextName, new RenameResult() {
                @Override
                public void onSuccess() {
                    saveNameButton.setEnabled(true);
                    order.name = nextName;
                    editingOrderKey = null;
                    notifyCurrentItem();
                }

                @Override
                public void onError(String message) {
                    saveNameButton.setEnabled(true);
                    nameInput.setError(message);
                }
            });
        }

        private String keyFor(OrderHistoryItem order) {
            if (order == null) {
                return "";
            }
            if (!TextUtils.isEmpty(order.id)) {
                return order.id;
            }
            if (!TextUtils.isEmpty(order.code)) {
                return order.code;
            }
            return String.valueOf(getBindingAdapterPosition());
        }

        private void notifyCurrentItem() {
            int position = getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                notifyItemChanged(position);
            } else {
                notifyDataSetChanged();
            }
        }

        private String statusText(Context context, String normalized, String raw) {
            switch (normalized) {
                case "pending":
                    return context.getString(R.string.orders_status_pending);
                case "processing":
                    return context.getString(R.string.orders_status_processing);
                case "shipping":
                    return context.getString(R.string.orders_status_shipping);
                case "delivered":
                    return context.getString(R.string.orders_status_delivered);
                case "cancelled":
                    return context.getString(R.string.orders_status_cancelled);
                case "returned":
                    return context.getString(R.string.orders_status_returned);
                default:
                    return TextUtils.isEmpty(raw) ? context.getString(R.string.orders_status_unknown) : raw;
            }
        }

        private int statusColor(String normalized) {
            if ("cancelled".equals(normalized)) {
                return Color.rgb(229, 57, 53);
            }
            if ("delivered".equals(normalized)) {
                return Color.rgb(46, 125, 50);
            }
            return Color.rgb(255, 107, 0);
        }

        private int statusBackground(String normalized) {
            if ("cancelled".equals(normalized)) {
                return R.drawable.bg_order_status_red;
            }
            if ("delivered".equals(normalized)) {
                return R.drawable.bg_order_status_green;
            }
            return R.drawable.bg_order_status_orange;
        }

        private String formatDate(String value) {
            if (TextUtils.isEmpty(value)) {
                return itemView.getContext().getString(R.string.orders_date_missing);
            }
            String trimmed = value.trim();
            if (trimmed.length() >= 10 && trimmed.charAt(4) == '-' && trimmed.charAt(7) == '-') {
                return trimmed.substring(8, 10) + "/" + trimmed.substring(5, 7) + "/" + trimmed.substring(0, 4);
            }
            return trimmed.length() > 10 ? trimmed.substring(0, 10) : trimmed;
        }
    }
}
