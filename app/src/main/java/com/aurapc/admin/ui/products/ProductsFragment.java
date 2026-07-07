package com.aurapc.admin.ui.products;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.ProductApi;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.Product;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.NetworkHelper;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class ProductsFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvProducts;
    private ProgressBar progress;
    private LinearLayout emptyState;
    private EditText etSearch;
    private ChipGroup stockChips;

    private LiveData<Resource<ProductApi.ProductListResponse>> productsLiveData;
    private ProductsAdapter adapter;
    private String currentQuery = "";
    private String currentStockFilter = null;
    private boolean viewReady;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_products, container, false);
        swipeRefresh = root.findViewById(R.id.swipeRefresh);
        rvProducts = root.findViewById(R.id.rvProducts);
        progress = root.findViewById(R.id.progress);
        emptyState = root.findViewById(R.id.emptyState);
        etSearch = root.findViewById(R.id.etSearch);
        stockChips = root.findViewById(R.id.stockChips);
        View btnAdd = root.findViewById(R.id.btnAdd);

        setupChips();
        adapter = new ProductsAdapter(p -> ProductEditActivity.start(requireContext(), p.id));
        rvProducts.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvProducts.setAdapter(adapter);

        swipeRefresh.setColorSchemeResources(R.color.aura_orange);
        swipeRefresh.setOnRefreshListener(this::loadProducts);

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim();
                loadProducts();
            }
        });

        btnAdd.setOnClickListener(v -> ProductEditActivity.start(requireContext(), null));
        viewReady = true;
        loadProducts();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (viewReady) {
            loadProducts();
        }
    }

    private void setupChips() {
        String[] labels = {"Tất cả", "Còn hàng", "Sắp hết", "Hết hàng"};
        String[] values = {null, "in_stock", "low_stock", "out_of_stock"};

        for (int i = 0; i < labels.length; i++) {
            final String filterValue = values[i];
            Chip chip = new Chip(requireContext());
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setOnClickListener(v -> {
                currentStockFilter = filterValue;
                loadProducts();
            });
            stockChips.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
    }

    private void loadProducts() {
        progress.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
        swipeRefresh.setRefreshing(false);

        if (productsLiveData != null) {
            productsLiveData.removeObservers(getViewLifecycleOwner());
        }
        productsLiveData = NetworkHelper.toLiveData(
                ServiceLocator.get().apiClient().productApi().getProducts(currentQuery, 1, 50));
        productsLiveData.observe(getViewLifecycleOwner(), result -> {
            if (result == null || result.isLoading()) {
                return;
            }
            progress.setVisibility(View.GONE);
            swipeRefresh.setRefreshing(false);
            if (result.isSuccess() && result.data != null) {
                ArrayList<Product> products = new ArrayList<>();
                if (result.data.items != null) {
                    for (Product p : result.data.items) {
                        if (currentStockFilter == null) {
                            products.add(p);
                        } else if ("in_stock".equals(currentStockFilter) && p.stock != null && p.stock > 10) {
                            products.add(p);
                        } else if ("low_stock".equals(currentStockFilter) && p.stock != null && p.stock > 0 && p.stock <= 10) {
                            products.add(p);
                        } else if ("out_of_stock".equals(currentStockFilter) && p.stock != null && p.stock <= 0) {
                            products.add(p);
                        }
                    }
                }
                adapter.setProducts(products);
                emptyState.setVisibility(products.isEmpty() ? View.VISIBLE : View.GONE);
            } else {
                adapter.setProducts(new ArrayList<>());
                emptyState.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), result.message != null ? result.message : getString(R.string.error_generic), Toast.LENGTH_LONG).show();
            }
        });
    }
}
