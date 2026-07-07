package com.aurapc.admin.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.aurapc.admin.R;
import com.aurapc.admin.databinding.ActivityMainBinding;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.ui.analytics.AnalyticsFragment;
import com.aurapc.admin.ui.dashboard.DashboardFragment;
import com.aurapc.admin.ui.orders.OrdersFragment;
import com.aurapc.admin.ui.products.ProductsFragment;
import com.aurapc.admin.ui.main.MoreHostFragment;

import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            // Connect socket (only if logged in)
            try {
                ServiceLocator.get().socketManager().connect();
            } catch (Throwable t) {
                android.util.Log.w("MainActivity", "socket connect failed", t);
            }

            setupBottomNav();

            if (savedInstanceState == null) {
                navigateTo(R.id.nav_dashboard);
            }
        } catch (Throwable t) {
            android.util.Log.e("MainActivity", "onCreate crash", t);
            // fallback to Toast so user at least sees something instead of ANR
            android.widget.Toast.makeText(this, "Lỗi khởi động: " + t.getMessage(), android.widget.Toast.LENGTH_LONG).show();
        }
    }

    private void setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_dashboard || id == R.id.nav_orders ||
                    id == R.id.nav_products || id == R.id.nav_analytics || id == R.id.nav_more) {
                navigateTo(id);
                return true;
            }
            return false;
        });
    }

    public void navigateToOrders() {
        binding.bottomNav.setSelectedItemId(R.id.nav_orders);
        navigateTo(R.id.nav_orders);
    }

    public void navigateToProducts() {
        binding.bottomNav.setSelectedItemId(R.id.nav_products);
        navigateTo(R.id.nav_products);
    }

    public void navigateToAnalytics() {
        binding.bottomNav.setSelectedItemId(R.id.nav_analytics);
        navigateTo(R.id.nav_analytics);
    }

    public void navigateTo(int navId) {
        Fragment fragment;
        String tag;

        if (navId == R.id.nav_dashboard) {
            fragment = new DashboardFragment();
            tag = "dashboard";
        } else if (navId == R.id.nav_orders) {
            fragment = new OrdersFragment();
            tag = "orders";
        } else if (navId == R.id.nav_products) {
            fragment = new ProductsFragment();
            tag = "products";
        } else if (navId == R.id.nav_analytics) {
            fragment = new AnalyticsFragment();
            tag = "analytics";
        } else if (navId == R.id.nav_more) {
            fragment = new MoreHostFragment();
            tag = "more";
        } else {
            return;
        }

        currentFragment = fragment;
        getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .replace(R.id.fragmentHost, fragment, tag)
                .commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}