package com.loretacafe.pos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.loretacafe.pos.data.local.entity.RecipeEntity;

import java.util.List;

@Dao
public interface RecipeDao {

    @Query("SELECT * FROM recipes WHERE product_id = :productId")
    LiveData<List<RecipeEntity>> observeByProductId(long productId);

    @Query("SELECT * FROM recipes WHERE product_id = :productId")
    List<RecipeEntity> getByProductId(long productId);

    @Query("SELECT * FROM recipes WHERE recipe_id = :recipeId LIMIT 1")
    RecipeEntity getById(long recipeId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(RecipeEntity recipe);

    @Update
    void update(RecipeEntity recipe);

    @Query("DELETE FROM recipes WHERE recipe_id = :recipeId")
    void delete(long recipeId);

    @Query("DELETE FROM recipes WHERE product_id = :productId")
    void deleteByProductId(long productId);

    @Query("SELECT * FROM recipes")
    List<RecipeEntity> getAllRecipes();
}

