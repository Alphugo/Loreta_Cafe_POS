package com.loretacafe.pos.data.local.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;

/**
 * Entity for tracking cashier shifts (clock in/out)
 */
@Entity(
    tableName = "shifts",
    foreignKeys = @ForeignKey(
        entity = UserEntity.class,
        parentColumns = "user_id",
        childColumns = "user_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("user_id"), @Index("clock_in_time")}
)
public class ShiftEntity {
    
    @PrimaryKey(autoGenerate = true)
    private long id;
    
    @ColumnInfo(name = "user_id")
    private long userId;
    
    @ColumnInfo(name = "user_name")
    private String userName;
    
    @ColumnInfo(name = "user_email")
    private String userEmail;
    
    @ColumnInfo(name = "clock_in_time")
    @NonNull
    private OffsetDateTime clockInTime;
    
    @ColumnInfo(name = "clock_out_time")
    private OffsetDateTime clockOutTime;
    
    @ColumnInfo(name = "duration_minutes")
    private Integer durationMinutes;
    
    @ColumnInfo(name = "notes")
    private String notes;
    
    @ColumnInfo(name = "created_at")
    @NonNull
    private OffsetDateTime createdAt;
    
    // Getters and Setters
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public long getUserId() {
        return userId;
    }
    
    public void setUserId(long userId) {
        this.userId = userId;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getUserEmail() {
        return userEmail;
    }
    
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
    
    @NonNull
    public OffsetDateTime getClockInTime() {
        return clockInTime;
    }
    
    public void setClockInTime(@NonNull OffsetDateTime clockInTime) {
        this.clockInTime = clockInTime;
    }
    
    public OffsetDateTime getClockOutTime() {
        return clockOutTime;
    }
    
    public void setClockOutTime(OffsetDateTime clockOutTime) {
        this.clockOutTime = clockOutTime;
    }
    
    public Integer getDurationMinutes() {
        return durationMinutes;
    }
    
    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    @NonNull
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(@NonNull OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Check if shift is currently active (not clocked out)
     */
    public boolean isActive() {
        return clockOutTime == null;
    }
}

