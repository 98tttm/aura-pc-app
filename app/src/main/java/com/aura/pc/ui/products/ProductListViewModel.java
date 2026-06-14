package com.aura.pc.ui.products;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.aura_pc_app.data.api.ApiService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ViewModel quản lý trạng thái phân trang và bộ lọc cho danh sách sản phẩm.
 * Sử dụng manual pagination thay vì Paging 3 để tương thích với Java thuần.
 */
public class ProductListViewModel extends ViewModel {

    private static final int PAGE_SIZE = 20;

    private ApiService apiService;
    private int currentPage = 1;
    private int totalProducts = 0;
    private boolean isLoading = false;

    // Filter state
    private String category = null;
    private String brand = null;
    private Double minPrice = null;
    private Double maxPrice = null;
    private Double minRating = null;
    private Boolean inStock = null;
    private String sort = null;

    // LiveData
    private final MutableLiveData<List<Map<String, Object>>> productList = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> loadingMore = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isEmpty = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> hasMore = new MutableLiveData<>(false);
    private final MutableLiveData<List<Map<String, Object>>> subCategories = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> resolvedParentCategory = new MutableLiveData<>(null);
    private final MutableLiveData<String> resolvedParentName = new MutableLiveData<>(null);
    
    // Store the main parent category slug
    private String parentCategorySlug = null;

    public void init(ApiService apiService) {
        this.apiService = apiService;
    }

    public LiveData<List<Map<String, Object>>> getProductList() { return productList; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<Boolean> getLoadingMore() { return loadingMore; }
    public LiveData<Boolean> getIsEmpty() { return isEmpty; }
    public LiveData<String> getError() { return error; }
    public LiveData<Boolean> getHasMore() { return hasMore; }
    public LiveData<List<Map<String, Object>>> getSubCategories() { return subCategories; }
    public LiveData<String> getResolvedParentCategory() { return resolvedParentCategory; }
    public LiveData<String> getResolvedParentName() { return resolvedParentName; }

    public void setParentCategorySlug(String parentCategorySlug) {
        this.parentCategorySlug = parentCategorySlug;
    }

    /**
     * Load trang đầu tiên (reset dữ liệu cũ).
     */
    public void loadFirstPage() {
        if (isLoading) return;
        isLoading = true;
        currentPage = 1;
        loading.setValue(true);
        isEmpty.setValue(false);
        error.setValue(null);

        fetchProducts(1, true);
    }

    /**
     * Load trang tiếp theo (append).
     */
    public void loadNextPage() {
        if (isLoading) return;
        List<Map<String, Object>> current = productList.getValue();
        if (current != null && current.size() >= totalProducts) return;

        isLoading = true;
        loadingMore.setValue(true);
        fetchProducts(currentPage + 1, false);
    }

    private void fetchProducts(int page, boolean isFirstPage) {
        apiService.getProductsFiltered(
                page, PAGE_SIZE, category, brand, minPrice, maxPrice, minRating, inStock, sort
        ).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                isLoading = false;
                loading.postValue(false);
                loadingMore.postValue(false);

                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                    if (items == null) items = Collections.emptyList();

                    Object totalObj = body.get("total");
                    if (totalObj instanceof Number) {
                        totalProducts = ((Number) totalObj).intValue();
                    }

                    currentPage = page;

                    if (isFirstPage) {
                        productList.postValue(new ArrayList<>(items));
                        isEmpty.postValue(items.isEmpty());
                    } else {
                        List<Map<String, Object>> current = productList.getValue();
                        if (current == null) current = new ArrayList<>();
                        List<Map<String, Object>> merged = new ArrayList<>(current);
                        merged.addAll(items);
                        productList.postValue(merged);
                    }

                    // Kiểm tra còn dữ liệu để tải không
                    List<Map<String, Object>> all = productList.getValue();
                    int loadedCount = (all != null ? all.size() : 0) + (isFirstPage ? 0 : items.size());
                    if (isFirstPage) loadedCount = items.size();
                    hasMore.postValue(loadedCount < totalProducts);
                } else {
                    if (isFirstPage) {
                        error.postValue("Lỗi tải dữ liệu (HTTP " + response.code() + ")");
                        isEmpty.postValue(true);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                isLoading = false;
                loading.postValue(false);
                loadingMore.postValue(false);
                if (isFirstPage) {
                    error.postValue("Lỗi kết nối mạng");
                    isEmpty.postValue(true);
                }
            }
        });
    }

    // ===== Filter setters =====

    public void setCategory(String category) { this.category = category; }
    public String getCategory() { return category; }

    public void setSort(String sort) { this.sort = sort; }
    public String getSort() { return sort; }

    public void setBrand(String brand) { this.brand = brand; }
    public String getBrand() { return brand; }

    public void setMinPrice(Double minPrice) { this.minPrice = minPrice; }
    public Double getMinPrice() { return minPrice; }

    public void setMaxPrice(Double maxPrice) { this.maxPrice = maxPrice; }
    public Double getMaxPrice() { return maxPrice; }

    public void setMinRating(Double minRating) { this.minRating = minRating; }
    public Double getMinRating() { return minRating; }

    public void setInStock(Boolean inStock) { this.inStock = inStock; }
    public Boolean getInStock() { return inStock; }

    /**
     * Áp dụng bộ lọc hiện tại → load lại từ trang 1.
     */
    public void applyFilters() {
        loadFirstPage();
    }

    /**
     * Xóa tất cả bộ lọc → load lại.
     */
    public void clearFilters() {
        // Không clear category để giữ nguyên danh mục hiện tại (VD: đang ở ghế công thái học thì reset xong vẫn ở đó)
        this.brand = null;
        this.minPrice = null;
        this.maxPrice = null;
        this.minRating = null;
        this.inStock = null;
        // Có thể cân nhắc giữ lại `sort` nếu muốn, hoặc reset sort về mặc định. Ở đây reset sort.
        this.sort = null;
        loadFirstPage();
    }

    public void fetchSubCategories() {
        if (parentCategorySlug == null || parentCategorySlug.isEmpty()) return;
        
        apiService.getCategories().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> allCategories = response.body();
                    
                    // 1. Find the target category by initial parentCategorySlug
                    Map<String, Object> targetCategory = null;
                    for (Map<String, Object> cat : allCategories) {
                        if (parentCategorySlug.equals(cat.get("slug"))) {
                            targetCategory = cat;
                            break;
                        }
                    }
                    
                    String actualParentSlug = parentCategorySlug;
                    String actualParentName = null;
                    
                    // 2. If it has a parent_id, it is already a subcategory!
                    // So we must use its parent's ID to fetch sibling subcategories.
                    if (targetCategory != null && targetCategory.get("parent_id") != null) {
                        actualParentSlug = targetCategory.get("parent_id").toString();
                        // Find the parent's name
                        for (Map<String, Object> cat : allCategories) {
                            if (actualParentSlug.equals(cat.get("slug"))) {
                                actualParentName = (String) cat.get("name");
                                break;
                            }
                        }
                    } else if (targetCategory != null) {
                        // This is the parent category itself
                        actualParentName = (String) targetCategory.get("name");
                    }
                    
                    // 3. Find subcategories of the actual parent
                    List<Map<String, Object>> subs = new ArrayList<>();
                    for (Map<String, Object> cat : allCategories) {
                        Object parentIdObj = cat.get("parent_id");
                        if (parentIdObj != null && actualParentSlug.equals(parentIdObj.toString())) {
                            subs.add(cat);
                        }
                    }
                    
                    resolvedParentCategory.postValue(actualParentSlug);
                    if (actualParentName != null) {
                        resolvedParentName.postValue(actualParentName);
                    }
                    subCategories.postValue(subs);
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                // Ignore or handle error
            }
        });
    }
}
