package com.loretacafe.pos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.loretacafe.pos.data.local.entity.IngredientDeductionEntity;

import java.util.List;

@Dao
public interface IngredientDeductionDao {

    @Insert
    long insert(IngredientDeductionEntity deduction);

    @Insert
    void insertAll(List<IngredientDeductionEntity> deductions);

    @Query("SELECT * FROM ingredient_deductions WHERE sale_id = :saleId ORDER BY deducted_at ASC")
    LiveData<List<IngredientDeductionEntity>> observeBySaleId(long saleId);

    @Query("SELECT * FROM ingredient_deductions WHERE sale_id = :saleId ORDER BY deducted_at ASC")
    List<IngredientDeductionEntity> getBySaleId(long saleId);

    @Query("SELECT * FROM ingredient_deductions WHERE raw_material_id = :rawMaterialId ORDER BY deducted_at DESC")
    List<IngredientDeductionEntity> getByRawMaterialId(long rawMaterialId);

    @Query("DELETE FROM ingredient_deductions WHERE sale_id = :saleId")
    void deleteBySaleId(long saleId);
}

