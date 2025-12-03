package com.loretacafe.pos.data.local.service;

import android.content.Context;
import android.util.Log;

import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.local.entity.VerificationCodeEntity;

import java.time.OffsetDateTime;
import java.util.Random;

/**
 * Service for handling password reset verification codes
 */
public class PasswordResetService {

    private static final String TAG = "PasswordResetService";
    private static final int CODE_EXPIRY_MINUTES = 5;
    private final AppDatabase database;
    private final Random random = new Random();

    public PasswordResetService(Context context) {
        this.database = AppDatabase.getInstance(context);
    }

    /**
     * Generate and store a 6-digit verification code for email
     * @param email User email
     * @return 6-digit code as string, or null if email doesn't exist
     */
    public String generateVerificationCode(String email) {
        try {
            // Check if email exists
            UserEntity user = database.userDao().getUserByEmail(email);
            if (user != null) {
                return generateVerificationCodeForEmail(email);
            } else {
                Log.d(TAG, "Email not found in local database: " + email);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error generating verification code", e);
            return null;
        }
    }

    /**
     * Generate and store a 6-digit verification code for any email
     * (Used for Firebase users or when email existence is checked elsewhere)
     * @param email User email
     * @return 6-digit code as string
     */
    public String generateVerificationCodeForEmail(String email) {
        try {
            // Generate 6-digit code
            String code = generateSixDigitCode();

            // Delete old codes for this email
            database.verificationCodeDao().deleteCodesForEmail(email);

            // Create new verification code entity
            VerificationCodeEntity codeEntity = new VerificationCodeEntity();
            codeEntity.setEmail(email);
            codeEntity.setCode(code);
            OffsetDateTime now = OffsetDateTime.now();
            codeEntity.setCreatedAt(now);
            codeEntity.setExpiresAt(now.plusMinutes(CODE_EXPIRY_MINUTES));
            codeEntity.setUsed(false);

            // Insert into database
            database.verificationCodeDao().insert(codeEntity);

            // Clean up expired codes
            database.verificationCodeDao().deleteExpiredCodes(now);

            Log.d(TAG, "Verification code generated for: " + email);
            return code;
        } catch (Exception e) {
            Log.e(TAG, "Error generating verification code", e);
            return null;
        }
    }

    /**
     * Verify the code entered by user
     * @param email User email
     * @param code 6-digit code
     * @return true if valid and not expired, false otherwise
     */
    public boolean verifyCode(String email, String code) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            VerificationCodeEntity codeEntity = database.verificationCodeDao()
                    .verifyCode(email, code, now);

            if (codeEntity != null) {
                // Mark code as used
                database.verificationCodeDao().markAsUsed(codeEntity.getId());
                Log.d(TAG, "Code verified successfully for: " + email);
                return true;
            } else {
                Log.d(TAG, "Invalid or expired code for: " + email);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error verifying code", e);
            return false;
        }
    }

    /**
     * Check if code is expired
     * @param email User email
     * @return true if expired, false otherwise
     */
    public boolean isCodeExpired(String email) {
        try {
            OffsetDateTime now = OffsetDateTime.now();
            VerificationCodeEntity codeEntity = database.verificationCodeDao()
                    .getLatestValidCode(email, now);
            return codeEntity == null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking code expiration", e);
            return true;
        }
    }

    /**
     * Generate a random 6-digit code
     */
    private String generateSixDigitCode() {
        int code = 100000 + random.nextInt(900000); // Range: 100000-999999
        return String.valueOf(code);
    }

    /**
     * Send verification code via email
     * Currently logs and shows in console for testing
     * In production, integrate with email service (SMTP, SendGrid, Firebase Cloud Functions, etc.)
     */
    public void sendVerificationEmail(String email, String code) {
        // Log the code for testing purposes
        Log.d(TAG, "═══════════════════════════════════════");
        Log.d(TAG, "PASSWORD RESET CODE");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "6-Digit Code: " + code);
        Log.d(TAG, "Code expires in 5 minutes");
        Log.d(TAG, "═══════════════════════════════════════");
        
        // TODO: In production, integrate with one of these:
        // 1. Firebase Cloud Functions + SendGrid
        // 2. Firebase Cloud Functions + Nodemailer
        // 3. Backend API with SMTP
        // 4. Third-party service (SendGrid, Mailgun, etc.)
        
        // Example implementation with Firebase Cloud Functions:
        // FirebaseFunctions.getInstance()
        //     .getHttpsCallable("sendPasswordResetEmail")
        //     .call(Map.of("email", email, "code", code));
    }
}

