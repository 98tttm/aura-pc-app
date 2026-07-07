package com.aurapc.admin.data.api;

import com.aurapc.admin.data.model.CustomerUser;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserApi {

    @GET("api/admin/users")
    Call<UserListResponse> getUsers(
            @Query("search") String search,
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("api/admin/users/{id}")
    Call<CustomerUser> getUserDetail(@Path("id") String id);

    @PUT("api/admin/users/{id}/status")
    Call<CustomerUser> updateUserStatus(@Path("id") String id, @Query("active") boolean active);

    class UserListResponse {
        public List<CustomerUser> items;
        public int total;
        public int page;
        public int limit;
    }
}