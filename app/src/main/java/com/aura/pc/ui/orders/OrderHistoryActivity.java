package com.aura.pc.ui.orders;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.ui.home.HomeActivity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryActivity extends AppCompatActivity {
    private final List<OrderHistoryItem> allOrders = new ArrayList<>();
    private final List<TextView> tabs = new ArrayList<>();
    private OrderRepository repository;
    private OrderAdapter adapter;
    private RecyclerView recycler;
    private ProgressBar progressBar;
    private View emptyState;
    private EditText searchInput;
    private TextView dateEndText;
    private String selectedStatus = "all";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!AuthGate.requireLogin(this, OrderHistoryActivity.class)) {
            finish();
            return;
        }
        setContentView(R.layout.activity_order_history);

        repository = new OrderRepository(this);
        adapter = new OrderAdapter((order, newName, result) ->
                repository.updateOrderName(order, newName, new OrderRepository.RenameCallback() {
                    @Override
                    public void onSuccess() {
                        result.onSuccess();
                        applyFilters();
                        Toast.makeText(OrderHistoryActivity.this,
                                R.string.orders_name_saved, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(String message) {
                        result.onError(message);
                    }
                }));
        bindViews();
        setupActions();
        renderTabs();
        loadOrders();
    }

    private void bindViews() {
        recycler = findViewById(R.id.ordersRecycler);
        progressBar = findViewById(R.id.ordersProgress);
        emptyState = findViewById(R.id.ordersEmptyState);
        searchInput = findViewById(R.id.ordersSearchInput);
        dateEndText = findViewById(R.id.ordersDateEnd);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        tabs.add(findViewById(R.id.ordersTabAll));
        tabs.add(findViewById(R.id.ordersTabProcessing));
        tabs.add(findViewById(R.id.ordersTabShipping));
        tabs.add(findViewById(R.id.ordersTabDelivered));
        tabs.add(findViewById(R.id.ordersTabCancelled));
        tabs.add(findViewById(R.id.ordersTabReturned));
    }

    private void setupActions() {
        findViewById(R.id.ordersBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.ordersHomeButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });
        dateEndText.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date()));
        setupTab(R.id.ordersTabAll, "all");
        setupTab(R.id.ordersTabProcessing, "processing");
        setupTab(R.id.ordersTabShipping, "shipping");
        setupTab(R.id.ordersTabDelivered, "delivered");
        setupTab(R.id.ordersTabCancelled, "cancelled");
        setupTab(R.id.ordersTabReturned, "returned");
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable s) { }
        });
    }

    private void setupTab(int id, String status) {
        View tab = findViewById(id);
        tab.setOnClickListener(v -> {
            selectedStatus = status;
            renderTabs();
            applyFilters();
        });
    }

    private void loadOrders() {
        progressBar.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        recycler.setVisibility(View.GONE);
        repository.load(new OrderRepository.Callback2() {
            @Override
            public void onSuccess(List<OrderHistoryItem> orders) {
                progressBar.setVisibility(View.GONE);
                allOrders.clear();
                if (orders != null) {
                    allOrders.addAll(orders);
                }
                renderTabs();
                applyFilters();
            }

            @Override
            public void onError(String message) {
                progressBar.setVisibility(View.GONE);
                allOrders.clear();
                applyFilters();
                Toast.makeText(OrderHistoryActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        String query = searchInput.getText() == null ? "" : searchInput.getText().toString();
        List<OrderHistoryItem> filtered = new ArrayList<>();
        for (OrderHistoryItem order : allOrders) {
            String normalized = OrderRepository.normalizeStatus(order.status);
            boolean statusMatch = "all".equals(selectedStatus) || selectedStatus.equals(normalized);
            if (statusMatch && order.matchesQuery(query)) {
                filtered.add(order);
            }
        }
        adapter.submit(filtered);
        boolean empty = filtered.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void renderTabs() {
        for (TextView tab : tabs) {
            String status = statusForTab(tab.getId());
            boolean selected = selectedStatus.equals(status);
            tab.setSelected(selected);
            tab.setTextColor(getColor(selected ? R.color.orange_primary : R.color.checkout_gray));
        }
    }

    private String statusForTab(int id) {
        if (id == R.id.ordersTabProcessing) return "processing";
        if (id == R.id.ordersTabShipping) return "shipping";
        if (id == R.id.ordersTabDelivered) return "delivered";
        if (id == R.id.ordersTabCancelled) return "cancelled";
        if (id == R.id.ordersTabReturned) return "returned";
        return "all";
    }
}
