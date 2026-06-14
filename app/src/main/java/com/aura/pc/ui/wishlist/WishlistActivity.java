package com.aura.pc.ui.wishlist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aura.pc.ui.products.ProductListActivity;
import com.aura.pc.ui.productdetail.ProductDetailActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.entity.WishlistEntity;
import com.example.aura_pc_app.utils.LocaleManager;

import java.util.List;
import java.util.concurrent.Executors;

public class WishlistActivity extends AppCompatActivity {

    private RecyclerView rvWishlist;
    private View emptyState;
    private WishlistAdapter adapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        rvWishlist = findViewById(R.id.rvWishlist);
        emptyState = findViewById(R.id.emptyState);
        View btnBack = findViewById(R.id.btnBack);
        View btnExplore = findViewById(R.id.btnExplore);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (btnExplore != null) {
            btnExplore.setOnClickListener(v -> {
                startActivity(new Intent(this, ProductListActivity.class));
                finish();
            });
        }

        setupRecyclerView();
        loadWishlist();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadWishlist();
    }

    private void setupRecyclerView() {
        adapter = new WishlistAdapter();
        rvWishlist.setLayoutManager(new LinearLayoutManager(this));
        rvWishlist.setAdapter(adapter);

        adapter.setOnRemoveListener((item, position) -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                AppDatabase.getInstance(this).wishlistDao().deleteByProductId(item.productId);
                runOnUiThread(() -> {
                    adapter.removeAt(position);
                    Toast.makeText(this, getString(R.string.wishlist_removed), Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
            });
        });

        adapter.setOnAddToCartListener(item -> {
            Toast.makeText(this, getString(R.string.toast_added_to_cart), Toast.LENGTH_SHORT).show();
        });

        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, ProductDetailActivity.class);
            intent.putExtra("product_id", item.productId);
            startActivity(intent);
        });
    }

    private void loadWishlist() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<WishlistEntity> items = AppDatabase.getInstance(this).wishlistDao().getAllSync();
            runOnUiThread(() -> {
                adapter.submitList(items);
                updateEmptyState();
            });
        });
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            rvWishlist.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvWishlist.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }
}
