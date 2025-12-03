package com.loretacafe.pos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.loretacafe.pos.data.local.entity.ProductEntity;

import java.util.List;

@Dao
public interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    LiveData<List<ProductEntity>> observeAll();

    @Query("SELECT * FROM products WHERE product_id >= 10000 OR category IN ('POWDER', 'SYRUP', 'SHAKERS / TOPPINGS / JAMS', 'MILK', 'COFFEE BEANS') ORDER BY name ASC")
    LiveData<List<ProductEntity>> observeAllRawMaterials();

    @Query("SELECT * FROM products ORDER BY name ASC")
    List<ProductEntity> getAll();

    @Query("SELECT * FROM products WHERE product_id = :productId LIMIT 1")
    ProductEntity getById(long productId);

    @Query("SELECT * FROM products WHERE name = :name LIMIT 1")
    ProductEntity getByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductEntity> products);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProductEntity product);

    @Update
    void update(ProductEntity product);

    @Query("DELETE FROM products")
    void clear();

    @Query("DELETE FROM products WHERE product_id = :productId")
    void delete(long productId);
}

