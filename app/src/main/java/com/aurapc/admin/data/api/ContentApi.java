package com.aurapc.admin.data.api;

import com.aurapc.admin.data.model.BlogPost;
import com.aurapc.admin.data.model.Category;
import com.aurapc.admin.data.model.Promotion;
import com.aurapc.admin.data.model.HubPost;
import com.aurapc.admin.data.model.HubComment;
import com.aurapc.admin.data.model.WarrantyItem;
import com.aurapc.admin.data.model.SupportConversation;
import com.aurapc.admin.data.model.SupportMessage;
import com.aurapc.admin.data.model.ProductReview;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ContentApi {

    /* Notifications */
    @GET("api/admin/notifications")
    Call<NotificationListResponse> listNotifications(
            @Query("limit") int limit,
            @Query("unreadOnly") boolean unreadOnly
    );

    @PATCH("api/admin/notifications/{id}/read")
    Call<Object> markNotificationRead(@Path("id") String id);

    @PATCH("api/admin/notifications/read-all")
    Call<Map<String, Object>> markAllNotificationsRead();

    /* Categories */
    @GET("api/admin/categories")
    Call<List<Category>> listCategories();

    @POST("api/admin/categories")
    Call<Category> createCategory(@Body Category body);

    @PUT("api/admin/categories/{id}")
    Call<Category> updateCategory(@Path("id") String id, @Body Category body);

    @DELETE("api/admin/categories/{id}")
    Call<Map<String, Object>> deleteCategory(@Path("id") String id);

    /* Blogs */
    @GET("api/admin/blogs")
    Call<BlogListResponse> listBlogs(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @GET("api/admin/blogs/{id}")
    Call<BlogPost> getBlog(@Path("id") String id);

    @POST("api/admin/blogs")
    Call<BlogPost> createBlog(@Body BlogPost body);

    @PUT("api/admin/blogs/{id}")
    Call<BlogPost> updateBlog(@Path("id") String id, @Body BlogPost body);

    @DELETE("api/admin/blogs/{id}")
    Call<Map<String, Object>> deleteBlog(@Path("id") String id);

    /* Hub (community) */
    @GET("api/admin/hub/posts")
    Call<HubListResponse> listHubPosts(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("status") String status,
            @Query("topic") String topic,
            @Query("search") String search,
            @Query("sort") String sort
    );

    @GET("api/admin/hub/posts/{id}")
    Call<HubPost> getHubPost(@Path("id") String id);

    @PATCH("api/admin/hub/posts/{id}/approve")
    Call<HubPost> approveHubPost(@Path("id") String id, @Body Map<String, Object> body);

    @PATCH("api/admin/hub/posts/{id}/reject")
    Call<HubPost> rejectHubPost(@Path("id") String id, @Body Map<String, Object> body);

    @DELETE("api/admin/hub/posts/{id}")
    Call<Map<String, Object>> deleteHubPost(@Path("id") String id);

    @GET("api/admin/hub/posts/{id}/comments")
    Call<List<HubComment>> getHubComments(@Path("id") String postId);

    @DELETE("api/admin/hub/comments/{id}")
    Call<Map<String, Object>> deleteHubComment(@Path("id") String id);

    /* Support */
    @GET("api/admin/support")
    Call<SupportListResponse> listSupport(
            @Query("tab") String tab,
            @Query("search") String search
    );

    @GET("api/admin/support/{conversationId}")
    Call<SupportDetailResponse> getSupportDetail(@Path("conversationId") String conversationId);

    @PUT("api/admin/support/{conversationId}/read")
    Call<Map<String, Object>> markSupportRead(@Path("conversationId") String conversationId);

    @PUT("api/admin/support/{conversationId}/archive")
    Call<Map<String, Object>> archiveSupport(@Path("conversationId") String conversationId,
                                              @Body Map<String, Object> body);

    @POST("api/admin/support/{conversationId}/messages")
    Call<SupportSendResponse> sendSupportMessage(@Path("conversationId") String conversationId,
                                                  @Body Map<String, Object> body);

    @POST("api/admin/support-assign/{conversationId}")
    Call<SupportSendResponse> assignSupport(@Path("conversationId") String conversationId,
                                             @Body Map<String, Object> body);

    /* Warranty */
    @GET("api/admin/warranty")
    Call<WarrantyListResponse> listWarranty(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("status") String status,
            @Query("search") String search
    );

    @GET("api/admin/warranty/stats")
    Call<Map<String, Object>> warrantyStats();

    /* Promotions */
    @GET("api/admin/promotions")
    Call<PromotionListResponse> listPromotions(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("search") String search
    );

    @GET("api/admin/promotions/{id}")
    Call<Promotion> getPromotion(@Path("id") String id);

    @POST("api/admin/promotions")
    Call<Promotion> createPromotion(@Body Promotion body);

    @PUT("api/admin/promotions/{id}")
    Call<Promotion> updatePromotion(@Path("id") String id, @Body Promotion body);

    @DELETE("api/admin/promotions/{id}")
    Call<Map<String, Object>> deletePromotion(@Path("id") String id);

    /* Reviews */
    @GET("api/admin/reviews/flagged")
    Call<ReviewListResponse> listFlaggedReviews(
            @Query("page") int page,
            @Query("limit") int limit
    );

    @POST("api/admin/reviews/{id}/flag")
    Call<ProductReview> flagReview(@Path("id") String id, @Body Map<String, Object> body);

    @POST("api/admin/reviews/{id}/hide")
    Call<ProductReview> hideReview(@Path("id") String id);

    @POST("api/admin/reviews/{id}/restore")
    Call<ProductReview> restoreReview(@Path("id") String id);

    class NotificationListResponse {
        public List<Map<String, Object>> items;
        public Integer unreadCount;
    }

    class BlogListResponse {
        public List<BlogPost> items;
        public int total;
        public int page;
        public int limit;
    }

    class HubListResponse {
        public List<HubPost> items;
        public int total;
        public int page;
        public int totalPages;
        public int limit;
    }

    class WarrantyListResponse {
        public List<WarrantyItem> items;
        public int total;
        public int page;
        public int limit;
    }

    class PromotionListResponse {
        public List<Promotion> items;
        public int total;
        public int page;
        public int limit;
    }

    class ReviewListResponse {
        public List<ProductReview> items;
        public int total;
        public int page;
        public int limit;
    }

    class SupportListResponse {
        public List<SupportConversation> items;
        public Map<String, Object> counts;
    }

    class SupportDetailResponse {
        public SupportConversation conversation;
        public List<SupportMessage> messages;
    }

    class SupportSendResponse {
        public boolean success;
        public SupportConversation conversation;
        public SupportMessage message;
    }
}