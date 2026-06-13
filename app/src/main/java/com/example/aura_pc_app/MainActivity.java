package com.example.aura_pc_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.aura.pc.CheckoutActivity;
import com.aura.pc.ui.blog.BlogActivity;
import com.aura.pc.ui.cart.CartActivity;
import com.example.aura_pc_app.adapter.ProductImageAdapter;
import com.example.aura_pc_app.adapter.RelatedProductAdapter;
import com.example.aura_pc_app.adapter.SpecAdapter;
import com.example.aura_pc_app.adapter.ViewedProductAdapter;
import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.domain.repository.mock.MockData;
import com.aura.pc.utils.BottomNavigationHelper;
import com.example.aura_pc_app.domain.model.Product;
import com.example.aura_pc_app.domain.model.ProductSpec;
import com.example.aura_pc_app.utils.AuthGate;
import com.example.aura_pc_app.utils.LocaleManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView thumbnailRecyclerView, specsRecyclerView, relatedRecyclerView, viewedRecyclerView;
    private TextView productName, ratingText, soldCount, reviewCount, currentPrice, oldPrice, discountBadge, productDescription;
    private ImageView mainProductImage, btnFavorite, descriptionImage;
    private android.view.View btnConsult, btnAddToCart, btnBuyNow;
    private boolean isFavorite = false;
    
    private String currentProductId;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        setupPrimaryNavigationOverrides();
        
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("product_id")) {
            currentProductId = intent.getStringExtra("product_id");
        }
        
        loadData();
        
        // Initialize Bottom Navigation
        BottomNavigationHelper.setup(this, BottomNavigationHelper.TAB_HOME);
        BottomNavigationHelper.setupHeader(this);
    }

    private void initViews() {
        mainProductImage = findViewById(R.id.mainProductImage);
        productName = findViewById(R.id.productName);
        ratingText = findViewById(R.id.ratingText);
        soldCount = findViewById(R.id.soldCount);
        reviewCount = findViewById(R.id.reviewCount);
        currentPrice = findViewById(R.id.currentPrice);
        oldPrice = findViewById(R.id.oldPrice);
        discountBadge = findViewById(R.id.discountBadge);
        productDescription = findViewById(R.id.productDescription);
        descriptionImage = findViewById(R.id.descriptionImage);
        btnFavorite = findViewById(R.id.btnFavorite);

        android.view.View actionOverlay = findViewById(R.id.actionOverlay);
        if (actionOverlay != null) {
            btnConsult = actionOverlay.findViewById(R.id.btnConsult);
            btnAddToCart = actionOverlay.findViewById(R.id.btnAddToCart);
            btnBuyNow = actionOverlay.findViewById(R.id.btnBuyNow);
        } else {
            btnConsult = findViewById(R.id.btnConsult);
            btnAddToCart = findViewById(R.id.btnAddToCart);
            btnBuyNow = findViewById(R.id.btnBuyNow);
        }

        android.view.View bottomNavCard = findViewById(R.id.navOverlay);
        if (bottomNavCard == null) {
            bottomNavCard = findViewById(R.id.bottomNavCard);
        }

        thumbnailRecyclerView = findViewById(R.id.thumbnailRecyclerView);
        specsRecyclerView = findViewById(R.id.specsRecyclerView);
        relatedRecyclerView = findViewById(R.id.relatedRecyclerView);
        viewedRecyclerView = findViewById(R.id.viewedRecyclerView);

        if (oldPrice != null) {
            oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        }

        if (btnFavorite != null) {
            btnFavorite.setOnClickListener(v -> {
                isFavorite = !isFavorite;
                if (isFavorite) {
                    btnFavorite.setColorFilter(getResources().getColor(R.color.orange_primary, getTheme()));
                } else {
                    btnFavorite.setColorFilter(getResources().getColor(R.color.gray_text, getTheme()));
                }
            });
        }

        if (btnConsult != null) {
            btnConsult.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Đang kết nối với tư vấn viên...", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Đã thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Đang chuyển đến trang thanh toán", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (bottomNavCard != null) {
            bottomNavCard.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Chức năng thanh điều hướng đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void setupPrimaryNavigationOverrides() {
        if (btnConsult != null) {
            btnConsult.setOnClickListener(v -> startActivity(new Intent(this, BlogActivity.class)));
        }
        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                if (!AuthGate.requireLogin(this, CartActivity.class)) {
                    return;
                }
                android.widget.Toast.makeText(this, "Added to cart", android.widget.Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, CartActivity.class));
            });
        }
        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                if (!AuthGate.requireLogin(this, CheckoutActivity.class)) {
                    return;
                }
                startActivity(new Intent(this, CheckoutActivity.class));
            });
        }
    }

    private void loadData() {
        if (currentProductId != null && !currentProductId.isEmpty()) {
            ApiClient.getInstance(this).getApiService().getProductById(currentProductId).enqueue(new retrofit2.Callback<Map<String, Object>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        bindRealProductData(response.body());
                    } else {
                        loadMockData();
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                    loadMockData();
                }
            });
        } else {
            // Fallback load default product list if no ID is passed
            ApiClient.getInstance(this).getApiService().getProducts().enqueue(new retrofit2.Callback<Map<String, Object>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            Map<String, Object> body = response.body();
                            List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                            if (items != null && !items.isEmpty()) {
                                bindRealProductData(items.get(0));
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    loadMockData();
                }

                @Override
                public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                    loadMockData();
                }
            });
        }
    }

    private void bindRealProductData(Map<String, Object> productData) {
        String name = (String) productData.get("name");
        String desc = (String) productData.get("description");
        if (desc == null || desc.trim().isEmpty()) {
            desc = "Laptop Gaming thế hệ mới với hiệu năng cực đỉnh, được trang bị card đồ họa Blackwell tiên tiến và vi xử lý AI mạnh mẽ giúp bạn thống trị mọi chiến trường AAA.";
        }

        // Save to viewed products
        saveToViewedProducts(productData);

        // Format Prices
        Double priceVal = 0.0;
        Double oldPriceVal = 0.0;
        try {
            if (productData.get("price") != null) {
                priceVal = ((Number) productData.get("price")).doubleValue();
            }
            if (productData.get("old_price") != null) {
                oldPriceVal = ((Number) productData.get("old_price")).doubleValue();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        String curPriceStr = formatCurrency(priceVal);
        String oldPriceStr = formatCurrency(oldPriceVal);
        int discountPct = 0;
        if (oldPriceVal > 0) {
            discountPct = (int) Math.round((1.0 - (priceVal / oldPriceVal)) * 100);
        }

        List<String> imageUrls = (List<String>) productData.get("images");
        if (imageUrls == null) {
            imageUrls = new ArrayList<>();
        }

        // Bind core data to views
        if (productName != null) productName.setText(name);
        if (productDescription != null) productDescription.setText(desc);
        if (currentPrice != null) currentPrice.setText(curPriceStr);
        if (oldPrice != null) oldPrice.setText(oldPriceStr);
        if (discountBadge != null) {
            if (discountPct > 0) {
                discountBadge.setVisibility(android.view.View.VISIBLE);
                discountBadge.setText("-" + discountPct + "%");
            } else {
                discountBadge.setVisibility(android.view.View.GONE);
            }
        }
        if (ratingText != null) ratingText.setText("4.9");
        if (soldCount != null) soldCount.setText(getString(R.string.sold_count_format, "850"));
        if (reviewCount != null) reviewCount.setText(getString(R.string.reviews_count_format, 124));

        // Load main image with Glide
        if (mainProductImage != null && !imageUrls.isEmpty()) {
            Glide.with(this)
                    .load(imageUrls.get(0))
                    .placeholder(R.drawable.aura_laptop)
                    .into(mainProductImage);
                    
            if (descriptionImage != null) {
                Glide.with(this)
                        .load(imageUrls.get(0))
                        .placeholder(R.drawable.product_case)
                        .into(descriptionImage);
            }
        } else if (mainProductImage != null) {
            mainProductImage.setImageResource(R.drawable.aura_laptop);
            if (descriptionImage != null) {
                descriptionImage.setImageResource(R.drawable.product_case);
            }
        }

        // Setup horizontal thumbnail carousel
        if (thumbnailRecyclerView != null) {
            thumbnailRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            ProductImageAdapter imageAdapter = new ProductImageAdapter(imageUrls);
            imageAdapter.setOnImageClickListener(imageUrl -> {
                if (mainProductImage != null) {
                    mainProductImage.setAlpha(0f);
                    Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.aura_laptop)
                            .into(mainProductImage);
                    mainProductImage.animate().alpha(1f).setDuration(300).start();
                }
            });
            thumbnailRecyclerView.setAdapter(imageAdapter);
        }

        // Setup specifications from backend dynamically
        if (specsRecyclerView != null) {
            List<ProductSpec> specList = new ArrayList<>();
            Map<String, String> specsMap = null;
            try {
                specsMap = (Map<String, String>) productData.get("specs");
            } catch (Exception ignored) {}
            
            if (specsMap != null) {
                for (Map.Entry<String, String> entry : specsMap.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue();
                    if ("Thông số".equalsIgnoreCase(key) || "Chi tiết".equalsIgnoreCase(key)) {
                        continue;
                    }
                    int icon = R.drawable.ic_cpu; // Default
                    String keyLower = key.toLowerCase();
                    if (keyLower.contains("cpu") || keyLower.contains("vi xử lý")) {
                        icon = R.drawable.ic_cpu;
                    } else if (keyLower.contains("gpu") || keyLower.contains("card") || keyLower.contains("đồ họa") || keyLower.contains("vga")) {
                        icon = R.drawable.ic_gpu;
                    } else if (keyLower.contains("ram") || keyLower.contains("bộ nhớ")) {
                        icon = R.drawable.ic_ram;
                    } else if (keyLower.contains("ssd") || keyLower.contains("ổ cứng") || keyLower.contains("lưu trữ") || keyLower.contains("rom")) {
                        icon = R.drawable.ic_ssd;
                    }
                    specList.add(new ProductSpec(key, val, icon));
                }
            }

            if (specList.isEmpty()) {
                specList = MockData.getDetailProduct().getSpecs();
            }

            specsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            specsRecyclerView.setAdapter(new SpecAdapter(specList));
        }

        String categorySlug = null;
        try {
            Map<String, Object> categoryMap = (Map<String, Object>) productData.get("category");
            if (categoryMap != null) {
                categorySlug = (String) categoryMap.get("slug");
            }
        } catch (Exception ignored) {}

        loadRelatedProducts(categorySlug);
        loadViewedProducts();
    }

    private void loadRelatedProducts(String categorySlug) {
        if (relatedRecyclerView != null) {
            ApiClient.getInstance(this).getApiService().getProductsFiltered(1, 10, categorySlug, null, null, null, null, null, null, null).enqueue(new retrofit2.Callback<Map<String, Object>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) response.body().get("items");
                        if (items != null) {
                            relatedRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this, LinearLayoutManager.HORIZONTAL, false));
                            RelatedProductAdapter adapter = new RelatedProductAdapter(items);
                            adapter.setOnProductClickListener(MainActivity.this::openProductDetail);
                            relatedRecyclerView.setAdapter(adapter);
                        }
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {}
            });
        }
    }

    private void saveToViewedProducts(Map<String, Object> product) {
        SharedPreferences prefs = getSharedPreferences("aura_prefs", MODE_PRIVATE);
        String json = prefs.getString("viewed_products", "[]");
        Gson gson = new Gson();
        Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> viewedList = gson.fromJson(json, type);
        if (viewedList == null) viewedList = new ArrayList<>();

        // Remove if already exists to put it at the top
        String newId = (String) product.get("_id");
        viewedList.removeIf(p -> {
            String id = (String) p.get("_id");
            return id != null && id.equals(newId);
        });

        viewedList.add(0, product);
        if (viewedList.size() > 10) {
            viewedList = viewedList.subList(0, 10);
        }

        prefs.edit().putString("viewed_products", gson.toJson(viewedList)).apply();
    }

    private void loadViewedProducts() {
        if (viewedRecyclerView != null) {
            SharedPreferences prefs = getSharedPreferences("aura_prefs", MODE_PRIVATE);
            String json = prefs.getString("viewed_products", "[]");
            Gson gson = new Gson();
            Type type = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> viewedList = gson.fromJson(json, type);

            if (viewedList != null && !viewedList.isEmpty()) {
                viewedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
                ViewedProductAdapter adapter = new ViewedProductAdapter(viewedList);
                adapter.setOnProductClickListener(this::openProductDetail);
                viewedRecyclerView.setAdapter(adapter);
            }
        }
    }

    private void openProductDetail(Map<String, Object> product) {
        Intent intent = new Intent(this, MainActivity.class);
        String productId = (String) product.get("_id");
        intent.putExtra("product_id", productId);
        startActivity(intent);
    }

    private String formatCurrency(double amount) {
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
        return formatter.format(amount).replace(",", ".") + "đ";
    }

    private void loadMockData() {
        Product product = MockData.getDetailProduct();
        if (productName != null) productName.setText(product.getName());
        if (productDescription != null) productDescription.setText(product.getDescription());
        if (currentPrice != null) currentPrice.setText(product.getCurrentPrice());
        if (oldPrice != null) oldPrice.setText(product.getOldPrice());
        if (discountBadge != null) discountBadge.setText(product.getDiscount());
        if (ratingText != null) ratingText.setText(String.valueOf(product.getRating()));
        if (soldCount != null) soldCount.setText(getString(R.string.sold_count_format, formatSoldCount(product.getSoldCount())));
        if (reviewCount != null) reviewCount.setText(getString(R.string.reviews_count_format, product.getReviewCount()));

        if (mainProductImage != null && product.getImages() != null && !product.getImages().isEmpty()) {
            mainProductImage.setImageResource(product.getImages().get(0));
        }

        if (thumbnailRecyclerView != null) {
            thumbnailRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            List<String> mockUrls = new ArrayList<>();
            for (Integer resId : product.getImages()) {
                mockUrls.add("android.resource://" + getPackageName() + "/" + resId);
            }
            ProductImageAdapter imageAdapter = new ProductImageAdapter(mockUrls);
            imageAdapter.setOnImageClickListener(imageUrl -> {
                if (mainProductImage != null) {
                    mainProductImage.setAlpha(0f);
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.aura_laptop).into(mainProductImage);
                    mainProductImage.animate().alpha(1f).setDuration(300).start();
                }
            });
            thumbnailRecyclerView.setAdapter(imageAdapter);
        }

        if (specsRecyclerView != null) {
            specsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            specsRecyclerView.setAdapter(new SpecAdapter(product.getSpecs()));
        }
    }

    private String formatSoldCount(int count) {
        if (count >= 1000) {
            return String.format(Locale.US, "%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
