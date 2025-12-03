package com.loretacafe.pos.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.R;
import com.loretacafe.pos.data.local.entity.ShiftEntity;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ShiftHistoryAdapter extends RecyclerView.Adapter<ShiftHistoryAdapter.ViewHolder> {
    
    private final List<ShiftEntity> shifts;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.getDefault());
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault());
    
    public ShiftHistoryAdapter(List<ShiftEntity> shifts) {
        this.shifts = shifts;
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
        
        // Date
        holder.tvDate.setText(shift.getClockInTime().format(dateFormatter));
        
        // Clock in/out times
        String clockIn = shift.getClockInTime().format(timeFormatter);
        String clockOut = shift.getClockOutTime() != null ? 
            shift.getClockOutTime().format(timeFormatter) : "In Progress";
        holder.tvTime.setText(clockIn + " - " + clockOut);
        
        // Duration
        if (shift.getDurationMinutes() != null) {
            int hours = shift.getDurationMinutes() / 60;
            int minutes = shift.getDurationMinutes() % 60;
            holder.tvDuration.setText(String.format(Locale.getDefault(), "%d hrs %d min", hours, minutes));
        } else {
            holder.tvDuration.setText("Active");
        }
    }
    
    @Override
    public int getItemCount() {
        return shifts.size();
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate;
        TextView tvTime;
        TextView tvDuration;
        
        ViewHolder(View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvShiftDate);
            tvTime = itemView.findViewById(R.id.tvShiftTime);
            tvDuration = itemView.findViewById(R.id.tvShiftDuration);
        }
    }
}






