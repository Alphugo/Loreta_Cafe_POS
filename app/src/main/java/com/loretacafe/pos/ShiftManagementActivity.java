package com.loretacafe.pos;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.adapter.ShiftHistoryAdapter;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.ShiftDao;
import com.loretacafe.pos.data.local.entity.ShiftEntity;
import com.loretacafe.pos.data.session.SessionManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shift Management for cashiers to clock in/out
 * Tracks work hours and shift history
 */
public class ShiftManagementActivity extends AppCompatActivity {
    
    private TextView tvCurrentStatus;
    private TextView tvClockInTime;
    private TextView tvDuration;
    private Button btnClockIn;
    private Button btnClockOut;
    private CardView cardCurrentShift;
    private RecyclerView rvShiftHistory;
    
    private SessionManager sessionManager;
    private AppDatabase database;
    private ShiftDao shiftDao;
    private Handler handler;
    
    private ShiftEntity currentShift;
    private ShiftHistoryAdapter adapter;
    private List<ShiftEntity> shiftHistory = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_management);
        
        sessionManager = new SessionManager(this);
        database = AppDatabase.getInstance(this);
        shiftDao = database.shiftDao();
        handler = new Handler(Looper.getMainLooper());
        
        initializeViews();
        setupListeners();
        loadCurrentShift();
        loadShiftHistory();
    }
    
    private void initializeViews() {
        ImageButton btnBack = findViewById(R.id.btnBack);
        tvCurrentStatus = findViewById(R.id.tvCurrentStatus);
        tvClockInTime = findViewById(R.id.tvClockInTime);
        tvDuration = findViewById(R.id.tvDuration);
        btnClockIn = findViewById(R.id.btnClockIn);
        btnClockOut = findViewById(R.id.btnClockOut);
        cardCurrentShift = findViewById(R.id.cardCurrentShift);
        rvShiftHistory = findViewById(R.id.rvShiftHistory);
        
        btnBack.setOnClickListener(v -> finish());
        
        // Setup RecyclerView
        rvShiftHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ShiftHistoryAdapter(shiftHistory);
        rvShiftHistory.setAdapter(adapter);
    }
    
    private void setupListeners() {
        btnClockIn.setOnClickListener(v -> clockIn());
        btnClockOut.setOnClickListener(v -> clockOut());
    }
    
    private void loadCurrentShift() {
        new Thread(() -> {
            long userId = sessionManager.getUserId();
            ShiftEntity activeShift = shiftDao.getActiveShiftByUser(userId);
            
            handler.post(() -> {
                currentShift = activeShift;
                updateShiftUI();
            });
        }).start();
    }
    
    private void loadShiftHistory() {
        new Thread(() -> {
            long userId = sessionManager.getUserId();
            List<ShiftEntity> shifts = shiftDao.getShiftsByUser(userId);
            
            handler.post(() -> {
                shiftHistory.clear();
                // Show only completed shifts (exclude active shift)
                for (ShiftEntity shift : shifts) {
                    if (!shift.isActive()) {
                        shiftHistory.add(shift);
                    }
                }
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
    
    private void updateShiftUI() {
        if (currentShift != null && currentShift.isActive()) {
            // Shift is active
            cardCurrentShift.setVisibility(View.VISIBLE);
            btnClockIn.setVisibility(View.GONE);
            btnClockOut.setVisibility(View.VISIBLE);
            
            tvCurrentStatus.setText("✅ Currently Clocked In");
            tvCurrentStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
            
            // Format clock in time
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault());
            tvClockInTime.setText("Clocked in at: " + currentShift.getClockInTime().format(timeFormatter));
            
            // Calculate and show duration
            updateDuration();
            
        } else {
            // No active shift
            cardCurrentShift.setVisibility(View.GONE);
            btnClockIn.setVisibility(View.VISIBLE);
            btnClockOut.setVisibility(View.GONE);
        }
    }
    
    private void updateDuration() {
        if (currentShift != null && currentShift.isActive()) {
            Duration duration = Duration.between(currentShift.getClockInTime(), OffsetDateTime.now());
            long hours = duration.toHours();
            long minutes = duration.toMinutes() % 60;
            
            tvDuration.setText(String.format(Locale.getDefault(), 
                "Duration: %d hours %d minutes", hours, minutes));
            
            // Update every minute
            handler.postDelayed(this::updateDuration, 60000);
        }
    }
    
    private void clockIn() {
        new Thread(() -> {
            try {
                long userId = sessionManager.getUserId();
                
                // Check if already clocked in
                int activeCount = shiftDao.getActiveShiftCount(userId);
                if (activeCount > 0) {
                    handler.post(() -> 
                        Toast.makeText(this, "You are already clocked in", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }
                
                // Create new shift
                ShiftEntity shift = new ShiftEntity();
                shift.setUserId(userId);
                shift.setUserName("User #" + userId); // Could fetch from UserDao
                shift.setUserEmail("");
                shift.setClockInTime(OffsetDateTime.now());
                shift.setCreatedAt(OffsetDateTime.now());
                
                long shiftId = shiftDao.insert(shift);
                shift.setId(shiftId);
                
                handler.post(() -> {
                    currentShift = shift;
                    updateShiftUI();
                    Toast.makeText(this, "✅ Clocked in successfully!", Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                handler.post(() -> 
                    Toast.makeText(this, "❌ Error clocking in: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    
    private void clockOut() {
        if (currentShift == null) {
            Toast.makeText(this, "No active shift", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new Thread(() -> {
            try {
                // Update shift with clock out time
                currentShift.setClockOutTime(OffsetDateTime.now());
                
                // Calculate duration in minutes
                Duration duration = Duration.between(currentShift.getClockInTime(), currentShift.getClockOutTime());
                currentShift.setDurationMinutes((int) duration.toMinutes());
                
                shiftDao.update(currentShift);
                
                handler.post(() -> {
                    long hours = duration.toHours();
                    long minutes = duration.toMinutes() % 60;
                    
                    Toast.makeText(this, 
                        String.format(Locale.getDefault(), 
                            "✅ Clocked out! Shift duration: %d hours %d minutes", hours, minutes), 
                        Toast.LENGTH_LONG).show();
                    
                    currentShift = null;
                    updateShiftUI();
                    loadShiftHistory(); // Refresh history
                });
                
            } catch (Exception e) {
                handler.post(() -> 
                    Toast.makeText(this, "❌ Error clocking out: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}






