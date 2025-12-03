package com.loretacafe.pos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.loretacafe.pos.data.local.entity.CategoryEntity;

import java.util.List;

@Dao
public interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CategoryEntity category);

    @Update
    void update(CategoryEntity category);

    @Query("DELETE FROM categories WHERE category_id = :id")
    void delete(long id);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    LiveData<List<CategoryEntity>> observeAll();

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<CategoryEntity> getAll();

    @Query("SELECT * FROM categories WHERE category_id = :id LIMIT 1")
    CategoryEntity getById(long id);

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    CategoryEntity getByName(String name);

    @Query("UPDATE categories SET item_count = item_count + :increment WHERE category_id = :id")
    void updateItemCount(long id, int increment);
}

