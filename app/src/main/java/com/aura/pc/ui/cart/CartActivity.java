package com.aura.pc.ui.cart;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.pc.CheckoutActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.ui.home.HomeActivity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CartActivity extends AppCompatActivity {
    private CartItemAdapter cartItemAdapter;
    private CartViewModel viewModel;
    private CheckBox selectAllCheckbox;
    private List<CartItemEntity> latestItems;
    private final NumberFormat currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CART);
        BottomNavigationHelper.setupHeader(this);
        viewModel = new ViewModelProvider(this).get(CartViewModel.class);
        setupCartList();
        observeCartItems();
        observeActions();
        setupSelectAll();

        findViewById(R.id.cartBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.btnCheckout).setOnClickListener(v -> {
            ArrayList<String> selectedKeys = cartItemAdapter.getSelectedKeys();
            if (selectedKeys.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!AuthGate.requireLogin(this, CheckoutActivity.class)) {
                return;
            }
            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putStringArrayListExtra(CheckoutActivity.EXTRA_SELECTED_CART_KEYS, selectedKeys);
            startActivity(intent);
        });
        View browse = findViewById(R.id.btnBrowseProducts);
        if (browse != null) {
            browse.setOnClickListener(v -> startActivity(new Intent(this, HomeActivity.class)));
        }
    }

    private void setupCartList() {
        cartItemAdapter = new CartItemAdapter(new CartItemAdapter.Listener() {
            @Override
            public void onIncrease(CartItemEntity item) {
                viewModel.increment(item);
            }

            @Override
            public void onDecrease(CartItemEntity item) {
                viewModel.decrement(item);
            }

            @Override
            public void onRemove(CartItemEntity item) {
                viewModel.remove(item);
            }

            @Override
            public void onSelectionChanged() {
                updateCartSummary(cartItemAdapter.getSelectedItems(), latestItems == null || latestItems.isEmpty());
                syncSelectAllState();
            }
        });
        RecyclerView cartList = findViewById(R.id.rvCartItems);
        cartList.setLayoutManager(new LinearLayoutManager(this));
        cartList.setAdapter(cartItemAdapter);
        cartList.setNestedScrollingEnabled(false);
    }

    private void observeCartItems() {
        viewModel.getCartItems().observe(this, items -> {
            latestItems = items;
            cartItemAdapter.setItems(items);
            updateCartSummary(cartItemAdapter.getSelectedItems(), items == null || items.isEmpty());
            syncSelectAllState();
        });
    }

    private void setupSelectAll() {
        selectAllCheckbox = findViewById(R.id.cbSelectAll);
        if (selectAllCheckbox == null) return;
        selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                cartItemAdapter.setAllSelected(isChecked));
    }

    private void observeActions() {
        viewModel.errorMessage.observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCartSummary(List<CartItemEntity> items, boolean cartEmpty) {
        int count = 0;
        double subtotal = 0;
        if (items != null) {
            for (CartItemEntity item : items) {
                int quantity = Math.max(0, item.quantity);
                count += quantity;
                subtotal += item.unitPrice * quantity;
            }
        }

        TextView countText = findViewById(R.id.tvCartItemCount);
        if (countText != null) {
            countText.setText(getString(R.string.cart_item_count_short, count));
        }
        TextView subtotalText = findViewById(R.id.tvCartSubtotalPrice);
        if (subtotalText != null) {
            subtotalText.setText(formatPrice(subtotal));
        }
        TextView totalText = findViewById(R.id.tvCartTotalPrice);
        if (totalText != null) {
            totalText.setText(formatPrice(subtotal));
        }

        boolean empty = cartEmpty;
        setVisibility(R.id.emptyCartState, empty);
        setVisibility(R.id.rvCartItems, !empty);
        setVisibility(R.id.promoCodeRow, !empty);
        setVisibility(R.id.orderSummaryCard, !empty);
    }

    private void syncSelectAllState() {
        if (selectAllCheckbox == null) return;
        selectAllCheckbox.setOnCheckedChangeListener(null);
        selectAllCheckbox.setChecked(cartItemAdapter.areAllItemsSelected());
        selectAllCheckbox.setOnCheckedChangeListener((buttonView, isChecked) ->
                cartItemAdapter.setAllSelected(isChecked));
    }

    private void setVisibility(int id, boolean visible) {
        View view = findViewById(id);
        if (view != null) {
            view.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String formatPrice(double price) {
        return currencyFormat.format(Math.max(0, price)) + "đ";
    }
}
