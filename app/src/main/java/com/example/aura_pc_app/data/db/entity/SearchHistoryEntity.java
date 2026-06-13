package com.example.aura_pc_app.data.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "search_history")
public class SearchHistoryEntity {
    @PrimaryKey
    @NonNull
    public String normalizedKeyword = "";

    public String keyword;
    public long updatedAt;

    public SearchHistoryEntity(@NonNull String normalizedKeyword, String keyword, long updatedAt) {
        this.normalizedKeyword = normalizedKeyword;
        this.keyword = keyword;
        this.updatedAt = updatedAt;
    }
}
