package com.loretacafe.pos.data.firebase;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.util.ApiResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Firebase Product Repository
 * Handles product/inventory data synchronization with Firestore
 */
public class FirebaseProductRepository {

    private static final String TAG = "FirebaseProductRepo";
    private static final String PRODUCTS_COLLECTION = "products";
    
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;
    private ListenerRegistration productsListener;

    public FirebaseProductRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Observe products in real-time from Firestore
     */
    public LiveData<List<ProductEntity>> observeProducts() {
        MutableLiveData<List<ProductEntity>> liveData = new MutableLiveData<>();

        CollectionReference productsRef = firestore.collection(PRODUCTS_COLLECTION);
        
        // Remove existing listener if any
        if (productsListener != null) {
            productsListener.remove();
        }

        productsListener = productsRef
                .orderBy("name")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to products", error);
                        liveData.postValue(new ArrayList<>());
                        return;
                    }

                    if (snapshot != null) {
                        List<ProductEntity> products = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            ProductEntity product = mapToProductEntity(doc);
                            if (product != null) {
                                products.add(product);
                            }
                        }
                        liveData.postValue(products);
                        Log.d(TAG, "Products updated: " + products.size());
                    }
                });

        return liveData;
    }

    /**
     * Create a new product in Firestore
     */
    public LiveData<ApiResult<ProductEntity>> createProduct(ProductEntity product) {
        MutableLiveData<ApiResult<ProductEntity>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        String productId = String.valueOf(product.getId());
        Map<String, Object> productData = mapToFirestore(product);

        firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .set(productData)
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Product created: " + productId);
                        liveData.postValue(ApiResult.success(product));
                    } else {
                        Log.e(TAG, "Failed to create product", task.getException());
                        liveData.postValue(ApiResult.error("Failed to create product: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error")));
                    }
                });

        return liveData;
    }

    /**
     * Update product in Firestore
     */
    public LiveData<ApiResult<ProductEntity>> updateProduct(ProductEntity product) {
        MutableLiveData<ApiResult<ProductEntity>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        String productId = String.valueOf(product.getId());
        Map<String, Object> productData = mapToFirestore(product);
        productData.put("updatedAt", OffsetDateTime.now().toString());

        firestore.collection(PRODUCTS_COLLECTION)
                .document(productId)
                .update(productData)
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Product updated: " + productId);
                        liveData.postValue(ApiResult.success(product));
                    } else {
                        Log.e(TAG, "Failed to update product", task.getException());
                        liveData.postValue(ApiResult.error("Failed to update product: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error")));
                    }
                });

        return liveData;
    }

    /**
     * Delete product from Firestore
     */
    public LiveData<ApiResult<Void>> deleteProduct(long productId) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        firestore.collection(PRODUCTS_COLLECTION)
                .document(String.valueOf(productId))
                .delete()
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Product deleted: " + productId);
                        liveData.postValue(ApiResult.success(null));
                    } else {
                        Log.e(TAG, "Failed to delete product", task.getException());
                        liveData.postValue(ApiResult.error("Failed to delete product: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error")));
                    }
                });

        return liveData;
    }

    /**
     * Sync all products from Firestore to local (one-time fetch)
     */
    public LiveData<ApiResult<List<ProductEntity>>> syncAllProducts() {
        MutableLiveData<ApiResult<List<ProductEntity>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        firestore.collection(PRODUCTS_COLLECTION)
                .orderBy("name")
                .get()
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        List<ProductEntity> products = new ArrayList<>();
                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                ProductEntity product = mapToProductEntity(doc);
                                if (product != null) {
                                    products.add(product);
                                }
                            }
                        }
                        Log.d(TAG, "Synced " + products.size() + " products from Firestore");
                        liveData.postValue(ApiResult.success(products));
                    } else {
                        Log.e(TAG, "Failed to sync products", task.getException());
                        liveData.postValue(ApiResult.error("Failed to sync products: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error")));
                    }
                });

        return liveData;
    }

    /**
     * Map ProductEntity to Firestore document
     */
    private Map<String, Object> mapToFirestore(ProductEntity product) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", product.getId());
        data.put("name", product.getName());
        data.put("category", product.getCategory());
        data.put("supplier", product.getSupplier());
        data.put("cost", product.getCost() != null ? product.getCost().doubleValue() : 0.0);
        data.put("price", product.getPrice() != null ? product.getPrice().doubleValue() : 0.0);
        data.put("quantity", product.getQuantity());
        data.put("status", product.getStatus());
        data.put("createdAt", product.getCreatedAt() != null ? 
                product.getCreatedAt().toString() : OffsetDateTime.now().toString());
        data.put("updatedAt", product.getUpdatedAt() != null ? 
                product.getUpdatedAt().toString() : OffsetDateTime.now().toString());
        return data;
    }

    /**
     * Map Firestore document to ProductEntity
     */
    private ProductEntity mapToProductEntity(DocumentSnapshot doc) {
        try {
            ProductEntity entity = new ProductEntity();
            
            // Handle ID - can be string or number
            Object idObj = doc.get("id");
            if (idObj != null) {
                if (idObj instanceof Long) {
                    entity.setId((Long) idObj);
                } else if (idObj instanceof Number) {
                    entity.setId(((Number) idObj).longValue());
                } else {
                    // Try to parse from document ID
                    try {
                        entity.setId(Long.parseLong(doc.getId()));
                    } catch (NumberFormatException e) {
                        entity.setId(System.currentTimeMillis());
                    }
                }
            } else {
                // Use document ID as fallback
                try {
                    entity.setId(Long.parseLong(doc.getId()));
                } catch (NumberFormatException e) {
                    entity.setId(System.currentTimeMillis());
                }
            }

            entity.setName(doc.getString("name"));
            entity.setCategory(doc.getString("category"));
            entity.setSupplier(doc.getString("supplier"));
            
            // Handle BigDecimal fields
            Double cost = doc.getDouble("cost");
            entity.setCost(cost != null ? BigDecimal.valueOf(cost) : BigDecimal.ZERO);
            
            Double price = doc.getDouble("price");
            entity.setPrice(price != null ? BigDecimal.valueOf(price) : BigDecimal.ZERO);
            
            Long quantity = doc.getLong("quantity");
            entity.setQuantity(quantity != null ? quantity.intValue() : 0);
            
            entity.setStatus(doc.getString("status"));
            
            // Parse dates
            String createdAt = doc.getString("createdAt");
            String updatedAt = doc.getString("updatedAt");
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
        } catch (Exception e) {
            Log.e(TAG, "Error mapping product entity", e);
            return null;
        }
    }

    /**
     * Clean up listeners
     */
    public void cleanup() {
        if (productsListener != null) {
            productsListener.remove();
            productsListener = null;
        }
    }
}

