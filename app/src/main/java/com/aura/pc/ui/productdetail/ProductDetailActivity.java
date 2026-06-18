package com.aura.pc.ui.productdetail;

import com.example.aura_pc_app.R;
import com.example.aura_pc_app.App;import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.HtmlCompat;
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
import java.util.concurrent.Executors;

import com.example.aura_pc_app.data.db.AppDatabase;
import com.example.aura_pc_app.data.db.entity.WishlistEntity;

public class ProductDetailActivity extends AppCompatActivity {

    private RecyclerView thumbnailRecyclerView, specsRecyclerView, relatedRecyclerView, viewedRecyclerView;
    private TextView productName, ratingText, soldCount, reviewCount, currentPrice, oldPrice, discountBadge, productDescription, specsTitle, descriptionTitle;
    private WebView productDescriptionWebView;
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
        productDescriptionWebView = findViewById(R.id.productDescriptionWebView);
        specsTitle = findViewById(R.id.specsTitle);
        descriptionTitle = findViewById(R.id.descriptionTitle);
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
            // Will be wired up after product data loads (see setupWishlistToggle)
        }

        if (btnConsult != null) {
            btnConsult.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, getString(R.string.toast_connecting_consultant), android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (btnAddToCart != null) {
            btnAddToCart.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, getString(R.string.toast_added_to_cart), android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (btnBuyNow != null) {
            btnBuyNow.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, getString(R.string.toast_navigating_checkout), android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        if (bottomNavCard != null) {
            bottomNavCard.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, getString(R.string.toast_nav_developing), android.widget.Toast.LENGTH_SHORT).show();
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
                android.widget.Toast.makeText(this, getString(R.string.toast_added_to_cart), android.widget.Toast.LENGTH_SHORT).show();
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
        String descHtml = (String) productData.get("description_html");
        String desc = (String) productData.get("description");

        if (descHtml != null && !descHtml.trim().isEmpty()) {
            // Xóa bảng thông số kỹ thuật dư thừa (nếu có)
            // Fix: Only remove table if there's enough other text (so we don't delete the whole description)
            String withoutTable = stripTableTags(descHtml);
            if (withoutTable.replaceAll("<[^>]*>", "").trim().length() > 50) {
                descHtml = withoutTable;
            }
            
            // Xóa tiêu đề "THÔNG SỐ KĨ THUẬT" nếu nó nằm độc lập ngoài bảng
            descHtml = descHtml.replaceAll("(?i)<h2>[^<]*?thông số[^<]*?(kĩ|kỹ)[^<]*?thuật[^<]*?</h2>", "");
            descHtml = descHtml.replaceAll("(?i)<strong>[^<]*?thông số[^<]*?(kĩ|kỹ)[^<]*?thuật[^<]*?</strong>", "");
            
            // Sửa lỗi url ảnh bị thiếu https:// (xử lý mọi trường hợp src="//, src = '//, v.v...)
            descHtml = descHtml.replaceAll("(?i)src\\s*=\\s*\"//", "src=\"https://");
            descHtml = descHtml.replaceAll("(?i)src\\s*=\\s*'//", "src='https://");
            descHtml = descHtml.replaceAll("(?i)src\\s*=\\s*\"http://", "src=\"https://");
            descHtml = descHtml.replaceAll("(?i)src\\s*=\\s*'http://", "src='https://");

            if (productDescriptionWebView != null) {
                productDescriptionWebView.setVisibility(android.view.View.GONE);
            }
            if (productDescription != null) {
                productDescription.setVisibility(android.view.View.VISIBLE);
                productDescription.setText(android.text.Html.fromHtml(
                        descHtml,
                        android.text.Html.FROM_HTML_MODE_COMPACT,
                        new com.example.aura_pc_app.utils.GlideImageGetter(this, productDescription),
                        null
                ));
            }
        } else {
            if (productDescriptionWebView != null) {
                productDescriptionWebView.setVisibility(android.view.View.GONE);
            }
            if (productDescription != null) {
                productDescription.setVisibility(android.view.View.VISIBLE);
                if (desc != null && !desc.trim().isEmpty()) {
                    productDescription.setText(desc);
                } else {
                    productDescription.setText("Chưa có thông tin mô tả sản phẩm.");
                }
            }
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
        if (ratingText != null) ratingText.setText(getString(R.string.label_default_rating));
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
                // If no specs available, hide the specs section entirely
                if (specsTitle != null) specsTitle.setVisibility(android.view.View.GONE);
                if (specsRecyclerView != null) specsRecyclerView.setVisibility(android.view.View.GONE);
            } else {
                if (specsTitle != null) specsTitle.setVisibility(android.view.View.VISIBLE);
                if (specsRecyclerView != null) specsRecyclerView.setVisibility(android.view.View.VISIBLE);
                specsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
                specsRecyclerView.setAdapter(new SpecAdapter(specList));
            }
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

        // Setup wishlist toggle for product detail
        setupWishlistToggle(productData);
    }

    private void setupWishlistToggle(Map<String, Object> productData) {
        if (btnFavorite == null || currentProductId == null) return;

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean fav = AppDatabase.getInstance(this).wishlistDao().isFavorite(currentProductId);
            runOnUiThread(() -> {
                isFavorite = fav;
                btnFavorite.setImageResource(fav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                btnFavorite.setColorFilter(getResources().getColor(
                        fav ? R.color.orange_primary : R.color.gray_text, getTheme()));
            });
        });

        btnFavorite.setOnClickListener(v -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                if (isFavorite) {
                    AppDatabase.getInstance(this).wishlistDao().deleteByProductId(currentProductId);
                } else {
                    String n = (String) productData.get("name");
                    double p = 0, op = 0;
                    try { p = ((Number) productData.get("price")).doubleValue(); } catch (Exception ignored) {}
                    try { op = ((Number) productData.get("old_price")).doubleValue(); } catch (Exception ignored) {}
                    String img = "";
                    try {
                        List<String> imgs = (List<String>) productData.get("images");
                        if (imgs != null && !imgs.isEmpty()) img = imgs.get(0);
                    } catch (Exception ignored) {}
                    AppDatabase.getInstance(this).wishlistDao().insert(
                            new WishlistEntity(currentProductId, n, p, op, img));
                }
                isFavorite = !isFavorite;
                runOnUiThread(() -> {
                    btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                    btnFavorite.setColorFilter(getResources().getColor(
                            isFavorite ? R.color.orange_primary : R.color.gray_text, getTheme()));
                    android.widget.Toast.makeText(this,
                            isFavorite ? getString(R.string.wishlist_added) : getString(R.string.wishlist_removed),
                            android.widget.Toast.LENGTH_SHORT).show();
                });
            });
        });
    }

    private void loadRelatedProducts(String categorySlug) {
        if (relatedRecyclerView != null) {
            ApiClient.getInstance(this).getApiService().getProductsFiltered(1, 10, categorySlug, null, null, null, null, null, null, null).enqueue(new retrofit2.Callback<Map<String, Object>>() {
                @Override
                public void onResponse(retrofit2.Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) response.body().get("items");
                        if (items != null) {
                            relatedRecyclerView.setLayoutManager(new LinearLayoutManager(ProductDetailActivity.this, LinearLayoutManager.HORIZONTAL, false));
                            RelatedProductAdapter adapter = new RelatedProductAdapter(items);
                            adapter.setOnProductClickListener(ProductDetailActivity.this::openProductDetail);
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
        Intent intent = new Intent(this, ProductDetailActivity.class);
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

    private static String stripTableTags(String html) {
        if (html == null) return "";
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        while (true) {
            int tableStart = html.toLowerCase(Locale.ROOT).indexOf("<table", lastIndex);
            if (tableStart == -1) {
                sb.append(html.substring(lastIndex));
                break;
            }
            sb.append(html.substring(lastIndex, tableStart));
            int tableEnd = html.toLowerCase(Locale.ROOT).indexOf("</table>", tableStart);
            if (tableEnd == -1) {
                break;
            }
            lastIndex = tableEnd + "</table>".length();
        }
        return sb.toString();
    }
}
