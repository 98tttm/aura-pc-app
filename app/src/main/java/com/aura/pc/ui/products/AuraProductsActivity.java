package com.aura.pc.ui.products;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.aura.pc.utils.BottomNavigationHelper;
import com.bumptech.glide.Glide;
import com.example.aura_pc_app.MainActivity;
import com.example.aura_pc_app.R;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.utils.LocaleManager;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuraProductsActivity extends AppCompatActivity {
    public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    public static final String EXTRA_CATEGORY_NAME = "extra_category_name";

    private GridLayout productGrid;
    private ProgressBar loadingProgress;
    private NumberFormat currencyFormat;
    private String categoryId;
    private String categoryName;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aura_products);

        currencyFormat = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);

        bindViews();
        setupActions();
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_CATEGORIES);
        loadProducts();
    }

    private void bindViews() {
        productGrid = findViewById(R.id.productGrid);
        loadingProgress = findViewById(R.id.loadingProgress);
        EditText searchInput = findViewById(R.id.searchInput);
        if (searchInput != null && categoryName != null && !categoryName.isEmpty()) {
            searchInput.setHint(getString(R.string.products_for_category, categoryName));
        }
    }

    private void setupActions() {
        View menu = findViewById(R.id.topMenuButton);
        View search = findViewById(R.id.topSearchButton);
        ImageButton filter = findViewById(R.id.searchFilterButton);
        ImageButton fab = findViewById(R.id.contextualFab);

        if (menu != null) menu.setOnClickListener(v -> finish());
        if (search != null) search.setOnClickListener(v -> focusSearch());
        if (filter != null) filter.setOnClickListener(v ->
                Toast.makeText(this, R.string.category_filter_pending, Toast.LENGTH_SHORT).show());
        if (fab != null) fab.setOnClickListener(v ->
                Toast.makeText(this, R.string.category_filter_pending, Toast.LENGTH_SHORT).show());
    }

    private void focusSearch() {
        EditText searchInput = findViewById(R.id.searchInput);
        if (searchInput != null) {
            searchInput.requestFocus();
        }
    }

    private void loadProducts() {
        setLoading(true);
        Call<Map<String, Object>> call = TextUtils.isEmpty(categoryId)
                ? ApiClient.getInstance(this).getApiService().getProducts()
                : ApiClient.getInstance(this).getApiService().getProductsByCategory(categoryId, 1, 60);
        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                setLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> items = readItems(response.body());
                    renderProducts(items);
                    return;
                }
                showEmptyOrError(R.string.products_load_failed);
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                setLoading(false);
                showEmptyOrError(R.string.products_load_failed);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readItems(Map<String, Object> body) {
        Object value = body.get("items");
        if (value instanceof List) {
            return (List<Map<String, Object>>) value;
        }
        return new ArrayList<>();
    }

    private void renderProducts(List<Map<String, Object>> products) {
        if (productGrid == null) return;
        productGrid.removeAllViews();
        if (products == null || products.isEmpty()) {
            showEmptyOrError(R.string.products_empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < products.size(); i++) {
            Map<String, Object> product = products.get(i);
            FrameLayout wrapper = new FrameLayout(this);
            GridLayout.LayoutParams wrapperParams = new GridLayout.LayoutParams();
            wrapperParams.width = 0;
            wrapperParams.height = dpToPx(248);
            wrapperParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            wrapperParams.setMargins(
                    i % 2 == 0 ? 0 : dpToPx(5),
                    i % 2 == 0 ? 0 : dpToPx(12),
                    i % 2 == 0 ? dpToPx(5) : 0,
                    dpToPx(14)
            );
            wrapper.setLayoutParams(wrapperParams);

            View card = inflater.inflate(R.layout.item_aura_product_card, wrapper, false);
            bindProductCard(card, product);
            card.setOnClickListener(v -> startActivity(new Intent(this, MainActivity.class)));
            wrapper.addView(card, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            productGrid.addView(wrapper);
        }
    }

    @SuppressWarnings("unchecked")
    private void bindProductCard(View card, Map<String, Object> product) {
        TextView title = card.findViewById(R.id.productTitle);
        TextView price = card.findViewById(R.id.productPriceText);
        TextView oldPrice = card.findViewById(R.id.oldPriceText);
        TextView rating = card.findViewById(R.id.productRatingText);
        ImageView image = card.findViewById(R.id.productImage);

        if (title != null) {
            title.setText(stringValue(product.get("name"), getString(R.string.home_product_case_name)));
        }

        double priceValue = numberValue(product.get("price"));
        double oldPriceValue = numberValue(product.get("old_price"));
        if (price != null) {
            price.setText(formatPrice(priceValue));
        }
        if (oldPrice != null) {
            if (oldPriceValue > 0 && oldPriceValue > priceValue) {
                oldPrice.setVisibility(View.VISIBLE);
                oldPrice.setText(formatPrice(oldPriceValue));
                oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                oldPrice.setVisibility(View.GONE);
            }
        }
        if (rating != null) {
            rating.setText(R.string.home_product_rating_short);
        }

        Object images = product.get("images");
        if (image != null && images instanceof List && !((List<?>) images).isEmpty()) {
            Object firstImage = ((List<?>) images).get(0);
            Glide.with(this)
                    .load(String.valueOf(firstImage))
                    .placeholder(R.drawable.figma_sale_case)
                    .error(R.drawable.figma_sale_case)
                    .into(image);
        }
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) return fallback;
        String text = String.valueOf(value);
        return text.trim().isEmpty() ? fallback : text;
    }

    private double numberValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0;
    }

    private String formatPrice(double price) {
        if (price <= 0) {
            return getString(R.string.product_contact_price);
        }
        return currencyFormat.format(price) + "đ";
    }

    private void showEmptyOrError(int messageRes) {
        if (productGrid != null) {
            productGrid.removeAllViews();
            TextView message = new TextView(this);
            message.setGravity(android.view.Gravity.CENTER);
            message.setText(messageRes);
            message.setTextColor(getColor(R.color.aura_muted));
            message.setTextSize(14);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = dpToPx(160);
            message.setLayoutParams(params);
            productGrid.addView(message);
        }
    }

    private void setLoading(boolean loading) {
        if (loadingProgress != null) {
            loadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }
}
