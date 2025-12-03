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
import com.loretacafe.pos.data.local.dao.SaleDao;
import com.loretacafe.pos.data.local.dao.ShiftDao;
import com.loretacafe.pos.data.local.dao.UserDao;
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.entity.ShiftEntity;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.session.SessionManager;
import com.loretacafe.pos.security.PermissionManager;

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
    private SaleDao saleDao;
    private UserDao userDao;
    private Handler handler;
    
    private ShiftEntity currentShift;
    private ShiftHistoryAdapter adapter;
    private List<ShiftEntity> shiftHistory = new ArrayList<>();
    private boolean isAdminView = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_management);
        
        sessionManager = new SessionManager(this);
        database = AppDatabase.getInstance(this);
        shiftDao = database.shiftDao();
        saleDao = database.saleDao();
        userDao = database.userDao();
        handler = new Handler(Looper.getMainLooper());
        
        // Check if admin - show all cashiers' shifts
        PermissionManager permissionManager = new PermissionManager(this);
        isAdminView = permissionManager.hasPermission(PermissionManager.Permission.VIEW_SALES_REPORTS);
        
        initializeViews();
        setupListeners();
        
        if (isAdminView) {
            loadAllCashiersShifts(); // Admin view - show all cashiers
        } else {
            loadCurrentShift(); // Cashier view - show own shifts
            loadShiftHistory();
        }
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
        adapter = new ShiftHistoryAdapter(shiftHistory, database);
        rvShiftHistory.setAdapter(adapter);
        
        // Update title for admin view
        if (isAdminView) {
            TextView tvTitle = findViewById(R.id.tvTitle);
            if (tvTitle != null) {
                tvTitle.setText("All Cashiers' Shifts");
            }
        }
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
    
    /**
     * Load all cashiers' completed shifts (Admin view)
     */
    private void loadAllCashiersShifts() {
        new Thread(() -> {
            // Get all completed shifts (clocked out)
            List<ShiftEntity> allShifts = shiftDao.getAllShifts();
            List<ShiftEntity> completedShifts = new ArrayList<>();
            
            for (ShiftEntity shift : allShifts) {
                if (!shift.isActive() && shift.getClockOutTime() != null) {
                    // Fetch user name if not set
                    if (shift.getUserName() == null || shift.getUserName().isEmpty() || 
                        shift.getUserName().startsWith("User #")) {
                        UserEntity user = userDao.getUserById(shift.getUserId());
                        if (user != null) {
                            shift.setUserName(user.getName() != null ? user.getName() : user.getEmail());
                        }
                    }
                    completedShifts.add(shift);
                }
            }
            
            // Sort by clock out time (most recent first)
            completedShifts.sort((a, b) -> b.getClockOutTime().compareTo(a.getClockOutTime()));
            
            handler.post(() -> {
                shiftHistory.clear();
                shiftHistory.addAll(completedShifts);
                adapter.notifyDataSetChanged();
            });
        }).start();
    }
    
    /**
     * Calculate total sales for a shift
     */
    private double calculateShiftSales(ShiftEntity shift) {
        if (shift.getClockOutTime() == null) {
            return 0.0; // Active shift, no sales yet
        }
        
        // Get all sales by this cashier between clock in and clock out
        List<SaleEntity> sales = saleDao.getSalesByDateRange(
            shift.getClockInTime(),
            shift.getClockOutTime()
        );
        
        // Filter by cashier ID
        double total = 0.0;
        for (SaleEntity sale : sales) {
            if (sale.getCashierId() == shift.getUserId()) {
                total += sale.getTotalAmount() != null ? sale.getTotalAmount().doubleValue() : 0;
            }
        }
        return total;
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
                
                // Fetch user name from database
                UserEntity user = userDao.getUserById(userId);
                if (user != null) {
                    shift.setUserName(user.getName() != null ? user.getName() : user.getEmail());
                    shift.setUserEmail(user.getEmail());
                } else {
                    shift.setUserName("User #" + userId);
                    shift.setUserEmail("");
                }
                
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
                
                // Calculate and save total sales for this shift
                double shiftSales = calculateShiftSales(currentShift);
                // Store sales in notes field for easy retrieval
                currentShift.setNotes(String.format(Locale.getDefault(), "%.2f", shiftSales));
                
                // Update user name if not set
                if (currentShift.getUserName() == null || currentShift.getUserName().isEmpty() || 
                    currentShift.getUserName().startsWith("User #")) {
                    UserEntity user = userDao.getUserById(currentShift.getUserId());
                    if (user != null) {
                        currentShift.setUserName(user.getName() != null ? user.getName() : user.getEmail());
                    }
                }
                
                shiftDao.update(currentShift);
                
                handler.post(() -> {
                    long hours = duration.toHours();
                    long minutes = duration.toMinutes() % 60;
                    
                    Toast.makeText(this, 
                        String.format(Locale.getDefault(), 
                            "✅ Clocked out! Shift: %dh %dm | Sales: ₱ %,.2f", 
                            hours, minutes, shiftSales), 
                        Toast.LENGTH_LONG).show();
                    
                    currentShift = null;
                    updateShiftUI();
                    if (isAdminView) {
                        loadAllCashiersShifts();
                    } else {
                        loadShiftHistory();
                    }
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






