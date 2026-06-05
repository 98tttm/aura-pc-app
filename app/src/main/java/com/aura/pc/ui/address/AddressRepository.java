package com.aura.pc.ui.address;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.aura_pc_app.data.api.ApiClient;
import com.example.aura_pc_app.data.api.ApiService;
import com.example.aura_pc_app.data.api.TokenManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Lớp trung gian gọi 5 endpoint /auth/addresses của backend (giống AddressService bên website).
 * Mọi request đều tự gắn Bearer token nhờ AuthInterceptor trong ApiClient.
 */
public class AddressRepository {

    private static final String TAG = "AddressRepository";

    /** Callback bất đồng bộ cho UI. */
    public interface Callback2 {
        void onSuccess(List<Address> addresses);
        void onError(String message);
    }

    private final ApiService api;
    private final TokenManager tokenManager;
    private final Gson gson = new Gson();

    public AddressRepository(Context context) {
        ApiClient client = ApiClient.getInstance(context.getApplicationContext());
        this.api = client.getApiService();
        this.tokenManager = client.getTokenManager();
    }

    /** Lấy userId từ JSON user đã lưu lúc đăng nhập (_id của Mongo, fallback id). */
    public String getUserId() {
        String json = tokenManager.getCurrentUserJson();
        if (TextUtils.isEmpty(json)) return null;
        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            if (obj == null) return null;
            if (obj.has("_id") && !obj.get("_id").isJsonNull()) {
                return obj.get("_id").getAsString();
            }
            if (obj.has("id") && !obj.get("id").isJsonNull()) {
                return obj.get("id").getAsString();
            }
        } catch (Exception e) {
            Log.e(TAG, "parse userId failed", e);
        }
        return null;
    }

    public boolean hasUser() {
        return !TextUtils.isEmpty(getUserId());
    }

    // ── Đọc danh sách ─────────────────────────────────────
    public void load(final Callback2 cb) {
        String userId = getUserId();
        if (userId == null) { cb.onError("Chưa đăng nhập"); return; }
        api.getAddresses(userId).enqueue(wrap(cb));
    }

    // ── Thêm ──────────────────────────────────────────────
    public void add(Address a, final Callback2 cb) {
        String userId = getUserId();
        if (userId == null) { cb.onError("Chưa đăng nhập"); return; }
        Map<String, Object> body = toBody(a);
        body.put("userId", userId);
        api.addAddress(body).enqueue(wrap(cb));
    }

    // ── Cập nhật ──────────────────────────────────────────
    public void update(String addressId, Address a, final Callback2 cb) {
        String userId = getUserId();
        if (userId == null) { cb.onError("Chưa đăng nhập"); return; }
        Map<String, Object> body = toBody(a);
        body.put("userId", userId);
        api.updateAddress(addressId, body).enqueue(wrap(cb));
    }

    // ── Xóa ───────────────────────────────────────────────
    public void remove(String addressId, final Callback2 cb) {
        String userId = getUserId();
        if (userId == null) { cb.onError("Chưa đăng nhập"); return; }
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        api.deleteAddress(addressId, body).enqueue(wrap(cb));
    }

    // ── Đặt mặc định ──────────────────────────────────────
    public void setDefault(String addressId, final Callback2 cb) {
        String userId = getUserId();
        if (userId == null) { cb.onError("Chưa đăng nhập"); return; }
        Map<String, Object> body = new HashMap<>();
        body.put("userId", userId);
        api.setDefaultAddress(addressId, body).enqueue(wrap(cb));
    }

    // ── Helpers ───────────────────────────────────────────
    private Map<String, Object> toBody(Address a) {
        Map<String, Object> body = new HashMap<>();
        body.put("label", a.label);
        body.put("fullName", a.fullName);
        body.put("phone", a.phone);
        body.put("city", a.city);
        body.put("district", a.district);
        body.put("ward", a.ward);
        body.put("address", a.address);
        body.put("isDefault", a.isDefault);
        return body;
    }

    private Callback<AddressListResponse> wrap(final Callback2 cb) {
        return new Callback<AddressListResponse>() {
            @Override
            public void onResponse(@NonNull Call<AddressListResponse> call,
                                   @NonNull Response<AddressListResponse> response) {
                AddressListResponse body = response.body();
                if (response.isSuccessful() && body != null && body.success) {
                    List<Address> list = body.addresses != null ? body.addresses : new ArrayList<>();
                    cb.onSuccess(list);
                } else {
                    String msg = body != null && body.message != null
                            ? body.message
                            : "Lỗi máy chủ (" + response.code() + ")";
                    cb.onError(msg);
                }
            }

            @Override
            public void onFailure(@NonNull Call<AddressListResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "API failure", t);
                cb.onError("Không kết nối được máy chủ. Kiểm tra mạng.");
            }
        };
    }
}
