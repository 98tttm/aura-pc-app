package com.example.aura_pc_app;

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

import com.example.aura_pc_app.adapter.ProductImageAdapter;
import com.example.aura_pc_app.adapter.RelatedProductAdapter;
import com.example.aura_pc_app.adapter.SpecAdapter;
import com.example.aura_pc_app.adapter.ViewedProductAdapter;
import com.example.aura_pc_app.domain.repository.mock.MockData;
import com.example.aura_pc_app.domain.model.Product;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView thumbnailRecyclerView, specsRecyclerView, relatedRecyclerView, viewedRecyclerView;
    private TextView productName, ratingText, soldCount, reviewCount, currentPrice, oldPrice, discountBadge, productDescription;
    private ImageView mainProductImage, btnFavorite;
    private android.view.View btnConsult, btnAddToCart, btnBuyNow, bottomNavCard;
    private boolean isFavorite = false;

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
        loadData();
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
        btnConsult = findViewById(R.id.btnConsult);
        btnAddToCart = findViewById(R.id.btnAddToCart);
        btnBuyNow = findViewById(R.id.btnBuyNow);
        bottomNavCard = findViewById(R.id.bottomNavCard);

        thumbnailRecyclerView = findViewById(R.id.thumbnailRecyclerView);
        specsRecyclerView = findViewById(R.id.specsRecyclerView);
        relatedRecyclerView = findViewById(R.id.relatedRecyclerView);
        viewedRecyclerView = findViewById(R.id.viewedRecyclerView);

        // Apply strikethrough to the old price
        oldPrice.setPaintFlags(oldPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

        btnFavorite.setOnClickListener(v -> {
            isFavorite = !isFavorite;
            if (isFavorite) {
                btnFavorite.setColorFilter(getResources().getColor(R.color.orange_primary, getTheme()));
            } else {
                btnFavorite.setColorFilter(getResources().getColor(R.color.gray_text, getTheme()));
            }
        });

        if (btnConsult != null) {
            btnConsult.setOnClickListener(v -> {
                android.widget.Toast.makeText(this, "Đang kết nối với tư vấn viên...", android.widget.Toast.LENGTH_SHORT).show();
            });
        }

        btnAddToCart.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Đã thêm vào giỏ hàng", android.widget.Toast.LENGTH_SHORT).show();
        });

        btnBuyNow.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Đang chuyển đến trang thanh toán", android.widget.Toast.LENGTH_SHORT).show();
        });

        bottomNavCard.setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Chức năng thanh điều hướng đang phát triển", android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void loadData() {
        // Load main product data from mock
        Product product = MockData.getDetailProduct();

        // Bind data to views
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            mainProductImage.setImageResource(product.getImages().get(0));
        }
        productName.setText(product.getName());
        ratingText.setText(String.valueOf(product.getRating()));
        soldCount.setText(getString(R.string.sold_count_format, formatSoldCount(product.getSoldCount())));
        reviewCount.setText(getString(R.string.reviews_count_format, product.getReviewCount()));
        currentPrice.setText(product.getCurrentPrice());
        oldPrice.setText(product.getOldPrice());
        discountBadge.setText(product.getDiscount());
        productDescription.setText(product.getDescription());

        // Setup Thumbnail RecyclerView (Horizontal)
        thumbnailRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        ProductImageAdapter imageAdapter = new ProductImageAdapter(product.getImages());
        imageAdapter.setOnImageClickListener(imageRes -> {
            mainProductImage.setAlpha(0f);
            mainProductImage.setImageResource(imageRes);
            mainProductImage.animate().alpha(1f).setDuration(300).start();
        });
        thumbnailRecyclerView.setAdapter(imageAdapter);

        // Setup Technical Specs RecyclerView (Grid 2 columns)
        specsRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        SpecAdapter specAdapter = new SpecAdapter(product.getSpecs());
        specsRecyclerView.setAdapter(specAdapter);

        // Setup Related Products
        relatedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        relatedRecyclerView.setAdapter(new RelatedProductAdapter(MockData.getRelatedProducts()));

        // Setup Viewed Products
        viewedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        viewedRecyclerView.setAdapter(new ViewedProductAdapter(MockData.getViewedProducts()));
    }

    private String formatSoldCount(int count) {
        if (count >= 1000) {
            return String.format(Locale.US, "%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
