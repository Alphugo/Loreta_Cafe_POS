package com.loretacafe.pos.printer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import com.loretacafe.pos.CartItem;
import com.loretacafe.pos.Order;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * PrinterHelper - Bluetooth Thermal Printer Integration
 * Supports 58mm and 80mm ESC/POS printers
 */
public class PrinterHelper {
    private static final String TAG = "PrinterHelper";
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // Printer preferences
    private static final String PREFS_NAME = "PrinterPrefs";
    private static final String KEY_PRINTER_MAC = "printer_mac_address";
    private static final String KEY_PRINTER_NAME = "printer_name";
    private static final String KEY_PAPER_WIDTH = "paper_width"; // 58 or 80
    
    // ESC/POS Commands
    private static final byte[] ESC_INIT = {0x1B, 0x40}; // Initialize printer
    private static final byte[] ESC_ALIGN_LEFT = {0x1B, 0x61, 0x00};
    private static final byte[] ESC_ALIGN_CENTER = {0x1B, 0x61, 0x01};
    private static final byte[] ESC_ALIGN_RIGHT = {0x1B, 0x61, 0x02};
    private static final byte[] ESC_BOLD_ON = {0x1B, 0x45, 0x01};
    private static final byte[] ESC_BOLD_OFF = {0x1B, 0x45, 0x00};
    private static final byte[] ESC_DOUBLE_ON = {0x1D, 0x21, 0x11}; // Double height & width
    private static final byte[] ESC_DOUBLE_OFF = {0x1D, 0x21, 0x00};
    private static final byte[] ESC_FEED_LINE = {0x0A};
    private static final byte[] ESC_FEED_PAPER = {0x1B, 0x64, 0x02}; // Feed 2 lines
    private static final byte[] ESC_CUT_PAPER = {0x1D, 0x56, 0x00}; // Full cut
    private static final byte[] ESC_CUT_PAPER_PARTIAL = {0x1D, 0x56, 0x01}; // Partial cut
    
    private Context context;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private boolean isConnected = false;
    
    public PrinterHelper(Context context) {
        this.context = context;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    /**
     * Check if Bluetooth is available and enabled
     */
    public boolean isBluetoothAvailable() {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }
    
    /**
     * Get list of paired Bluetooth devices
     */
    public List<BluetoothDevice> getPairedPrinters() {
        List<BluetoothDevice> printers = new ArrayList<>();
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            try {
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (pairedDevices != null) {
                    for (BluetoothDevice device : pairedDevices) {
                        // Filter for printer devices (optional - add all paired devices)
                        printers.add(device);
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception getting paired devices", e);
            }
        }
        return printers;
    }
    
    /**
     * Save printer preferences
     */
    public void savePrinterPreferences(String macAddress, String printerName, int paperWidth) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(KEY_PRINTER_MAC, macAddress)
            .putString(KEY_PRINTER_NAME, printerName)
            .putInt(KEY_PAPER_WIDTH, paperWidth)
            .apply();
        Log.d(TAG, "Saved printer: " + printerName + " (" + macAddress + ")");
    }
    
    /**
     * Get saved printer MAC address
     */
    public String getSavedPrinterMac() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PRINTER_MAC, null);
    }
    
    /**
     * Get saved printer name
     */
    public String getSavedPrinterName() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PRINTER_NAME, "No printer selected");
    }
    
    /**
     * Get paper width (58 or 80mm)
     */
    public int getPaperWidth() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_PAPER_WIDTH, 58); // Default 58mm
    }
    
    /**
     * Connect to printer by MAC address
     */
    public boolean connectPrinter(String macAddress) {
        if (!isBluetoothAvailable()) {
            Log.e(TAG, "Bluetooth not available");
            return false;
        }
        
        try {
            // Close existing connection
            disconnect();
            
            // Get device by MAC address
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(macAddress);
            
            // Create socket
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID);
            
            // Connect
            bluetoothAdapter.cancelDiscovery();
            bluetoothSocket.connect();
            
            // Get output stream
            outputStream = bluetoothSocket.getOutputStream();
            isConnected = true;
            
            // Initialize printer
            sendBytes(ESC_INIT);
            
            Log.d(TAG, "Connected to printer: " + device.getName());
            return true;
            
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception connecting to printer", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "IO exception connecting to printer", e);
            disconnect();
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error connecting to printer", e);
            disconnect();
            return false;
        }
    }
    
    /**
     * Auto-connect to last saved printer
     */
    public boolean autoConnect() {
        String macAddress = getSavedPrinterMac();
        if (macAddress != null && !macAddress.isEmpty()) {
            return connectPrinter(macAddress);
        }
        return false;
    }
    
    /**
     * Check if printer is connected
     */
    public boolean isConnected() {
        return isConnected && bluetoothSocket != null && bluetoothSocket.isConnected();
    }
    
    /**
     * Disconnect from printer
     */
    public void disconnect() {
        try {
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
                bluetoothSocket = null;
            }
            isConnected = false;
        } catch (IOException e) {
            Log.e(TAG, "Error disconnecting", e);
        }
    }
    
    /**
     * Send raw bytes to printer
     */
    private void sendBytes(byte[] bytes) throws IOException {
        if (outputStream != null) {
            outputStream.write(bytes);
            outputStream.flush();
        }
    }
    
    /**
     * Print text
     */
    public void printText(String text) throws IOException {
        sendBytes(text.getBytes(Charset.forName("UTF-8")));
    }
    
    /**
     * Print line (text + newline)
     */
    public void printLine(String text) throws IOException {
        printText(text);
        sendBytes(ESC_FEED_LINE);
    }
    
    /**
     * Print centered text
     */
    public void printCentered(String text) throws IOException {
        sendBytes(ESC_ALIGN_CENTER);
        printLine(text);
        sendBytes(ESC_ALIGN_LEFT);
    }
    
    /**
     * Print right-aligned text
     */
    public void printRight(String text) throws IOException {
        sendBytes(ESC_ALIGN_RIGHT);
        printLine(text);
        sendBytes(ESC_ALIGN_LEFT);
    }
    
    /**
     * Print bold text
     */
    public void printBold(String text) throws IOException {
        sendBytes(ESC_BOLD_ON);
        printLine(text);
        sendBytes(ESC_BOLD_OFF);
    }
    
    /**
     * Print large text (double size)
     */
    public void printLarge(String text) throws IOException {
        sendBytes(ESC_DOUBLE_ON);
        printLine(text);
        sendBytes(ESC_DOUBLE_OFF);
    }
    
    /**
     * Print centered large text
     */
    public void printCenteredLarge(String text) throws IOException {
        sendBytes(ESC_ALIGN_CENTER);
        sendBytes(ESC_DOUBLE_ON);
        printLine(text);
        sendBytes(ESC_DOUBLE_OFF);
        sendBytes(ESC_ALIGN_LEFT);
    }
    
    /**
     * Print dashed line separator
     */
    public void printDashedLine() throws IOException {
        int width = getPaperWidth();
        int chars = (width == 58) ? 32 : 48;
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < chars; i++) {
            line.append("-");
        }
        printLine(line.toString());
    }
    
    /**
     * Print two-column text (left-aligned and right-aligned)
     */
    public void printTwoColumn(String left, String right) throws IOException {
        int width = getPaperWidth();
        int totalChars = (width == 58) ? 32 : 48;
        
        int leftLen = left.length();
        int rightLen = right.length();
        int spaces = totalChars - leftLen - rightLen;
        
        if (spaces < 1) {
            // If text too long, truncate left text
            int maxLeft = totalChars - rightLen - 1;
            if (maxLeft > 0) {
                left = left.substring(0, maxLeft);
                spaces = 1;
            } else {
                // Just print on separate lines
                printLine(left);
                printRight(right);
                return;
            }
        }
        
        StringBuilder line = new StringBuilder(left);
        for (int i = 0; i < spaces; i++) {
            line.append(" ");
        }
        line.append(right);
        printLine(line.toString());
    }
    
    /**
     * Print three-column text (for item details)
     */
    public void printThreeColumn(String col1, String col2, String col3) throws IOException {
        int width = getPaperWidth();
        int totalChars = (width == 58) ? 32 : 48;
        
        // Allocate space: col1(50%), col2(20%), col3(30%)
        int col1Width = (int)(totalChars * 0.5);
        int col2Width = (int)(totalChars * 0.2);
        int col3Width = totalChars - col1Width - col2Width;
        
        String formatted = String.format("%-" + col1Width + "s%" + col2Width + "s%" + col3Width + "s", 
            truncate(col1, col1Width), 
            truncate(col2, col2Width), 
            truncate(col3, col3Width));
        printLine(formatted);
    }
    
    /**
     * Truncate string to fit width
     */
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 2) + "..";
    }
    
    /**
     * Feed paper (add blank lines)
     */
    public void feedPaper(int lines) throws IOException {
        for (int i = 0; i < lines; i++) {
            sendBytes(ESC_FEED_LINE);
        }
    }
    
    /**
     * Cut paper
     */
    public void cutPaper() throws IOException {
        feedPaper(3); // Feed before cutting
        sendBytes(ESC_CUT_PAPER_PARTIAL);
    }
    
    /**
     * Print complete receipt for an order
     */
    public boolean printReceipt(Order order) {
        try {
            // Initialize printer
            sendBytes(ESC_INIT);
            
            // Header - Store name
            sendBytes(ESC_ALIGN_CENTER);
            sendBytes(ESC_BOLD_ON);
            sendBytes(ESC_DOUBLE_ON);
            printLine("Loreta's Cafe");
            sendBytes(ESC_DOUBLE_OFF);
            sendBytes(ESC_BOLD_OFF);
            
            // Store address
            printLine("Rainbow Avenue");
            printLine("Rainbow Village 5 Phase 1");
            printLine("Your Cozy Corner in Town ♡");
            sendBytes(ESC_ALIGN_LEFT);
            
            feedPaper(1);
            
            // Date and time
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy | hh:mm a", Locale.getDefault());
            String dateTime = dateFormat.format(new Date());
            printCentered(dateTime);
            
            // Staff name
            printCentered("Loreta's Cafe Staff");
            
            feedPaper(1);
            
            // Order number
            if (order.getOrderId() != null && !order.getOrderId().isEmpty()) {
                printTwoColumn("Invoice No:", order.getOrderId());
            }
            
            // Customer name
            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                printTwoColumn("Customer:", order.getCustomerName());
            }
            
            printDashedLine();
            
            // Column headers
            int width = getPaperWidth();
            if (width == 58) {
                printLine("Item          Qty    Amount");
            } else {
                printLine("Item                    Qty    Price    Amount");
            }
            printDashedLine();
            
            // Items
            double subtotal = 0.0;
            for (CartItem item : order.getItems()) {
                String itemName = item.getProductName();
                if (itemName.length() > 15 && width == 58) {
                    itemName = itemName.substring(0, 15);
                } else if (itemName.length() > 20 && width == 80) {
                    itemName = itemName.substring(0, 20);
                }
                
                String size = item.getSelectedSize();
                if (size != null && !size.isEmpty() && !size.equals("N/A")) {
                    itemName = itemName + " (" + size + ")";
                }
                
                int qty = item.getQuantity();
                double price = item.getUnitPrice();
                double amount = item.getTotalPrice();
                subtotal += amount;
                
                if (width == 58) {
                    // 58mm format
                    printLine(String.format("%-14s %2d %8s", 
                        truncate(itemName, 14), 
                        qty, 
                        formatCurrency(amount)));
                } else {
                    // 80mm format
                    printLine(String.format("%-20s %3d %8s %8s", 
                        truncate(itemName, 20), 
                        qty,
                        formatCurrency(price),
                        formatCurrency(amount)));
                }
            }
            
            printDashedLine();
            
            // Totals
            sendBytes(ESC_BOLD_ON);
            printTwoColumn("Subtotal:", formatCurrency(subtotal));
            
            // Discount (if any)
            double discount = 0.0;
            if (discount > 0) {
                printTwoColumn("Discount:", formatCurrency(discount));
            }
            
            // Tax (if any)
            double tax = 0.0;
            if (tax > 0) {
                printTwoColumn("Tax:", formatCurrency(tax));
            }
            
            // Total
            double total = subtotal - discount + tax;
            sendBytes(ESC_DOUBLE_ON);
            printTwoColumn("TOTAL:", formatCurrency(total));
            sendBytes(ESC_DOUBLE_OFF);
            sendBytes(ESC_BOLD_OFF);
            
            printDashedLine();
            
            // Payment details
            String paymentMethod = order.getPaymentMethod();
            printTwoColumn("Payment Method:", paymentMethod);
            
            if ("Cash".equalsIgnoreCase(paymentMethod)) {
                printTwoColumn("Cash Received:", formatCurrency(order.getCashReceived()));
                double change = order.getChange();
                if (change > 0) {
                    sendBytes(ESC_BOLD_ON);
                    printTwoColumn("Change:", formatCurrency(change));
                    sendBytes(ESC_BOLD_OFF);
                }
            }
            
            printDashedLine();
            
            feedPaper(2);
            
            // Thank you message
            sendBytes(ESC_ALIGN_CENTER);
            printLine("Thank you for your purchase!");
            printLine("Please come again! ♡");
            sendBytes(ESC_ALIGN_LEFT);
            
            feedPaper(2);
            
            // Cut paper
            cutPaper();
            
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error printing receipt", e);
            return false;
        }
    }
    
    /**
     * Print test receipt
     */
    public boolean printTestReceipt() {
        try {
            sendBytes(ESC_INIT);
            
            sendBytes(ESC_ALIGN_CENTER);
            sendBytes(ESC_BOLD_ON);
            sendBytes(ESC_DOUBLE_ON);
            printLine("TEST RECEIPT");
            sendBytes(ESC_DOUBLE_OFF);
            sendBytes(ESC_BOLD_OFF);
            
            printLine("Loreta's Cafe");
            printLine("POS System");
            sendBytes(ESC_ALIGN_LEFT);
            
            feedPaper(1);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault());
            printCentered(dateFormat.format(new Date()));
            
            feedPaper(1);
            printDashedLine();
            
            printLine("Printer: " + getSavedPrinterName());
            printLine("Paper Width: " + getPaperWidth() + "mm");
            printLine("Status: Connected");
            
            printDashedLine();
            
            feedPaper(1);
            printCentered("Test completed successfully!");
            
            feedPaper(2);
            cutPaper();
            
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Error printing test receipt", e);
            return false;
        }
    }
    
    /**
     * Format currency (Philippine Peso)
     */
    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "₱%.2f", amount);
    }
}

