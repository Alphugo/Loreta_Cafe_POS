package com.loretacafe.pos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.loretacafe.pos.data.local.entity.IngredientEntity;

import java.util.List;

@Dao
public interface IngredientDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(IngredientEntity ingredient);

    @Update
    void update(IngredientEntity ingredient);

    @Query("DELETE FROM ingredients WHERE ingredient_id = :id")
    void delete(long id);

    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    LiveData<List<IngredientEntity>> observeAll();

    @Query("SELECT * FROM ingredients ORDER BY name ASC")
    List<IngredientEntity> getAll();

    @Query("SELECT * FROM ingredients WHERE ingredient_id = :id LIMIT 1")
    IngredientEntity getById(long id);

    @Query("SELECT * FROM ingredients WHERE name LIKE :searchQuery ORDER BY name ASC")
    List<IngredientEntity> searchByName(String searchQuery);

    @Query("SELECT * FROM ingredients WHERE status = :status ORDER BY name ASC")
    List<IngredientEntity> getByStatus(String status);

    @Query("UPDATE ingredients SET quantity = quantity - :amount WHERE ingredient_id = :id")
    void decreaseQuantity(long id, double amount);

    @Query("UPDATE ingredients SET quantity = quantity + :amount WHERE ingredient_id = :id")
    void increaseQuantity(long id, double amount);
}

