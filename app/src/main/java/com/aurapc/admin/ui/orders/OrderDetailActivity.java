package com.aurapc.admin.ui.orders;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurapc.admin.R;
import com.aurapc.admin.data.model.Order;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.Formatters;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.HashMap;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ORDER_NUMBER = "orderNumber";

    private MaterialToolbar toolbar;
    private TextView tvOrderNumber, tvStatus, tvCreatedAt, tvCustomerName,
            tvCustomerPhone, tvAddress, tvSubtotal, tvShipping, tvDiscount,
            tvTotal, tvPaymentMethod, tvPaymentStatus;
    private RecyclerView rvItems;
    private ChipGroup statusChips;
    private MaterialButton btnSaveStatus;
    private View progressView;

    private Order order;
    private String selectedStatus;

    public static void start(android.content.Context ctx, String orderNumber) {
        android.content.Intent i = new android.content.Intent(ctx, OrderDetailActivity.class);
        i.putExtra(EXTRA_ORDER_NUMBER, orderNumber);
        ctx.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        toolbar = findViewById(R.id.toolbar);
        tvOrderNumber = findViewById(R.id.tvOrderNumber);
        tvStatus = findViewById(R.id.tvStatus);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvCustomerName = findViewById(R.id.tvCustomerName);
        tvCustomerPhone = findViewById(R.id.tvCustomerPhone);
        tvAddress = findViewById(R.id.tvAddress);
        tvSubtotal = findViewById(R.id.tvSubtotal);
        tvShipping = findViewById(R.id.tvShipping);
        tvDiscount = findViewById(R.id.tvDiscount);
        tvTotal = findViewById(R.id.tvTotal);
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod);
        tvPaymentStatus = findViewById(R.id.tvPaymentStatus);
        rvItems = findViewById(R.id.rvItems);
        statusChips = findViewById(R.id.statusChips);
        btnSaveStatus = findViewById(R.id.btnSaveStatus);
        progressView = findViewById(R.id.progress);

        toolbar.setNavigationOnClickListener(v -> finish());
        btnSaveStatus.setOnClickListener(v -> saveStatus());

        setupStatusChips();
        loadOrder();
    }

    private void setupStatusChips() {
        // Admin can only update to processing or shipped per backend
        String[] statuses = {"processing", "shipped", "cancelled"};
        String[] labels = {"Đang xử lý", "Đang giao", "Hủy đơn"};

        for (int i = 0; i < statuses.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            final String s = statuses[i];
            chip.setTag(s);
            chip.setOnClickListener(v -> selectedStatus = s);
            statusChips.addView(chip);
        }
    }

    private void loadOrder() {
        progressView.setVisibility(View.VISIBLE);
        String orderNumber = getIntent().getStringExtra(EXTRA_ORDER_NUMBER);
        if (orderNumber == null) {
            finish();
            return;
        }
        ServiceLocator.get().apiClient().orderApi().getOrderDetail(orderNumber).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                progressView.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    order = response.body();
                    bindOrder();
                } else {
                    Toast.makeText(OrderDetailActivity.this, "Không thể tải đơn hàng", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                progressView.setVisibility(View.GONE);
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindOrder() {
        if (order == null) return;
        tvOrderNumber.setText(order.orderNumber != null ? order.orderNumber : "#" + (order.id != null ? order.id : ""));
        tvStatus.setText(getStatusLabel(order.status));
        tvCreatedAt.setText(Formatters.formatDateTime(order.createdAt));

        if (order.shippingAddress != null) {
            String name = order.shippingAddress.name != null ? order.shippingAddress.name : order.shippingAddress.fullName;
            tvCustomerName.setText(name != null ? name : "");
            tvCustomerPhone.setText(order.shippingAddress.phone != null ? order.shippingAddress.phone : "");
            StringBuilder addr = new StringBuilder();
            if (order.shippingAddress.street != null) addr.append(order.shippingAddress.street);
            if (order.shippingAddress.ward != null) addr.append(", ").append(order.shippingAddress.ward);
            if (order.shippingAddress.district != null) addr.append(", ").append(order.shippingAddress.district);
            if (order.shippingAddress.city != null) addr.append(", ").append(order.shippingAddress.city);
            tvAddress.setText(addr.length() > 0 ? addr.toString() : (order.shippingAddress.address != null ? order.shippingAddress.address : "—"));
        }

        tvSubtotal.setText(Formatters.formatVnd(order.subtotal));
        tvShipping.setText(Formatters.formatVnd(order.shippingFee));
        double discount = order.discountAmount != null ? order.discountAmount : 0;
        tvDiscount.setText(discount > 0 ? "-" + Formatters.formatVnd(discount) : "0đ");
        Double total = order.totalAmount != null ? order.totalAmount : order.total;
        tvTotal.setText(Formatters.formatVnd(total));
        tvPaymentMethod.setText(order.paymentMethod != null ? order.paymentMethod.toUpperCase() : "COD");
        boolean paid = "paid".equals(order.paymentStatus) || "completed".equals(order.paymentStatus);
        tvPaymentStatus.setText(paid ? "Đã thanh toán" : "Chưa thanh toán");

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        rvItems.setAdapter(new OrderItemsAdapter(order.items));

        selectedStatus = order.status;
        for (int i = 0; i < statusChips.getChildCount(); i++) {
            Chip c = (Chip) statusChips.getChildAt(i);
            c.setChecked(c.getTag().equals(selectedStatus));
        }
    }

    private void saveStatus() {
        if (selectedStatus == null || selectedStatus.equals(order.status)) return;

        btnSaveStatus.setEnabled(false);
        final String newStatus = selectedStatus;

        if ("cancelled".equals(newStatus)) {
            HashMap<String, Object> body = new HashMap<>();
            body.put("reason", "Admin hủy đơn");
            ServiceLocator.get().apiClient().orderApi().cancel(order.orderNumber, body).enqueue(new Callback<Order>() {
                @Override
                public void onResponse(Call<Order> call, Response<Order> response) {
                    btnSaveStatus.setEnabled(true);
                    if (response.isSuccessful() && response.body() != null) {
                        order = response.body();
                        bindOrder();
                        Toast.makeText(OrderDetailActivity.this, "Đã hủy đơn", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(OrderDetailActivity.this, "Hủy thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Order> call, Throwable t) {
                    btnSaveStatus.setEnabled(true);
                    Toast.makeText(OrderDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        HashMap<String, String> body = new HashMap<>();
        body.put("status", newStatus);

        ServiceLocator.get().apiClient().orderApi().updateStatus(order.orderNumber, body).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(Call<Order> call, Response<Order> response) {
                btnSaveStatus.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    order = response.body();
                    bindOrder();
                    Toast.makeText(OrderDetailActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                } else {
                    String err = "Cập nhật thất bại";
                    try {
                        if (response.errorBody() != null) err = response.errorBody().string();
                    } catch (Exception ignored) {}
                    Toast.makeText(OrderDetailActivity.this, err, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Order> call, Throwable t) {
                btnSaveStatus.setEnabled(true);
                Toast.makeText(OrderDetailActivity.this, "Lỗi: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getStatusLabel(String status) {
        if (status == null) return "—";
        switch (status) {
            case "pending": return "Chờ xác nhận";
            case "confirmed": return "Đã xác nhận";
            case "processing": return "Đang xử lý";
            case "shipping": return "Đang giao hàng";
            case "shipped": return "Đang giao";
            case "delivered": return "Đã giao";
            case "cancelled": return "Đã hủy";
            case "refunded": return "Hoàn tiền";
            default: return status;
        }
    }
}
