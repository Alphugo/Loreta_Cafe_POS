package com.loretacafe.pos.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BackendDiscovery {
    private static final String TAG = "BackendDiscovery";
    private static final int BACKEND_PORT = 8080;
    private static final int CONNECTION_TIMEOUT = 500; // milliseconds
    private static final int MAX_CONCURRENT_SCANS = 50;
    
    private static ExecutorService executorService = Executors.newFixedThreadPool(MAX_CONCURRENT_SCANS);
    
    /**
     * Automatically discover backend server on the current network
     * Scans common IP ranges and finds the server
     */
    public static CompletableFuture<String> discoverBackend(Context context) {
        CompletableFuture<String> future = new CompletableFuture<>();
        
        executorService.execute(() -> {
            try {
                // Get device's network IP
                String deviceIP = getDeviceIP(context);
                if (deviceIP == null) {
                    Log.w(TAG, "Could not determine device IP");
                    future.complete(null);
                    return;
                }
                
                Log.d(TAG, "Device IP: " + deviceIP);
                
                // Extract network prefix (e.g., 192.168.1 from 192.168.1.100)
                String networkPrefix = extractNetworkPrefix(deviceIP);
                Log.d(TAG, "Network prefix: " + networkPrefix);
                
                // Generate IPs to scan
                List<String> ipsToScan = generateIPsToScan(networkPrefix);
                Log.d(TAG, "Scanning " + ipsToScan.size() + " IPs...");
                
                // Scan IPs concurrently
                String foundServer = scanIPs(ipsToScan);
                
                if (foundServer != null) {
                    Log.i(TAG, "Backend server found at: " + foundServer);
                    future.complete(foundServer);
                } else {
                    Log.w(TAG, "Backend server not found on network");
                    future.complete(null);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error discovering backend", e);
                future.complete(null);
            }
        });
        
        return future;
    }
    
    /**
     * Get device's current IP address on active network
     */
    private static String getDeviceIP(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) 
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return null;
            
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork == null) return null;
            
            // Get network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Skip loopback and inactive interfaces
                if (networkInterface.isLoopback() || !networkInterface.isUp()) {
                    continue;
                }
                
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    
                    // Skip loopback and IPv6
                    if (address.isLoopbackAddress() || address instanceof java.net.Inet6Address) {
                        continue;
                    }
                    
                    String ip = address.getHostAddress();
                    Log.d(TAG, "Found IP: " + ip);
                    return ip;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting device IP", e);
        }
        return null;
    }
    
    /**
     * Extract network prefix from IP (e.g., "192.168.1" from "192.168.1.100")
     */
    private static String extractNetworkPrefix(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return null;
    }
    
    /**
     * Generate list of IPs to scan based on network prefix
     */
    private static List<String> generateIPsToScan(String networkPrefix) {
        List<String> ips = new ArrayList<>();
        
        if (networkPrefix == null) {
            // Fallback: scan common ranges
            scanCommonRanges(ips);
            return ips;
        }
        
        // Scan current subnet (1-254)
        for (int i = 1; i <= 254; i++) {
            ips.add(networkPrefix + "." + i);
        }
        
        // Also scan common gateway IPs first (priority)
        ips.add(0, networkPrefix + ".1");
        ips.add(1, networkPrefix + ".254");
        ips.add(2, networkPrefix + ".100");
        ips.add(3, networkPrefix + ".101");
        ips.add(4, networkPrefix + ".14");
        ips.add(5, networkPrefix + ".13");
        
        return ips;
    }
    
    /**
     * Scan common network ranges if we can't determine device network
     */
    private static void scanCommonRanges(List<String> ips) {
        // Common home network ranges
        String[] commonPrefixes = {
            "192.168.1", "192.168.0", "192.168.100",
            "10.0.0", "10.0.2",
            "172.16.0", "172.20.10"
        };
        
        for (String prefix : commonPrefixes) {
            // Scan gateway and common server IPs
            ips.add(prefix + ".1");
            ips.add(prefix + ".100");
            ips.add(prefix + ".101");
            ips.add(prefix + ".14");
            ips.add(prefix + ".13");
        }
    }
    
    /**
     * Scan multiple IPs concurrently to find backend server
     */
    private static String scanIPs(List<String> ips) {
        List<Future<String>> futures = new ArrayList<>();
        
        // Submit all scan tasks
        for (String ip : ips) {
            Future<String> future = executorService.submit(() -> checkServer(ip));
            futures.add(future);
        }
        
        // Wait for first successful result
        for (Future<String> future : futures) {
            try {
                String result = future.get(2, TimeUnit.SECONDS);
                if (result != null) {
                    // Cancel remaining tasks
                    for (Future<String> f : futures) {
                        if (!f.isDone()) {
                            f.cancel(true);
                        }
                    }
                    return result;
                }
            } catch (Exception e) {
                // Continue to next IP
            }
        }
        
        return null;
    }
    
    /**
     * Check if server is running at given IP
     */
    private static String checkServer(String ip) {
        try {
            // Try to connect to the port
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, BACKEND_PORT), CONNECTION_TIMEOUT);
            socket.close();
            
            // Verify it's actually the backend by checking HTTP endpoint
            String baseUrl = "http://" + ip + ":" + BACKEND_PORT;
            if (verifyBackendEndpoint(baseUrl)) {
                Log.d(TAG, "Verified backend at: " + baseUrl);
                return baseUrl + "/";
            }
        } catch (SocketTimeoutException e) {
            // Timeout - server not available
        } catch (IOException e) {
            // Connection refused or other error
        } catch (Exception e) {
            Log.d(TAG, "Error checking " + ip + ": " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Verify that the endpoint is actually our backend server
     */
    private static boolean verifyBackendEndpoint(String baseUrl) {
        try {
            java.net.URL url = new java.net.URL(baseUrl + "/api/health");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(1000);
            conn.setReadTimeout(1000);
            conn.setRequestMethod("GET");
            
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            
            // Accept 200, 404 (server exists), or 401 (auth required - server exists)
            return responseCode == 200 || responseCode == 404 || responseCode == 401;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Quick check if a specific IP is the backend (for cached IPs)
     */
    public static boolean isBackendAvailable(String baseUrl) {
        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "api/health" : baseUrl + "/api/health";
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) 
                new java.net.URL(url).openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 500;
        } catch (Exception e) {
            return false;
        }
    }
}



