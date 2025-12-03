package com.loretacafe.pos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.R;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.local.dao.SaleDao;
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.entity.ShiftEntity;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ShiftHistoryAdapter extends RecyclerView.Adapter<ShiftHistoryAdapter.ViewHolder> {
    
    private final List<ShiftEntity> shifts;
    private final SaleDao saleDao;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault());
    
    public ShiftHistoryAdapter(List<ShiftEntity> shifts, AppDatabase database) {
        this.shifts = shifts;
        this.saleDao = database != null ? database.saleDao() : null;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_shift, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShiftEntity shift = shifts.get(position);
        
        // Cashier name
        String cashierName = shift.getUserName() != null && !shift.getUserName().isEmpty() 
            ? shift.getUserName() 
            : "Cashier #" + shift.getUserId();
        holder.tvCashierName.setText(cashierName);
        
        // Clock in/out times in format: "09:00 → 17:00"
        String clockIn = shift.getClockInTime().format(timeFormatter);
        String clockOut = shift.getClockOutTime() != null ? 
            shift.getClockOutTime().format(timeFormatter) : "Active";
        holder.tvTime.setText(clockIn + " → " + clockOut);
        
        // Duration in format: "8h"
        if (shift.getDurationMinutes() != null && shift.getClockOutTime() != null) {
            int hours = shift.getDurationMinutes() / 60;
            holder.tvDuration.setText(String.format(Locale.getDefault(), "%dh", hours));
        } else {
            holder.tvDuration.setText("Active");
        }
        
        // Calculate and display sales for this shift
        calculateAndDisplaySales(holder, shift);
    }
    
    private void calculateAndDisplaySales(@NonNull ViewHolder holder, ShiftEntity shift) {
        if (saleDao == null || shift.getClockOutTime() == null) {
            holder.tvSales.setText("₱ 0.00 sales");
            return;
        }
        
        // Calculate sales in background thread
        new Thread(() -> {
            try {
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
                
                // Store in final variable for lambda
                final double finalTotal = total;
                
                // Update UI on main thread
                holder.itemView.post(() -> {
                    holder.tvSales.setText(String.format(Locale.getDefault(), "₱ %,.2f sales", finalTotal));
                });
            } catch (Exception e) {
                holder.itemView.post(() -> {
                    holder.tvSales.setText("₱ 0.00 sales");
                });
            }
        }).start();
    }
    
    @Override
    public int getItemCount() {
        return shifts.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCashierName;
        TextView tvTime;
        TextView tvDuration;
        TextView tvSales;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvCashierName = itemView.findViewById(R.id.tvShiftCashierName);
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvDuration = itemView.findViewById(R.id.tvShiftDuration);
            tvSales = itemView.findViewById(R.id.tvShiftSales);
        }
    }
}






