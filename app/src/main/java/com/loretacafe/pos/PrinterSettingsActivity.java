package com.loretacafe.pos;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.loretacafe.pos.printer.PrinterHelper;
import com.loretacafe.pos.printer.PrinterListAdapter;
import com.loretacafe.pos.security.PermissionManager;

import java.util.ArrayList;
import java.util.List;

public class PrinterSettingsActivity extends AppCompatActivity {
    
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 100;
    private static final int REQUEST_ENABLE_BT = 101;
    
    private TextView tvCurrentPrinter;
    private TextView tvPrinterStatus;
    private Button btnSelectPrinter;
    private Button btnTestPrint;
    private Button btnDisconnect;
    private ProgressBar progressBar;
    
    private PrinterHelper printerHelper;
    private Handler handler;
    
    // Permission launcher for Android 12+
    private ActivityResultLauncher<String[]> bluetoothPermissionLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check admin permission - only admins can configure printers
        PermissionManager permissionManager = new PermissionManager(this);
        if (!permissionManager.checkPermissionOrFinish(this, PermissionManager.Permission.CONFIGURE_SETTINGS)) {
            return;
        }
        
        setContentView(R.layout.activity_printer_settings);
        
        handler = new Handler(Looper.getMainLooper());
        printerHelper = new PrinterHelper(this);
        
        initializeViews();
        setupPermissionLauncher();
        setupListeners();
        updatePrinterStatus();
        
        // Check permissions on start
        checkBluetoothPermissions();
    }
    
    private void initializeViews() {
        tvCurrentPrinter = findViewById(R.id.tvCurrentPrinter);
        tvPrinterStatus = findViewById(R.id.tvPrinterStatus);
        btnSelectPrinter = findViewById(R.id.btnSelectPrinter);
        btnTestPrint = findViewById(R.id.btnTestPrint);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        progressBar = findViewById(R.id.progressBar);
        
        progressBar.setVisibility(View.GONE);
    }
    
    private void setupPermissionLauncher() {
        bluetoothPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean connectGranted = result.get(Manifest.permission.BLUETOOTH_CONNECT);
                Boolean scanGranted = result.get(Manifest.permission.BLUETOOTH_SCAN);
                
                if (Boolean.TRUE.equals(connectGranted) && Boolean.TRUE.equals(scanGranted)) {
                    Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show();
                    updatePrinterStatus();
                } else {
                    Toast.makeText(this, "Bluetooth permissions required for printing", Toast.LENGTH_LONG).show();
                }
            }
        );
    }
    
    private void setupListeners() {
        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        // Select printer
        btnSelectPrinter.setOnClickListener(v -> showPrinterSelectionDialog());
        
        // Test print
        btnTestPrint.setOnClickListener(v -> testPrint());
        
        // Disconnect
        btnDisconnect.setOnClickListener(v -> {
            printerHelper.disconnect();
            updatePrinterStatus();
            Toast.makeText(this, "Printer disconnected", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void checkBluetoothPermissions() {
        if (!printerHelper.isBluetoothAvailable()) {
            // Bluetooth not supported or not enabled
            if (BluetoothAdapter.getDefaultAdapter() == null) {
                Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_LONG).show();
                btnSelectPrinter.setEnabled(false);
                btnTestPrint.setEnabled(false);
                return;
            }
            
            // Bluetooth not enabled - request to enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            try {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } catch (SecurityException e) {
                requestBluetoothPermissions();
            }
            return;
        }
        
        // Check runtime permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                != PackageManager.PERMISSION_GRANTED) {
                requestBluetoothPermissions();
                return;
            }
        } else {
            // Android 11 and below
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) 
                != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                    REQUEST_BLUETOOTH_PERMISSIONS);
                return;
            }
        }
    }
    
    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            bluetoothPermissionLauncher.launch(new String[]{
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            });
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN},
                REQUEST_BLUETOOTH_PERMISSIONS);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permissions granted", Toast.LENGTH_SHORT).show();
                updatePrinterStatus();
            } else {
                Toast.makeText(this, "Bluetooth permissions required for printing", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                checkBluetoothPermissions();
            } else {
                Toast.makeText(this, "Bluetooth is required for printing", Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void updatePrinterStatus() {
        String printerName = printerHelper.getSavedPrinterName();
        tvCurrentPrinter.setText(printerName);
        
        if (printerHelper.isConnected()) {
            tvPrinterStatus.setText("Connected");
            tvPrinterStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnTestPrint.setEnabled(true);
            btnDisconnect.setEnabled(true);
        } else {
            String macAddress = printerHelper.getSavedPrinterMac();
            if (macAddress != null && !macAddress.isEmpty()) {
                tvPrinterStatus.setText("Not connected");
                tvPrinterStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
                btnDisconnect.setEnabled(false);
            } else {
                tvPrinterStatus.setText("No printer selected");
                tvPrinterStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray));
                btnDisconnect.setEnabled(false);
            }
            btnTestPrint.setEnabled(false);
        }
    }
    
    private void showPrinterSelectionDialog() {
        if (!printerHelper.isBluetoothAvailable()) {
            Toast.makeText(this, "Please enable Bluetooth first", Toast.LENGTH_SHORT).show();
            checkBluetoothPermissions();
            return;
        }
        
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        btnSelectPrinter.setEnabled(false);
        
        new Thread(() -> {
            try {
                // Get paired devices
                List<BluetoothDevice> devices = printerHelper.getPairedPrinters();
                
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSelectPrinter.setEnabled(true);
                    
                    if (devices.isEmpty()) {
                        Toast.makeText(this, "No paired Bluetooth devices found. Please pair your printer first.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    showPrinterListDialog(devices);
                });
            } catch (SecurityException e) {
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSelectPrinter.setEnabled(true);
                    Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                    requestBluetoothPermissions();
                });
            }
        }).start();
    }
    
    private void showPrinterListDialog(List<BluetoothDevice> devices) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_printer_list, null);
        
        RecyclerView recyclerView = dialogView.findViewById(R.id.rvPrinters);
        RadioGroup rgPaperWidth = dialogView.findViewById(R.id.rgPaperWidth);
        Button btnConnect = dialogView.findViewById(R.id.btnConnect);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // Set current paper width
        int currentWidth = printerHelper.getPaperWidth();
        if (currentWidth == 80) {
            ((RadioButton) rgPaperWidth.findViewById(R.id.rb80mm)).setChecked(true);
        } else {
            ((RadioButton) rgPaperWidth.findViewById(R.id.rb58mm)).setChecked(true);
        }
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PrinterListAdapter adapter = new PrinterListAdapter(devices);
        recyclerView.setAdapter(adapter);
        
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        
        btnConnect.setOnClickListener(v -> {
            BluetoothDevice selectedDevice = adapter.getSelectedDevice();
            if (selectedDevice == null) {
                Toast.makeText(this, "Please select a printer", Toast.LENGTH_SHORT).show();
                return;
            }
            
            int paperWidth = rgPaperWidth.getCheckedRadioButtonId() == R.id.rb80mm ? 80 : 58;
            
            dialog.dismiss();
            connectToPrinter(selectedDevice, paperWidth);
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }
    
    private void connectToPrinter(BluetoothDevice device, int paperWidth) {
        progressBar.setVisibility(View.VISIBLE);
        btnSelectPrinter.setEnabled(false);
        tvPrinterStatus.setText("Connecting...");
        
        new Thread(() -> {
            try {
                String deviceName = device.getName();
                String macAddress = device.getAddress();
                
                boolean connected = printerHelper.connectPrinter(macAddress);
                
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSelectPrinter.setEnabled(true);
                    
                    if (connected) {
                        // Save printer preferences
                        printerHelper.savePrinterPreferences(macAddress, deviceName, paperWidth);
                        Toast.makeText(this, "Connected to " + deviceName, Toast.LENGTH_SHORT).show();
                        updatePrinterStatus();
                    } else {
                        tvPrinterStatus.setText("Connection failed");
                        Toast.makeText(this, "Failed to connect to printer", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (SecurityException e) {
                handler.post(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSelectPrinter.setEnabled(true);
                    Toast.makeText(this, "Bluetooth permission required", Toast.LENGTH_SHORT).show();
                    requestBluetoothPermissions();
                });
            }
        }).start();
    }
    
    private void testPrint() {
        if (!printerHelper.isConnected()) {
            Toast.makeText(this, "Printer not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        
        progressBar.setVisibility(View.VISIBLE);
        btnTestPrint.setEnabled(false);
        
        new Thread(() -> {
            boolean success = printerHelper.printTestReceipt();
            
            handler.post(() -> {
                progressBar.setVisibility(View.GONE);
                btnTestPrint.setEnabled(true);
                
                if (success) {
                    Toast.makeText(this, "Test receipt printed successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to print test receipt", Toast.LENGTH_LONG).show();
                    printerHelper.disconnect();
                    updatePrinterStatus();
                }
            });
        }).start();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Don't disconnect on destroy - keep connection for printing
    }
}

