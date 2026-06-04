package com.aura.pc.ui.cart;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.aura.pc.CheckoutActivity;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.entity.CartItemEntity;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;
import java.util.ArrayList;
import java.util.List;

public class CartActivity extends AppCompatActivity {
    private CartItemAdapter cartItemAdapter;
    private CartViewModel viewModel;

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

        findViewById(R.id.cartBackButton).setOnClickListener(v -> finish());
        findViewById(R.id.btnCheckout).setOnClickListener(v -> {
            if (!AuthGate.requireLogin(this, CheckoutActivity.class)) {
                return;
            }
            startActivity(new Intent(this, CheckoutActivity.class));
        });
    }

    private void setupCartList() {
        cartItemAdapter = new CartItemAdapter();
        androidx.recyclerview.widget.RecyclerView cartList = findViewById(R.id.rvCartItems);
        cartList.setLayoutManager(new LinearLayoutManager(this));
        cartList.setAdapter(cartItemAdapter);
        cartList.setNestedScrollingEnabled(false);
    }

    private void observeCartItems() {
        viewModel.getCartItems().observe(this, items -> {
            List<CartItemEntity> displayItems = items == null || items.isEmpty()
                    ? createFallbackItems()
                    : items;
            cartItemAdapter.setItems(displayItems);
            updateCartCount(displayItems.size());
        });
    }

    private void updateCartCount(int count) {
        TextView countText = findViewById(R.id.tvCartItemCount);
        if (countText != null) {
            countText.setText(getString(R.string.cart_item_count_short, count));
        }
    }

    private List<CartItemEntity> createFallbackItems() {
        List<CartItemEntity> items = new ArrayList<>();
        addFallback(items, getString(R.string.cart_dummy_product_name), 1);
        addFallback(items, getString(R.string.cart_dummy_product_name), 1);
        addFallback(items, getString(R.string.cart_dummy_product_name), 1);
        return items;
    }

    private void addFallback(List<CartItemEntity> items, String productId, int quantity) {
        CartItemEntity item = new CartItemEntity();
        item.productId = productId;
        item.quantity = quantity;
        items.add(item);
    }
}
