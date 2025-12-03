# Firebase Setup Guide for Loreta's Café POS

## 🎯 What Has Been Done

I've integrated Firebase into your POS system with the following components:

### ✅ Completed:
1. **Firebase Dependencies Added**
   - Firebase Authentication
   - Cloud Firestore
   - Firebase Storage
   - Firebase Analytics

2. **Firebase Repositories Created**
   - `FirebaseAuthRepository.java` - Handles authentication
   - `FirebaseProductRepository.java` - Handles products/inventory sync
   - `FirebaseSalesRepository.java` - Handles sales transactions sync

3. **Application Initialization**
   - Firebase initialized in `PosApp.java`

4. **Build Configuration**
   - Google Services plugin added
   - Dependencies configured in `build.gradle.kts`

## 📋 What You Need to Do

### Step 1: Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add project"** or select existing project
3. Enter project name: `Loreta Cafe POS` (or your preferred name)
4. Follow the setup wizard (disable Google Analytics if you want)

### Step 2: Add Android App to Firebase

1. In Firebase Console, click **"Add app"** → Select **Android**
2. Enter package name: `com.loretacafe.pos`
3. Enter app nickname: `Loreta Cafe POS` (optional)
4. Click **"Register app"**
5. **Download `google-services.json`**
6. Place the downloaded file in: `app/google-services.json`
   - **Important:** Replace the placeholder file if it exists

### Step 3: Enable Firebase Services

#### Enable Authentication:
1. In Firebase Console, go to **Authentication**
2. Click **"Get started"**
3. Go to **"Sign-in method"** tab
4. Enable **"Email/Password"** provider
5. Click **"Save"**

#### Enable Firestore:
1. In Firebase Console, go to **Firestore Database**
2. Click **"Create database"**
3. Choose **"Start in test mode"** (for development)
4. Select a location (choose closest to your region)
5. Click **"Enable"**

### Step 4: Configure Firestore Security Rules

1. In Firestore, go to **"Rules"** tab
2. Replace the default rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Products collection
    match /products/{productId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    // Sales collection
    match /sales/{saleId} {
      allow read: if request.auth != null;
      allow create: if request.auth != null;
      allow update, delete: if request.auth != null;
    }
    
    // Sale items collection
    match /saleItems/{itemId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

3. Click **"Publish"**

**⚠️ Note:** These rules allow authenticated users to read/write. For production, implement stricter rules based on user roles.

### Step 5: Build and Test

1. **Sync Gradle files** in Android Studio
2. **Build the project** (should compile successfully)
3. **Run the app** on a device/emulator
4. Test authentication:
   - Try to register a new user
   - Try to login
   - Check Firebase Console → Authentication to see users

### Step 6: Migrate Existing Data (Optional)

If you have existing data in SQLite that you want to migrate to Firestore:

1. Export data from Room database
2. Use Firebase Admin SDK or a migration script to upload to Firestore
3. Or manually create initial data in Firestore Console

## 🔧 Next Steps for Full Integration

The Firebase repositories are created but not yet integrated into your existing repositories. To complete the integration:

### Option A: Gradual Migration (Recommended)
1. Keep existing Room database for offline support
2. Add Firebase sync alongside existing code
3. Gradually migrate features to Firebase-first approach

### Option B: Full Migration
1. Replace `AuthRepository` to use `FirebaseAuthRepository`
2. Update `InventoryRepository` to sync with `FirebaseProductRepository`
3. Update `SalesRepository` to sync with `FirebaseSalesRepository`
4. Implement bidirectional sync (Room ↔ Firestore)

## 📝 Integration Code Examples

### Using Firebase Auth in MainActivity:

```java
// Replace LocalAuthService with FirebaseAuthRepository
FirebaseAuthRepository firebaseAuth = new FirebaseAuthRepository();
firebaseAuth.login(email, password).observe(this, result -> {
    if (result.getStatus() == ApiResult.Status.SUCCESS) {
        // Login successful
        UserEntity user = result.getData();
        navigateToDashboard();
    } else {
        // Show error
        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
    }
});
```

### Syncing Products:

```java
FirebaseProductRepository firebaseProducts = new FirebaseProductRepository();

// Observe real-time updates
firebaseProducts.observeProducts().observe(this, products -> {
    // Update UI with products
    adapter.setProducts(products);
});

// Create new product
firebaseProducts.createProduct(productEntity).observe(this, result -> {
    if (result.getStatus() == ApiResult.Status.SUCCESS) {
        // Product created in Firestore
    }
});
```

## 🐛 Troubleshooting

### Issue: "google-services.json not found"
- **Solution:** Make sure `google-services.json` is in `app/` directory (not `app/src/main/`)

### Issue: "FirebaseApp not initialized"
- **Solution:** Check that Firebase is initialized in `PosApp.onCreate()`

### Issue: "Authentication failed"
- **Solution:** 
  - Check that Email/Password is enabled in Firebase Console
  - Verify `google-services.json` has correct package name
  - Check internet connection

### Issue: "Permission denied" in Firestore
- **Solution:** 
  - Check Firestore security rules
  - Ensure user is authenticated
  - Verify rules are published

## 📚 Additional Resources

- [Firebase Documentation](https://firebase.google.com/docs)
- [Firestore Security Rules](https://firebase.google.com/docs/firestore/security/get-started)
- [Firebase Authentication](https://firebase.google.com/docs/auth)

## ✅ Checklist

- [ ] Firebase project created
- [ ] Android app added to Firebase
- [ ] `google-services.json` downloaded and placed in `app/`
- [ ] Authentication enabled (Email/Password)
- [ ] Firestore database created
- [ ] Security rules configured
- [ ] Project builds successfully
- [ ] App runs without crashes
- [ ] Can register/login users
- [ ] Data syncs to Firestore

---

**Need Help?** Check the `FIREBASE_INTEGRATION_PLAN.md` for detailed architecture and implementation strategy.

