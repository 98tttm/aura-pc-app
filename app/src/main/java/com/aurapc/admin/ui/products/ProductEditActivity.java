package com.aurapc.admin.ui.products;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aurapc.admin.R;
import com.aurapc.admin.data.api.ApiClient;
import com.aurapc.admin.data.api.Resource;
import com.aurapc.admin.data.model.Category;
import com.aurapc.admin.data.model.Product;
import com.aurapc.admin.di.ServiceLocator;
import com.aurapc.admin.utils.Formatters;
import com.aurapc.admin.utils.NetworkHelper;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class ProductEditActivity extends AppCompatActivity {

    public static final String EXTRA_PRODUCT_ID = "product_id";

    private final List<Category> categories = new ArrayList<>();
    private TextInputEditText etName, etSlug, etBrand, etShortDescription, etDescription;
    private TextInputEditText etPrice, etSalePrice, etStock, etImages;
    private Spinner spCategory;
    private MaterialSwitch swActive, swFeatured;
    private ProgressBar progress;
    private ScrollView scroll;
    private ImageView ivPreview;
    private TextView tvSummaryPrice, tvSummaryOriginal, tvSummaryCategory, tvSummaryStock;
    private TextView tvSummaryBrand, tvSummarySlug, tvImageCount;
    private MaterialButton btnSave;
    private String productId;
    private String pendingCategoryKey;
    private boolean bindingProduct;

    public static void start(Context context, @Nullable String productId) {
        Intent intent = new Intent(context, ProductEditActivity.class);
        if (productId != null) intent.putExtra(EXTRA_PRODUCT_ID, productId);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_edit);

        productId = getIntent().getStringExtra(EXTRA_PRODUCT_ID);
        bindViews();
        setupToolbar();
        setupListeners();
        setupCategorySpinner();
        loadCategories();
        if (productId != null) loadProduct();
        updateSummary();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTag("toolbar");
        scroll = findViewById(R.id.scroll);
        progress = findViewById(R.id.progress);
        etName = findViewById(R.id.etName);
        etSlug = findViewById(R.id.etSlug);
        etBrand = findViewById(R.id.etBrand);
        etShortDescription = findViewById(R.id.etShortDescription);
        etDescription = findViewById(R.id.etDescription);
        etPrice = findViewById(R.id.etPrice);
        etSalePrice = findViewById(R.id.etSalePrice);
        etStock = findViewById(R.id.etStock);
        etImages = findViewById(R.id.etImages);
        spCategory = findViewById(R.id.spCategory);
        swActive = findViewById(R.id.swActive);
        swFeatured = findViewById(R.id.swFeatured);
        ivPreview = findViewById(R.id.ivPreview);
        tvSummaryPrice = findViewById(R.id.tvSummaryPrice);
        tvSummaryOriginal = findViewById(R.id.tvSummaryOriginal);
        tvSummaryCategory = findViewById(R.id.tvSummaryCategory);
        tvSummaryStock = findViewById(R.id.tvSummaryStock);
        tvSummaryBrand = findViewById(R.id.tvSummaryBrand);
        tvSummarySlug = findViewById(R.id.tvSummarySlug);
        tvImageCount = findViewById(R.id.tvImageCount);
        btnSave = findViewById(R.id.btnSave);
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(productId == null ? "Them san pham" : "Sua san pham");
        toolbar.setNavigationOnClickListener(v -> finish());
        btnSave.setText(productId == null ? "Tao san pham" : "Luu thay doi");
    }

    private void setupListeners() {
        SummaryWatcher watcher = new SummaryWatcher();
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!bindingProduct && textOf(etSlug).isEmpty()) {
                    etSlug.setText(slugify(s.toString()));
                }
                updateSummary();
            }
        });
        etSlug.addTextChangedListener(watcher);
        etBrand.addTextChangedListener(watcher);
        etPrice.addTextChangedListener(watcher);
        etSalePrice.addTextChangedListener(watcher);
        etStock.addTextChangedListener(watcher);
        etImages.addTextChangedListener(watcher);
        swActive.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
        swFeatured.setOnCheckedChangeListener((buttonView, isChecked) -> updateSummary());
        btnSave.setOnClickListener(v -> submit());
    }

    private void setupCategorySpinner() {
        spCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSummary();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {
                updateSummary();
            }
        });
    }

    private void loadCategories() {
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.contentApi().listCategories(), (Resource<List<Category>> result) -> {
            if (result != null && result.isSuccess() && result.data != null) {
                categories.clear();
                categories.addAll(result.data);
                bindCategories();
            }
        });
    }

    private void bindCategories() {
        List<String> labels = new ArrayList<>();
        labels.add("-- Chon danh muc --");
        for (Category c : categories) {
            String name = c.name != null ? c.name : categoryKey(c);
            labels.add(name != null ? name : "Danh muc");
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);
        selectPendingCategory();
    }

    private void loadProduct() {
        setLoading(true);
        ApiClient api = ServiceLocator.get().apiClient();
        NetworkHelper.toLiveData(api.productApi().getProduct(productId), (Resource<Product> result) -> {
            setLoading(false);
            if (result != null && result.isSuccess() && result.data != null) {
                bindProduct(result.data);
            } else {
                Toast.makeText(this, result != null && result.message != null ? result.message : getString(R.string.error_generic), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void bindProduct(Product p) {
        bindingProduct = true;
        etName.setText(value(p.name));
        etSlug.setText(value(p.slug));
        etBrand.setText(value(p.brand));
        etShortDescription.setText(value(p.shortDescription));
        etDescription.setText(value(p.description));
        etPrice.setText(numberText(p.price));
        etSalePrice.setText(p.salePrice != null && p.salePrice > 0 ? numberText(p.salePrice) : "");
        etStock.setText(String.valueOf(p.stock != null ? p.stock : 0));
        swActive.setChecked(p.active());
        swFeatured.setChecked((p.isFeatured != null && p.isFeatured) || (p.featured != null && p.featured));
        List<String> imageUrls = p.imageUrls();
        if (!imageUrls.isEmpty()) {
            etImages.setText(String.join("\n", imageUrls));
        } else {
            etImages.setText(value(p.thumbnail));
        }
        pendingCategoryKey = categoryKeyFromProduct(p);
        selectPendingCategory();
        bindingProduct = false;
        updateSummary();
    }

    private void selectPendingCategory() {
        if (pendingCategoryKey == null || categories.isEmpty()) return;
        for (int i = 0; i < categories.size(); i++) {
            if (pendingCategoryKey.equals(categoryKey(categories.get(i)))) {
                spCategory.setSelection(i + 1);
                return;
            }
        }
    }

    private void submit() {
        String name = textOf(etName);
        double price = parseDouble(textOf(etPrice));
        if (name.isEmpty()) {
            etName.setError("Bat buoc");
            etName.requestFocus();
            return;
        }
        if (price <= 0) {
            etPrice.setError("Gia phai lon hon 0");
            etPrice.requestFocus();
            return;
        }
        double salePrice = parseDouble(textOf(etSalePrice));
        if (salePrice > 0 && salePrice >= price) {
            etSalePrice.setError("Gia khuyen mai phai nho hon gia goc");
            etSalePrice.requestFocus();
            return;
        }

        Product body = new Product();
        body.name = name;
        body.slug = textOf(etSlug).isEmpty() ? slugify(name) : slugify(textOf(etSlug));
        body.brand = textOf(etBrand);
        body.shortDescription = textOf(etShortDescription);
        body.description = textOf(etDescription);
        body.price = price;
        body.salePrice = salePrice > 0 ? salePrice : null;
        body.stock = Math.max(0, parseInt(textOf(etStock)));
        body.active = swActive.isChecked();
        body.featured = swFeatured.isChecked();
        body.isActive = swActive.isChecked();
        body.isFeatured = swFeatured.isChecked();
        List<String> imageUrls = parseImages();
        body.images = new ArrayList<>(imageUrls);
        body.thumbnail = imageUrls.isEmpty() ? null : imageUrls.get(0);
        applySelectedCategory(body);

        setLoading(true);
        ApiClient api = ServiceLocator.get().apiClient();
        if (productId == null) {
            NetworkHelper.toLiveData(api.productApi().createProduct(body), this::handleSubmitResult);
        } else {
            NetworkHelper.toLiveData(api.productApi().updateProduct(productId, body), this::handleSubmitResult);
        }
    }

    private void handleSubmitResult(Resource<Product> result) {
        setLoading(false);
        if (result != null && result.isSuccess()) {
            Toast.makeText(this, productId == null ? "Da tao san pham" : "Da cap nhat san pham", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, result != null && result.message != null ? result.message : getString(R.string.error_generic), Toast.LENGTH_LONG).show();
        }
    }

    private void applySelectedCategory(Product body) {
        int pos = spCategory.getSelectedItemPosition();
        if (pos <= 0 || pos - 1 >= categories.size()) return;
        Category c = categories.get(pos - 1);
        String key = categoryKey(c);
        if (key == null || key.isEmpty()) return;
        body.category_id = key;
        body.category_ids = new ArrayList<>();
        body.category_ids.add(key);
        try {
            int numericId = Integer.parseInt(key);
            body.primaryCategoryId = numericId;
            body.categoryIds = new ArrayList<>();
            body.categoryIds.add(numericId);
        } catch (NumberFormatException ignored) {
            // Category collections can use either numeric category_id or ObjectId.
        }
    }

    private void updateSummary() {
        double price = parseDouble(textOf(etPrice));
        double salePrice = parseDouble(textOf(etSalePrice));
        double selling = salePrice > 0 && salePrice < price ? salePrice : price;
        tvSummaryPrice.setText("Gia dang ban: " + Formatters.formatVnd(selling));
        tvSummaryOriginal.setText("Gia goc: " + Formatters.formatVnd(price));
        tvSummaryCategory.setText("Danh muc: " + selectedCategoryName());
        int stock = Math.max(0, parseInt(textOf(etStock)));
        tvSummaryStock.setText("Ton kho: " + (stock <= 0 ? "Het hang" : stock + " san pham"));
        String brand = textOf(etBrand);
        tvSummaryBrand.setText("Thuong hieu: " + (brand.isEmpty() ? "Chua nhap" : brand));
        String slug = textOf(etSlug);
        tvSummarySlug.setText("Slug: " + (slug.isEmpty() ? "Chua tao" : slug));
        List<String> images = parseImages();
        tvImageCount.setText(images.size() + " anh");
        if (!images.isEmpty()) {
            Glide.with(this).load(images.get(0)).placeholder(R.drawable.ic_box).into(ivPreview);
        } else {
            ivPreview.setImageResource(R.drawable.ic_box);
        }
    }

    private String selectedCategoryName() {
        int pos = spCategory.getSelectedItemPosition();
        if (pos <= 0 || pos - 1 >= categories.size()) return "Chua chon";
        Category c = categories.get(pos - 1);
        return c.name != null ? c.name : "Danh muc";
    }

    private List<String> parseImages() {
        List<String> urls = new ArrayList<>();
        String raw = textOf(etImages);
        if (raw.isEmpty()) return urls;
        String[] lines = raw.split("\\r?\\n");
        for (String line : lines) {
            String url = line.trim();
            if (!url.isEmpty()) urls.add(url);
        }
        return urls;
    }

    private String categoryKey(Category c) {
        if (c == null) return null;
        if (c.categoryId != null && !c.categoryId.isEmpty()) return c.categoryId;
        if (c.id != null && !c.id.isEmpty()) return c.id;
        return null;
    }

    private String categoryKeyFromProduct(Product p) {
        if (p.category_id != null && !p.category_id.isEmpty()) return p.category_id;
        if (p.category_ids != null && !p.category_ids.isEmpty()) return p.category_ids.get(0);
        if (p.primaryCategoryId != null) return String.valueOf(p.primaryCategoryId);
        if (p.category instanceof Map) {
            Object key = p.category.get("category_id");
            if (key == null) key = p.category.get("_id");
            if (key != null) return key.toString();
        }
        return null;
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        scroll.setAlpha(loading ? 0.45f : 1f);
        btnSave.setEnabled(!loading);
    }

    private String slugify(String input) {
        String normalized = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD);
        String withoutMarks = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
        return withoutMarks.toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String textOf(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private String value(String text) {
        return text != null ? text : "";
    }

    private String numberText(Double value) {
        if (value == null) return "";
        if (Math.floor(value) == value) return String.valueOf(value.longValue());
        return String.valueOf(value);
    }

    private double parseDouble(String text) {
        try {
            return text == null || text.isEmpty() ? 0 : Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private int parseInt(String text) {
        try {
            return text == null || text.isEmpty() ? 0 : Integer.parseInt(text);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private class SummaryWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            updateSummary();
        }
    }
}
