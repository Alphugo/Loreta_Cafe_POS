package com.loretacafe.pos.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.loretacafe.pos.data.local.entity.VerificationCodeEntity;

import java.time.OffsetDateTime;

@Dao
public interface VerificationCodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(VerificationCodeEntity code);

    @Update
    void update(VerificationCodeEntity code);

    @Query("SELECT * FROM verification_codes WHERE email = :email AND used = 0 AND expires_at > :now ORDER BY created_at DESC LIMIT 1")
    VerificationCodeEntity getLatestValidCode(String email, OffsetDateTime now);

    @Query("SELECT * FROM verification_codes WHERE email = :email AND code = :code AND used = 0 AND expires_at > :now LIMIT 1")
    VerificationCodeEntity verifyCode(String email, String code, OffsetDateTime now);

    @Query("UPDATE verification_codes SET used = 1 WHERE code_id = :codeId")
    void markAsUsed(long codeId);

    @Query("DELETE FROM verification_codes WHERE expires_at < :now OR used = 1")
    void deleteExpiredCodes(OffsetDateTime now);

    @Query("DELETE FROM verification_codes WHERE email = :email")
    void deleteCodesForEmail(String email);
}

