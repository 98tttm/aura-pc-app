package com.aura.pc.ui.address;

import java.util.List;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Path;

/**
 * Client gọi API đơn vị hành chính VN (https://provinces.open-api.vn) —
 * dùng đúng nguồn dữ liệu Tỉnh/Quận/Phường như website (address.service.ts).
 * Tách riêng vì base URL khác và không cần token xác thực.
 */
public interface LocationApi {

    String BASE_URL = "https://provinces.open-api.vn/api/";

    /** Danh sách toàn bộ tỉnh/thành. */
    @GET("p/")
    Call<List<VNLocation>> getProvinces();

    /** Một tỉnh kèm danh sách quận/huyện (depth=2). */
    @GET("p/{code}?depth=2")
    Call<VNLocation> getDistricts(@Path("code") int provinceCode);

    /** Một quận/huyện kèm danh sách phường/xã (depth=2). */
    @GET("d/{code}?depth=2")
    Call<VNLocation> getWards(@Path("code") int districtCode);

    /** Singleton Retrofit nhẹ, không Bearer token. */
    final class Provider {
        private static LocationApi instance;

        private Provider() {}

        public static synchronized LocationApi get() {
            if (instance == null) {
                OkHttpClient client = new OkHttpClient.Builder().build();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                instance = retrofit.create(LocationApi.class);
            }
            return instance;
        }
    }
}
