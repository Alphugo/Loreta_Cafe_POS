package com.loretacafe.pos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.loretacafe.pos.data.local.entity.ReportEntity;

import java.util.List;

@Dao
public interface ReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReportEntity record);

    @Query("SELECT * FROM reports ORDER BY created_at DESC")
    LiveData<List<ReportEntity>> observeReports();

    @Query("DELETE FROM reports")
    void clear();
}

