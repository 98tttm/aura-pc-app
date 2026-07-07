package com.aurapc.admin.ui.orders;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.OrderApi;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.Order;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class OrdersFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvOrders;
    private ProgressBar progress;
    private LinearLayout emptyState;
    private EditText etSearch;
    private ChipGroup statusChips;

    private LiveData<Resource<OrderApi.OrderListResponse>> ordersLiveData;
    private OrdersAdapter adapter;
    private String currentStatus = null;
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_orders, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvOrders = root.findViewById(R.id.rvOrders);
        progress = root.findViewById(R.id.progress);
        emptyState = root.findViewById(R.id.emptyState);
        etSearch = root.findViewById(R.id.etSearch);
        statusChips = root.findViewById(R.id.statusChips);

        setupChips();
        adapter = new OrdersAdapter(order -> {
            OrderDetailActivity.start(requireContext(), order.orderNumber);
        });
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvOrders.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.aura_orange);
        swipeRefresh.setOnRefreshListener(this::loadOrders);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim();
                loadOrders();
            }
        });

        loadOrders();
        return root;
    }

    private void setupChips() {
        String[] statuses = {"Tất cả", "pending", "confirmed", "shipping", "delivered", "cancelled"};
        String[] labels = {"Tất cả", "Chờ xác nhận", "Đã xác nhận", "Đang giao", "Đã giao", "Đã hủy"};

        for (int i = 0; i < statuses.length; i++) {
            final String statusValue = statuses[i].equals("Tất cả") ? null : statuses[i];
            Chip chip = new Chip(requireContext());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColorResource(R.color.bg_surface_elevated);
            chip.setTextColor(getResources().getColor(R.color.text_primary, null));
            chip.setOnClickListener(v -> {
                currentStatus = statusValue;
                loadOrders();
            });
            statusChips.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
    }

    private void loadOrders() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);

        ordersLiveData = NetworkHelper.toLiveData(
                ServiceLocator.get().apiClient().orderApi().getOrders(currentStatus, currentQuery, 1, 50));
        ordersLiveData.observe(getViewLifecycleOwner(), result -> {
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<Order> orders = new ArrayList<>();
                if (result.data.items != null) {
                    for (Order o : result.data.items) {
                        if (currentStatus == null || currentStatus.isEmpty() || currentStatus.equals(o.status)) {
                            orders.add(o);
                        }
                    }
                }
                adapter.setOrders(orders);
                emptyState.setVisibility(orders.isEmpty() ? View.VISIBLE : View.GONE);
                // Cache each order for offline quick-lookup
                for (Order o : orders) {
                    if (o.orderNumber != null) ServiceLocator.get().localCache().putOrder(o.orderNumber, o);
                }
            } else {
                emptyState.setVisibility(View.VISIBLE);
            }
        });
    }
}
