package com.aurapc.admin.utils;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aurapc.admin.data.api.Resource;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NetworkHelper {

    /** Adapter for raw responses (most endpoints return raw JSON). */
    public static <T> LiveData<Resource<T>> toLiveData(Call<T> call) {
        MutableLiveData<Resource<T>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    result.setValue(Resource.success(response.body()));
                } else if (response.isSuccessful()) {
                    result.setValue(Resource.success(null));
                } else {
                    result.setValue(Resource.error(parseError(response), null));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                result.setValue(Resource.error(t.getMessage() != null ? t.getMessage() : "Lỗi mạng", null));
            }
        });

        return result;
    }

    /**
     * Variant that triggers a callback side-effect on success. Useful when the
     * caller needs to do additional work after the request completes (e.g.,
     * updating UI in a non-LiveData context).
     */
    public static <T> void toLiveData(Call<T> call, ResourceListener<T> listener) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(Call<T> call, Response<T> response) {
                if (response.isSuccessful() && response.body() != null) {
                    if (listener != null) listener.onResult(Resource.success(response.body()));
                } else {
                    if (listener != null)
                        listener.onResult(Resource.error(parseError(response), null));
                }
            }

            @Override
            public void onFailure(Call<T> call, Throwable t) {
                if (listener != null)
                    listener.onResult(Resource.error(t.getMessage() != null ? t.getMessage() : "Lỗi mạng", null));
            }
        });
    }

    private static String parseError(Response<?> response) {
        try {
            if (response.errorBody() != null) {
                return "Lỗi " + response.code() + ": " + response.errorBody().string();
            }
        } catch (Exception ignored) {
        }
        return "Lỗi kết nối: " + response.code();
    }

    public interface ResourceListener<T> {
        void onResult(Resource<T> result);
    }
}
