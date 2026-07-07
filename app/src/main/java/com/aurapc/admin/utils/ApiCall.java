package com.aurapc.admin.utils;

import androidx.annotation.NonNull;

import com.aurapc.admin.data.api.Resource;

/**
 * Functional interface that maps a Retrofit Response to a Resource.
 */
public interface ApiCall<T> {
    retrofit2.Call<T> call();
}