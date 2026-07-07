package com.aurapc.admin.data.api;

import com.aurapc.admin.data.model.Product;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ProductApi {

    @GET("api/admin/products")
    Call<ProductListResponse> getProducts(
            @Query("search") String search,
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("api/admin/products/{id}")
    Call<Product> getProduct(@Path("id") String id);

    @POST("api/admin/products")
    Call<Product> createProduct(@Body Product body);

    @PUT("api/admin/products/{id}")
    Call<Product> updateProduct(@Path("id") String id, @Body Product body);

    @DELETE("api/admin/products/{id}")
    Call<Object> deleteProduct(@Path("id") String id);

    @GET("api/admin/extra-products/low-stock")
    Call<ProductListResponse> getLowStockProducts(@Query("threshold") int threshold);

    @POST("api/admin/extra-products/bulk-update")
    Call<Object> bulkUpdateProducts(@Body BulkProductUpdate body);

    class ProductListResponse {
        public List<Product> items;
        public int total;
        public int page;
        public int limit;
    }

    class BulkProductUpdate {
        public List<String> productIds;
        public Boolean isActive;
        public Integer stockAdjustment;
    }
}
