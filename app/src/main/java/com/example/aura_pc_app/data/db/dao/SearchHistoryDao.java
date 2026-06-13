package com.example.aura_pc_app.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.aura_pc_app.data.db.entity.SearchHistoryEntity;

import java.util.List;

@Dao
public interface SearchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(SearchHistoryEntity item);

    @Query("SELECT * FROM search_history ORDER BY updatedAt DESC LIMIT :limit")
    List<SearchHistoryEntity> getRecent(int limit);

    @Query("DELETE FROM search_history WHERE normalizedKeyword NOT IN (" +
            "SELECT normalizedKeyword FROM search_history ORDER BY updatedAt DESC LIMIT :limit" +
            ")")
    void pruneToLimit(int limit);
}
