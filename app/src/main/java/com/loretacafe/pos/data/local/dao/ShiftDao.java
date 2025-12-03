package com.loretacafe.pos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.loretacafe.pos.data.local.entity.ShiftEntity;

import java.time.OffsetDateTime;
import java.util.List;

@Dao
public interface ShiftDao {
    
    @Insert
    long insert(ShiftEntity shift);
    
    @Update
    void update(ShiftEntity shift);
    
    @Delete
    void delete(ShiftEntity shift);
    
    @Query("SELECT * FROM shifts WHERE id = :id")
    ShiftEntity getById(long id);
    
    @Query("SELECT * FROM shifts WHERE user_id = :userId ORDER BY clock_in_time DESC")
    List<ShiftEntity> getShiftsByUser(long userId);
    
    @Query("SELECT * FROM shifts WHERE user_id = :userId AND clock_out_time IS NULL LIMIT 1")
    ShiftEntity getActiveShiftByUser(long userId);
    
    @Query("SELECT * FROM shifts ORDER BY clock_in_time DESC")
    List<ShiftEntity> getAllShifts();
    
    @Query("SELECT * FROM shifts WHERE clock_in_time >= :startDate AND clock_in_time < :endDate ORDER BY clock_in_time DESC")
    List<ShiftEntity> getShiftsByDateRange(OffsetDateTime startDate, OffsetDateTime endDate);
    
    @Query("SELECT COUNT(*) FROM shifts WHERE user_id = :userId AND clock_out_time IS NULL")
    int getActiveShiftCount(long userId);
}






