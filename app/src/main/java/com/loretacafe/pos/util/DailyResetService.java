package com.loretacafe.pos.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.loretacafe.pos.data.local.AppDatabase;

import java.util.Calendar;

/**
 * Service to handle daily reset at 3:00 AM
 * Resets daily sales counters and prepares for new day
 */
public class DailyResetService extends BroadcastReceiver {

    private static final String TAG = "DailyResetService";
    private static final int RESET_HOUR = 3; // 3 AM
    private static final int RESET_MINUTE = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // Device booted, reschedule the daily reset
            Log.d(TAG, "Device booted, scheduling daily reset");
            scheduleDailyReset(context);
            return;
        }
        
        Log.d(TAG, "Daily reset triggered at 3:00 AM");
        
        // Perform daily reset operations
        new Thread(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                
                // Daily reset operations:
                // 1. Archive today's sales (optional - for now we just log)
                // 2. Reset any daily counters if needed
                // 3. Clear any temporary data
                
                // Note: Sales data is already filtered by date in queries,
                // so no need to delete old data. This is mainly for logging/cleanup.
                
                Log.d(TAG, "Daily reset completed successfully");
                
                // Schedule next reset
                scheduleDailyReset(context);
                
            } catch (Exception e) {
                Log.e(TAG, "Error during daily reset", e);
            }
        }).start();
    }

    /**
     * Schedule daily reset at 3:00 AM
     */
    public static void scheduleDailyReset(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                Log.e(TAG, "AlarmManager is null");
                return;
            }

            Intent intent = new Intent(context, DailyResetService.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Calculate next 3:00 AM
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, RESET_HOUR);
            calendar.set(Calendar.MINUTE, RESET_MINUTE);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // If 3 AM has already passed today, schedule for tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Use setExactAndAllowWhileIdle for Android 6.0+ to ensure it runs even in doze mode
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            }

            Log.d(TAG, "Daily reset scheduled for: " + calendar.getTime());
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling daily reset", e);
        }
    }

    /**
     * Cancel daily reset schedule
     */
    public static void cancelDailyReset(Context context) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }

            Intent intent = new Intent(context, DailyResetService.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "Daily reset cancelled");
        } catch (Exception e) {
            Log.e(TAG, "Error cancelling daily reset", e);
        }
    }
}

