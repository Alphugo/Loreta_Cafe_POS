package com.loretacafe.pos.data.firebase;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.util.ApiResult;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Firebase Authentication Repository
 * Handles user authentication using Firebase Auth
 */
public class FirebaseAuthRepository {

    private static final String TAG = "FirebaseAuthRepo";
    private static final String USERS_COLLECTION = "users";
    
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;

    public FirebaseAuthRepository() {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Login with email and password
     */
    public LiveData<ApiResult<UserEntity>> login(String email, String password) {
        MutableLiveData<ApiResult<UserEntity>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Fetch user profile from Firestore
                            fetchUserProfile(firebaseUser.getUid(), liveData);
                        } else {
                            liveData.postValue(ApiResult.error("User not found"));
                        }
                    } else {
                        String errorMessage = getErrorMessage(task.getException());
                        Log.e(TAG, "Login failed: " + errorMessage);
                        liveData.postValue(ApiResult.error(errorMessage));
                    }
                });

        return liveData;
    }

    /**
     * Register new user
     */
    public LiveData<ApiResult<UserEntity>> register(String name, String email, String password, String role) {
        MutableLiveData<ApiResult<UserEntity>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Create user profile in Firestore
                            createUserProfile(firebaseUser.getUid(), name, email, role, liveData);
                        } else {
                            liveData.postValue(ApiResult.error("Failed to create user"));
                        }
                    } else {
                        String errorMessage = getErrorMessage(task.getException());
                        Log.e(TAG, "Registration failed: " + errorMessage);
                        liveData.postValue(ApiResult.error(errorMessage));
                    }
                });

        return liveData;
    }

    /**
     * Send password reset email
     */
    public LiveData<ApiResult<Void>> sendPasswordResetEmail(String email) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        Log.d(TAG, "Sending password reset email to: " + email);
        
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent successfully to: " + email);
                        liveData.postValue(ApiResult.success(null));
                    } else {
                        Exception exception = task.getException();
                        String errorMessage = getErrorMessage(exception);
                        
                        // Log full exception for debugging
                        if (exception != null) {
                            Log.e(TAG, "Password reset failed", exception);
                            Log.e(TAG, "Exception class: " + exception.getClass().getName());
                            Log.e(TAG, "Exception message: " + exception.getMessage());
                        }
                        
                        // If error message is null, provide a default message
                        if (errorMessage == null || errorMessage.isEmpty()) {
                            errorMessage = "Failed to send password reset email. Please check if the email is registered or try again later.";
                        }
                        
                        Log.e(TAG, "Password reset failed: " + errorMessage);
                        liveData.postValue(ApiResult.error(errorMessage));
                    }
                })
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Password reset email sent (success listener)");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Password reset email failed (failure listener)", e);
                });

        return liveData;
    }

    /**
     * Get current user
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Logout current user
     */
    public void logout() {
        firebaseAuth.signOut();
        Log.d(TAG, "User logged out");
    }

    /**
     * Create user profile in Firestore
     */
    private void createUserProfile(String userId, String name, String email, String role, 
                                   MutableLiveData<ApiResult<UserEntity>> liveData) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);
        userData.put("email", email);
        userData.put("role", role);
        userData.put("createdAt", OffsetDateTime.now().toString());
        userData.put("updatedAt", OffsetDateTime.now().toString());

        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .set(userData)
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        UserEntity userEntity = mapToUserEntity(userId, userData);
                        liveData.postValue(ApiResult.success(userEntity));
                    } else {
                        Log.e(TAG, "Failed to create user profile", task.getException());
                        liveData.postValue(ApiResult.error("Failed to create user profile"));
                    }
                });
    }

    /**
     * Fetch user profile from Firestore by email
     * Returns UserEntity if found, null otherwise
     * Note: This is a synchronous method that blocks until result is received
     */
    public UserEntity fetchUserByEmail(String email) {
        try {
            // Query Firestore for user with matching email
            // Note: This requires an index on email field in Firestore
            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final UserEntity[] result = new UserEntity[1];
            
            firestore.collection(USERS_COLLECTION)
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(executorService, task -> {
                        try {
                            if (task.isSuccessful()) {
                                QuerySnapshot snapshot = task.getResult();
                                if (snapshot != null && !snapshot.isEmpty()) {
                                    DocumentSnapshot document = snapshot.getDocuments().get(0);
                                    Map<String, Object> userData = document.getData();
                                    if (userData != null) {
                                        result[0] = mapToUserEntity(document.getId(), userData);
                                        Log.d(TAG, "User found in Firestore: " + email);
                                    }
                                } else {
                                    Log.d(TAG, "User not found in Firestore: " + email);
                                }
                            } else {
                                Log.e(TAG, "Failed to fetch user by email", task.getException());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing Firestore result", e);
                        } finally {
                            latch.countDown();
                        }
                    });
            
            // Wait for result (with timeout)
            try {
                boolean completed = latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
                if (!completed) {
                    Log.w(TAG, "Timeout waiting for Firestore query");
                }
            } catch (InterruptedException e) {
                Log.e(TAG, "Interrupted while waiting for user fetch", e);
                Thread.currentThread().interrupt();
            }
            
            return result[0];
        } catch (Exception e) {
            Log.e(TAG, "Error fetching user by email", e);
            return null;
        }
    }

    /**
     * Fetch user profile from Firestore
     */
    private void fetchUserProfile(String userId, MutableLiveData<ApiResult<UserEntity>> liveData) {
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document != null && document.exists()) {
                            Map<String, Object> userData = document.getData();
                            if (userData != null) {
                                UserEntity userEntity = mapToUserEntity(userId, userData);
                                liveData.postValue(ApiResult.success(userEntity));
                            } else {
                                liveData.postValue(ApiResult.error("User profile not found"));
                            }
                        } else {
                            liveData.postValue(ApiResult.error("User profile not found"));
                        }
                    } else {
                        Log.e(TAG, "Failed to fetch user profile", task.getException());
                        liveData.postValue(ApiResult.error("Failed to fetch user profile"));
                    }
                });
    }

    /**
     * Map Firestore data to UserEntity
     */
    private UserEntity mapToUserEntity(String userId, Map<String, Object> data) {
        UserEntity entity = new UserEntity();
        try {
            entity.setId(Long.parseLong(userId.replaceAll("[^0-9]", ""))); // Extract numeric ID
        } catch (NumberFormatException e) {
            entity.setId(System.currentTimeMillis()); // Fallback to timestamp
        }
        entity.setName((String) data.get("name"));
        entity.setEmail((String) data.get("email"));
        entity.setRole((String) data.get("role"));
        entity.setPassword(""); // Firebase handles passwords, not stored in entity
        
        String createdAt = (String) data.get("createdAt");
        String updatedAt = (String) data.get("updatedAt");
        if (createdAt != null) {
            try {
                entity.setCreatedAt(OffsetDateTime.parse(createdAt));
            } catch (Exception e) {
                entity.setCreatedAt(OffsetDateTime.now());
            }
        }
        if (updatedAt != null) {
            try {
                entity.setUpdatedAt(OffsetDateTime.parse(updatedAt));
            } catch (Exception e) {
                entity.setUpdatedAt(OffsetDateTime.now());
            }
        }
        
        return entity;
    }

    /**
     * Get user-friendly error message from exception
     */
    private String getErrorMessage(Exception exception) {
        if (exception == null) {
            return "Unknown error occurred";
        }
        
        if (exception instanceof FirebaseAuthException) {
            FirebaseAuthException authException = (FirebaseAuthException) exception;
            String errorCode = authException.getErrorCode();
            
            Log.d(TAG, "Firebase Auth Error Code: " + errorCode);
            
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    return "Invalid email address";
                case "ERROR_WRONG_PASSWORD":
                    return "Incorrect password";
                case "ERROR_INVALID_CREDENTIAL":
                    return "Invalid email or password";
                case "ERROR_USER_NOT_FOUND":
                    return "No account found with this email address";
                case "ERROR_USER_DISABLED":
                    return "This account has been disabled";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Too many requests. Please try again later";
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return "Email already in use";
                case "ERROR_WEAK_PASSWORD":
                    return "Password is too weak";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Network error. Please check your internet connection";
                case "ERROR_INTERNAL_ERROR":
                    return "Internal error. Please try again";
                default:
                    // Return a generic error message
                    return "Authentication failed. Error: " + errorCode;
            }
        }
        
        // For non-Firebase exceptions, return the message or a default
        String message = exception.getMessage();
        if (message != null && !message.isEmpty()) {
            return message;
        }
        
        return "Failed to send password reset email. Please try again.";
    }
}

