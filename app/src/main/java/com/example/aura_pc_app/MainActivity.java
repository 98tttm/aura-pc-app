package com.example.aura_pc_app;

import android.content.Context;
import android.content.Intent;
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

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView thumbnailRecyclerView, specsRecyclerView, relatedRecyclerView, viewedRecyclerView;
    private TextView productName, ratingText, soldCount, reviewCount, currentPrice, oldPrice, discountBadge, productDescription, specsTitle;
    private ImageView mainProductImage, btnFavorite, descProductImage;
    private android.view.View btnConsult, btnAddToCart, btnBuyNow, viewedSection;
    private boolean isFavorite = false;

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
        btnFavorite = findViewById(R.id.btnFavorite);

        // Find views inside included overlays to avoid ID overrides returning null
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

        // navOverlay overrides the root ID of view_bottom_navigation
        android.view.View bottomNavCard = findViewById(R.id.navOverlay);
        if (bottomNavCard == null) {
            bottomNavCard = findViewById(R.id.bottomNavCard);
        }

        thumbnailRecyclerView = findViewById(R.id.thumbnailRecyclerView);
        specsRecyclerView = findViewById(R.id.specsRecyclerView);
        relatedRecyclerView = findViewById(R.id.relatedRecyclerView);
        viewedRecyclerView = findViewById(R.id.viewedRecyclerView);
        descProductImage = findViewById(R.id.descProductImage);
        viewedSection = findViewById(R.id.viewedSection);
        specsTitle = findViewById(R.id.specsTitle);

        // Apply strikethrough to the old price with safety check
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
        String productId = getIntent().getStringExtra("product_id");

        if (productId != null && !productId.isEmpty()) {
            // Load specific product by ID
            ApiClient.getInstance(this).getApiService().getProductById(productId).enqueue(new retrofit2.Callback<Map<String, Object>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            bindRealProductData(response.body());
                            return;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    loadMockData();
                }

                @Override
                public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {
                    t.printStackTrace();
                    loadMockData();
                }
            });
        } else {
            // Fallback: load first product from all products (Old behavior)
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
                    t.printStackTrace();
                    loadMockData();
                }
            });
        }
    }

    private void bindRealProductData(Map<String, Object> productData) {
        String name = (String) productData.get("name");
        String desc = (String) productData.get("description");
        if (desc == null || desc.trim().isEmpty()) {
            desc = "Sản phẩm " + name + " mang đến trải nghiệm tuyệt vời với hiệu năng mạnh mẽ. Được thiết kế tinh tế và trang bị công nghệ tiên tiến nhất, đáp ứng hoàn hảo mọi nhu cầu của bạn.";
        }

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
            discountBadge.setText("-" + discountPct + "%");
        }
        if (ratingText != null) ratingText.setText("4.9");
        if (soldCount != null) soldCount.setText(getString(R.string.sold_count_format, "850"));
        if (reviewCount != null) reviewCount.setText(getString(R.string.reviews_count_format, 124));

        // Load main image with Glide
        if (mainProductImage != null && !imageUrls.isEmpty()) {
            Glide.with(this)
                    .load(imageUrls.get(0))
                    .placeholder(R.drawable.product_case)
                    .into(mainProductImage);
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
                            .placeholder(R.drawable.product_case)
                            .into(mainProductImage);
                    mainProductImage.animate().alpha(1f).setDuration(300).start();
                }
            });
            thumbnailRecyclerView.setAdapter(imageAdapter);
        }

        // Setup specifications from backend dynamically
        if (specsRecyclerView != null) {
            List<ProductSpec> specList = new ArrayList<>();
            Map<String, String> specsMap = (Map<String, String>) productData.get("specs");
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
                if (specsTitle != null) specsTitle.setVisibility(android.view.View.GONE);
                specsRecyclerView.setVisibility(android.view.View.GONE);
            } else {
                if (specsTitle != null) specsTitle.setVisibility(android.view.View.VISIBLE);
                specsRecyclerView.setVisibility(android.view.View.VISIBLE);
                specsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
                specsRecyclerView.setAdapter(new SpecAdapter(specList));
            }
        }

        // Load descProductImage with Glide if available
        if (descProductImage != null && !imageUrls.isEmpty()) {
            Glide.with(this)
                    .load(imageUrls.get(0))
                    .placeholder(R.drawable.product_case)
                    .into(descProductImage);
        }

        // Setup related products dynamically from API by category
        if (relatedRecyclerView != null) {
            relatedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            
            String categorySlug = "";
            Object catObj = productData.get("category");
            if (catObj instanceof Map) {
                Object slugObj = ((Map<?, ?>) catObj).get("slug");
                if (slugObj instanceof String) categorySlug = (String) slugObj;
            }
            if (categorySlug.isEmpty()) {
                Object catIdObj = productData.get("category_id");
                if (catIdObj instanceof String) categorySlug = (String) catIdObj;
            }
            
            if (categorySlug.isEmpty()) categorySlug = "pc"; // fallback
            
            // Gọi API lấy danh sách sản phẩm cùng danh mục
            ApiClient.getInstance(this).getApiService().getProductsByCategory(categorySlug, 1, 15).enqueue(new retrofit2.Callback<Map<String, Object>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            List<Map<String, Object>> items = (List<Map<String, Object>>) response.body().get("items");
                            if (items != null && items.size() > 0) {
                                // Trộn (shuffle) để hiện ngẫu nhiên
                                java.util.Collections.shuffle(items);
                                // Lấy tối đa 5 sản phẩm
                                List<Map<String, Object>> relatedItems = items.subList(0, Math.min(items.size(), 5));
                                RelatedProductAdapter adapter = new RelatedProductAdapter(relatedItems);
                                adapter.setOnProductClickListener(p -> openProductDetail(p));
                                relatedRecyclerView.setAdapter(adapter);
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                }
                @Override
                public void onFailure(retrofit2.Call<Map<String, Object>> call, Throwable t) {}
            });
        }
        
        // Save current product to viewed history and load the viewed section
        saveViewedProduct(productData);
        loadViewedProducts((String) productData.get("_id"));
    }

    private void saveViewedProduct(Map<String, Object> productData) {
        if (productData == null || productData.get("_id") == null) return;
        
        android.content.SharedPreferences prefs = getSharedPreferences("aura_prefs", Context.MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = prefs.getString("viewed_products", "[]");
        
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> viewedList = gson.fromJson(json, type);
        if (viewedList == null) viewedList = new ArrayList<>();
        
        String currentId = (String) productData.get("_id");
        
        // Remove if already exists to move it to the front
        for (int i = 0; i < viewedList.size(); i++) {
            if (currentId.equals(viewedList.get(i).get("_id"))) {
                viewedList.remove(i);
                break;
            }
        }
        
        viewedList.add(0, productData);
        if (viewedList.size() > 10) {
            viewedList = viewedList.subList(0, 10);
        }
        
        prefs.edit().putString("viewed_products", gson.toJson(viewedList)).apply();
    }
    
    private void loadViewedProducts(String currentId) {
        if (viewedRecyclerView == null || viewedSection == null) return;
        
        android.content.SharedPreferences prefs = getSharedPreferences("aura_prefs", Context.MODE_PRIVATE);
        com.google.gson.Gson gson = new com.google.gson.Gson();
        String json = prefs.getString("viewed_products", "[]");
        
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<List<Map<String, Object>>>(){}.getType();
        List<Map<String, Object>> viewedList = gson.fromJson(json, type);
        
        if (viewedList == null) viewedList = new ArrayList<>();
        
        // Filter out current product so we don't show the one being viewed right now
        List<Map<String, Object>> displayList = new ArrayList<>();
        for (Map<String, Object> item : viewedList) {
            if (!item.get("_id").equals(currentId)) {
                displayList.add(item);
            }
        }
        
        if (displayList.isEmpty()) {
            viewedSection.setVisibility(android.view.View.GONE);
        } else {
            viewedSection.setVisibility(android.view.View.VISIBLE);
            viewedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            ViewedProductAdapter adapter = new ViewedProductAdapter(displayList);
            adapter.setOnProductClickListener(p -> openProductDetail(p));
            viewedRecyclerView.setAdapter(adapter);
        }
    }

    private void openProductDetail(Map<String, Object> product) {
        android.content.Intent intent = new android.content.Intent(this, MainActivity.class);
        String productId = "";
        Object idObj = product.get("_id");
        if (idObj instanceof String) {
            productId = (String) idObj;
        } else {
            Object prodIdObj = product.get("product_id");
            if (prodIdObj instanceof String) {
                productId = (String) prodIdObj;
            }
        }
        intent.putExtra("product_id", productId);
        
        Object nameObj = product.get("name");
        if (nameObj instanceof String) {
            intent.putExtra("product_name", (String) nameObj);
        }
        
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
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.product_case).into(mainProductImage);
                    mainProductImage.animate().alpha(1f).setDuration(300).start();
                }
            });
            thumbnailRecyclerView.setAdapter(imageAdapter);
        }

        if (specsRecyclerView != null) {
            specsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
            specsRecyclerView.setAdapter(new SpecAdapter(product.getSpecs()));
        }

        if (relatedRecyclerView != null) {
            relatedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            
            // Convert MockData Product objects to Map<String, Object> for the updated adapter
            List<Map<String, Object>> mockRelatedMaps = new ArrayList<>();
            for (Product p : MockData.getRelatedProducts()) {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("name", p.getName());
                try {
                    String priceStr = p.getCurrentPrice().replace(".", "").replace("đ", "").trim();
                    map.put("price", Double.parseDouble(priceStr));
                } catch (Exception e) {
                    map.put("price", 0.0);
                }
                List<String> imgs = new ArrayList<>();
                if (p.getImages() != null && !p.getImages().isEmpty()) {
                    imgs.add("android.resource://" + getPackageName() + "/" + p.getImages().get(0));
                }
                map.put("images", imgs);
                mockRelatedMaps.add(map);
            }
            RelatedProductAdapter adapter = new RelatedProductAdapter(mockRelatedMaps);
            adapter.setOnProductClickListener(p -> openProductDetail(p));
            relatedRecyclerView.setAdapter(adapter);
        }

        if (viewedSection != null) {
            viewedSection.setVisibility(android.view.View.GONE); // Hide viewed section in mock mode
        }
    }

    private String formatSoldCount(int count) {
        if (count >= 1000) {
            return String.format(Locale.US, "%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
