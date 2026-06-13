package com.aura.pc.ui.products;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingSource;
import androidx.paging.PagingState;

import com.example.aura_pc_app.data.api.ApiService;

import java.util.List;
import java.util.Map;

import kotlin.coroutines.Continuation;
import retrofit2.Response;

/**
 * PagingSource cho danh sách sản phẩm.
 * Gọi API getProductsFiltered với các bộ lọc hiện tại.
 */
public class ProductPagingSource extends PagingSource<Integer, Map<String, Object>> {

    private final ApiService apiService;
    private final String category;
    private final String brand;
    private final Double minPrice;
    private final Double maxPrice;
    private final Double minRating;
    private final Boolean inStock;
    private final String sort;

    public ProductPagingSource(ApiService apiService,
                                String category,
                                String brand,
                                Double minPrice,
                                Double maxPrice,
                                Double minRating,
                                Boolean inStock,
                                String sort) {
        this.apiService = apiService;
        this.category = category;
        this.brand = brand;
        this.minPrice = minPrice;
        this.maxPrice = maxPrice;
        this.minRating = minRating;
        this.inStock = inStock;
        this.sort = sort;
    }

    @Nullable
    @Override
    public Integer getRefreshKey(@NonNull PagingState<Integer, Map<String, Object>> state) {
        Integer anchorPosition = state.getAnchorPosition();
        if (anchorPosition == null) return null;
        LoadResult.Page<Integer, Map<String, Object>> page = state.closestPageToPosition(anchorPosition);
        if (page == null) return null;
        Integer prevKey = page.getPrevKey();
        if (prevKey != null) return prevKey + 1;
        Integer nextKey = page.getNextKey();
        if (nextKey != null) return nextKey - 1;
        return null;
    }

    @Nullable
    @Override
    public Object load(@NonNull LoadParams<Integer> params, @NonNull Continuation<? super LoadResult<Integer, Map<String, Object>>> continuation) {
        // Paging 3 sử dụng Kotlin coroutines, nhưng trong Java ta cần blocking call
        // Tuy nhiên Paging 3 cho Java hỗ trợ thông qua ListenableFuture hoặc RxJava.
        // Ở đây ta sẽ dùng synchronous call bên trong thread pool của Paging.
        int page = params.getKey() != null ? params.getKey() : 1;
        int limit = params.getLoadSize();

        try {
            Response<Map<String, Object>> response = apiService.getProductsFiltered(
                    page, limit, category, brand, minPrice, maxPrice, minRating, inStock, sort
            ).execute();

            if (response.isSuccessful() && response.body() != null) {
                Map<String, Object> body = response.body();
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                if (items == null) {
                    items = java.util.Collections.emptyList();
                }

                int total = 0;
                Object totalObj = body.get("total");
                if (totalObj instanceof Number) {
                    total = ((Number) totalObj).intValue();
                }

                Integer prevKey = (page == 1) ? null : page - 1;
                // Nếu đã lấy hết sản phẩm thì nextKey = null
                Integer nextKey = (page * 20 >= total) ? null : page + 1;

                return new LoadResult.Page<>(items, prevKey, nextKey);
            } else {
                return new LoadResult.Error<>(new Exception("API error: " + response.code()));
            }
        } catch (Exception e) {
            return new LoadResult.Error<>(e);
        }
    }
}
