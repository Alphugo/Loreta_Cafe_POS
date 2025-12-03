package com.loretacafe.pos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.loretacafe.pos.data.local.entity.PendingSyncEntity;

import java.util.List;

@Dao
public interface PendingSyncDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(PendingSyncEntity pendingSyncEntity);

    @Query("SELECT * FROM pending_sync ORDER BY created_at ASC")
    List<PendingSyncEntity> getPending();

    @Delete
    void delete(PendingSyncEntity entity);

    @Update
    void update(PendingSyncEntity entity);

    @Query("DELETE FROM pending_sync")
    void clear();
}

