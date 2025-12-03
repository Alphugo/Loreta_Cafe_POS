package com.loretacafe.pos;

import android.app.Application;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.firebase.FirebaseApp;
import com.loretacafe.pos.di.RepositoryProvider;
import com.loretacafe.pos.sync.SyncWorker;

import java.util.concurrent.TimeUnit;

public class PosApp extends Application {

    private static final String SYNC_WORK_NAME = "pending_sync_worker";

    private RepositoryProvider repositoryProvider;

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase with offline persistence
        try {
            FirebaseApp.initializeApp(this);
            
            // Configure Firestore to use offline persistence
            // This prevents connection warnings when device is offline
            try {
                com.google.firebase.firestore.FirebaseFirestore firestore = 
                    com.google.firebase.firestore.FirebaseFirestore.getInstance();
                com.google.firebase.firestore.FirebaseFirestoreSettings settings = 
                    new com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true) // Enable offline persistence
                        .setCacheSizeBytes(com.google.firebase.firestore.FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                        .build();
                firestore.setFirestoreSettings(settings);
                android.util.Log.d("PosApp", "Firebase Firestore configured with offline persistence");
            } catch (Exception firestoreException) {
                android.util.Log.w("PosApp", "Firestore offline persistence setup failed (app will still work offline)", firestoreException);
            }
            
            android.util.Log.d("PosApp", "Firebase initialized successfully");
        } catch (Exception e) {
            android.util.Log.e("PosApp", "Firebase initialization failed", e);
        }
        
        repositoryProvider = new RepositoryProvider(this);
        
        // CRITICAL: Force clean database FIRST (synchronously) to remove all non-ingredients
        // This must happen before any other operations to ensure Inventory only shows 61 ingredients
        android.util.Log.d("PosApp", "Cleaning database to keep only 61 ingredients...");
        com.loretacafe.pos.data.local.RawMaterialsSeeder.forceCleanDatabase(
                repositoryProvider.getDatabase().productDao()
        );

        // Seed raw materials master list on first install
        // Note: We no longer seed the old sample inventory (Espresso, Oreo Cheesecake).
        // Inventory should follow the new formatted raw materials master list.
        com.loretacafe.pos.data.local.RawMaterialsSeeder.seedIfNeeded(
                this,
                repositoryProvider.getDatabase().productDao()
        );
        
        // CRITICAL: Seed menu items FIRST (before recipes) so recipes can reference them
        // This ensures Create Order screen works even if MenuActivity is never opened
        // Run synchronously to ensure menu items are available immediately
        com.loretacafe.pos.data.local.MenuSeeder.seedIfNeeded(
                this,
                repositoryProvider.getDatabase().productDao(),
                false // Synchronous - ensures menu items are ready before app starts
        );
        
        // Seed exact recipes for all menu items on first install
        com.loretacafe.pos.data.local.RecipeSeeder.seedIfNeeded(
                this,
                repositoryProvider.getDatabase().productDao(),
                repositoryProvider.getDatabase().recipeDao()
        );
        
        // Create default admin user on app start
        createDefaultAdminUser();
        
        // Disable sync worker for offline mode
        // Cancel any existing sync work
        cancelSyncWork();
        // scheduleSyncWork();
        
        // Start automatic backend discovery on app start
        com.loretacafe.pos.data.remote.ApiConfig.startDiscovery(this);
        
        // Schedule daily reset at 3:00 AM
        com.loretacafe.pos.util.DailyResetService.scheduleDailyReset(this);
    }
    
    /**
     * Create default admin user for initial app setup
     * Ensures temp@loreta.com account always exists
     */
    private void createDefaultAdminUser() {
        new Thread(() -> {
            try {
                android.util.Log.d("PosApp", "Creating default admin user...");
                com.loretacafe.pos.data.local.service.LocalAuthService authService = 
                    new com.loretacafe.pos.data.local.service.LocalAuthService(this);
                authService.createDefaultAdmin();
                android.util.Log.d("PosApp", "Default admin user initialization complete");
            } catch (Exception e) {
                android.util.Log.e("PosApp", "Error creating default admin user", e);
            }
        }).start();
    }
    
    private void cancelSyncWork() {
        try {
            WorkManager.getInstance(this).cancelUniqueWork(SYNC_WORK_NAME);
            android.util.Log.d("PosApp", "Sync work cancelled for offline mode");
        } catch (Exception e) {
            android.util.Log.e("PosApp", "Error cancelling sync work", e);
        }
    }

    public RepositoryProvider getRepositoryProvider() {
        return repositoryProvider;
    }

    private void scheduleSyncWork() {
        PeriodicWorkRequest syncRequest = new PeriodicWorkRequest.Builder(
                SyncWorker.class,
                15,
                TimeUnit.MINUTES
        ).build();

        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        SYNC_WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP,
                        syncRequest
                );
    }
}

