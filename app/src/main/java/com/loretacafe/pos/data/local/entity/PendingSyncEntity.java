package com.loretacafe.pos.data.local.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;

@Entity(tableName = "pending_sync")
public class PendingSyncEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private PendingSyncType type;

    @ColumnInfo(name = "entity_id")
    private Long entityId;

    private String payload;

    @ColumnInfo(name = "retry_count")
    private int retryCount;

    @ColumnInfo(name = "created_at")
    private OffsetDateTime createdAt;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public PendingSyncType getType() {
        return type;
    }

    public void setType(PendingSyncType type) {
        this.type = type;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

