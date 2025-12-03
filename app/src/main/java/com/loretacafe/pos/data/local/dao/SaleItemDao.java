package com.loretacafe.pos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.loretacafe.pos.data.local.entity.SaleItemEntity;

import java.util.List;

@Dao
public interface SaleItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<SaleItemEntity> items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SaleItemEntity item);

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    List<SaleItemEntity> getItemsBySaleId(long saleId);

    @Query("DELETE FROM sale_items WHERE sale_id = :saleId")
    void deleteBySaleId(long saleId);

    @Query("DELETE FROM sale_items")
    void clear();
}

