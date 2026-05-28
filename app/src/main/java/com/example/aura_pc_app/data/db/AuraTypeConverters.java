package com.example.aura_pc_app.data.db;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;

public class AuraTypeConverters {
    private static final Gson gson = new Gson();

    @TypeConverter
    public static List<String> fromStringList(String value) {
        if (value == null) return null;
        Type listType = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(value, listType);
    }

    @TypeConverter
    public static String toStringList(List<String> list) {
        return gson.toJson(list);
    }
}