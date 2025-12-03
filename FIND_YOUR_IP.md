# How to Find Your Computer's IP Address

## For Windows:
1. Open Command Prompt or PowerShell
2. Run: `ipconfig`
3. Look for "IPv4 Address" under your active network adapter (usually Wi-Fi or Ethernet)
4. It will look something like: `192.168.1.100` or `192.168.100.14`

## For Mac/Linux:
1. Open Terminal
2. Run: `ifconfig` or `ip addr`
3. Look for your network interface (usually `en0` or `wlan0`)
4. Find the `inet` address (IPv4)

## Setting the IP in Your App:

### Option 1: Add to gradle.properties (Recommended)
Add this line to `gradle.properties`:
```
debugBaseUrl=http://YOUR_IP_ADDRESS:8080/
```
Replace `YOUR_IP_ADDRESS` with your actual IP (e.g., `192.168.100.14`)

### Option 2: Build with parameter
When building, use:
```bash
./gradlew assembleDebug -PdebugBaseUrl=http://YOUR_IP_ADDRESS:8080/
```

## Important Notes:
- Make sure your phone and computer are on the **same Wi-Fi network**
- Make sure Windows Firewall allows connections on port 8080
- The backend must be running and bound to `0.0.0.0` (which it should be by default)

