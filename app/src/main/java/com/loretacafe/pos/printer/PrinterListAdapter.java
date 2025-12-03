package com.loretacafe.pos.printer;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.R;

import java.util.List;

public class PrinterListAdapter extends RecyclerView.Adapter<PrinterListAdapter.ViewHolder> {
    
    private List<BluetoothDevice> devices;
    private int selectedPosition = -1;
    
    public PrinterListAdapter(List<BluetoothDevice> devices) {
        this.devices = devices;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_printer, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BluetoothDevice device = devices.get(position);
        
        try {
            String deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = "Unknown Device";
            }
            holder.tvPrinterName.setText(deviceName);
            holder.tvPrinterAddress.setText(device.getAddress());
        } catch (SecurityException e) {
            holder.tvPrinterName.setText("Unknown Device");
            holder.tvPrinterAddress.setText("Permission Required");
        }
        
        holder.radioButton.setChecked(position == selectedPosition);
        
        holder.itemView.setOnClickListener(v -> {
            int previousPosition = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousPosition);
            notifyItemChanged(selectedPosition);
        });
    }
    
    @Override
    public int getItemCount() {
        return devices.size();
    }
    
    public BluetoothDevice getSelectedDevice() {
        if (selectedPosition >= 0 && selectedPosition < devices.size()) {
            return devices.get(selectedPosition);
        }
        return null;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        RadioButton radioButton;
        TextView tvPrinterName;
        TextView tvPrinterAddress;
        
        ViewHolder(View itemView) {
            super(itemView);
            radioButton = itemView.findViewById(R.id.radioButton);
            tvPrinterName = itemView.findViewById(R.id.tvPrinterName);
            tvPrinterAddress = itemView.findViewById(R.id.tvPrinterAddress);
        }
    }
}

