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
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.entity.SaleItemEntity;
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
 * Firebase Sales Repository
 * Handles sales transactions synchronization with Firestore
 */
public class FirebaseSalesRepository {

    private static final String TAG = "FirebaseSalesRepo";
    private static final String SALES_COLLECTION = "sales";
    private static final String SALE_ITEMS_COLLECTION = "saleItems";
    
    private final FirebaseFirestore firestore;
    private final ExecutorService executorService;
    private ListenerRegistration salesListener;

    public FirebaseSalesRepository() {
        this.firestore = FirebaseFirestore.getInstance();
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Create a new sale in Firestore
     */
    public LiveData<ApiResult<SaleEntity>> createSale(SaleEntity sale, List<SaleItemEntity> saleItems) {
        MutableLiveData<ApiResult<SaleEntity>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        String saleId = String.valueOf(sale.getId());
        Map<String, Object> saleData = mapSaleToFirestore(sale);

        // Use batch write for atomic operation
        firestore.runTransaction(transaction -> {
            // Create sale document
            transaction.set(firestore.collection(SALES_COLLECTION).document(saleId), saleData);
            
            // Create sale items
            for (SaleItemEntity item : saleItems) {
                String itemId = String.valueOf(item.getId());
                Map<String, Object> itemData = mapSaleItemToFirestore(item);
                transaction.set(
                    firestore.collection(SALE_ITEMS_COLLECTION).document(itemId),
                    itemData
                );
            }
            return null;
        }).addOnCompleteListener(executorService, task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "Sale created: " + saleId);
                liveData.postValue(ApiResult.success(sale));
            } else {
                Log.e(TAG, "Failed to create sale", task.getException());
                liveData.postValue(ApiResult.error("Failed to create sale: " + 
                        (task.getException() != null ? task.getException().getMessage() : "Unknown error")));
            }
        });

        return liveData;
    }

    /**
     * Observe sales in real-time from Firestore
     */
    public LiveData<List<SaleEntity>> observeSales() {
        MutableLiveData<List<SaleEntity>> liveData = new MutableLiveData<>();

        CollectionReference salesRef = firestore.collection(SALES_COLLECTION);
        
        // Remove existing listener if any
        if (salesListener != null) {
            salesListener.remove();
        }

        salesListener = salesRef
                .orderBy("saleDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to sales", error);
                        liveData.postValue(new ArrayList<>());
                        return;
                    }

                    if (snapshot != null) {
                        List<SaleEntity> sales = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            SaleEntity sale = mapSaleFromFirestore(doc);
                            if (sale != null) {
                                sales.add(sale);
                            }
                        }
                        liveData.postValue(sales);
                        Log.d(TAG, "Sales updated: " + sales.size());
                    }
                });

        return liveData;
    }

    /**
     * Get sale items for a specific sale
     */
    public LiveData<List<SaleItemEntity>> getSaleItems(long saleId) {
        MutableLiveData<List<SaleItemEntity>> liveData = new MutableLiveData<>();

        firestore.collection(SALE_ITEMS_COLLECTION)
                .whereEqualTo("saleId", saleId)
                .get()
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        List<SaleItemEntity> items = new ArrayList<>();
                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                SaleItemEntity item = mapSaleItemFromFirestore(doc);
                                if (item != null) {
                                    items.add(item);
                                }
                            }
                        }
                        liveData.postValue(items);
                    } else {
                        Log.e(TAG, "Failed to get sale items", task.getException());
                        liveData.postValue(new ArrayList<>());
                    }
                });

        return liveData;
    }

    /**
     * Sync all sales from Firestore (one-time fetch)
     */
    public LiveData<ApiResult<List<SaleEntity>>> syncAllSales() {
        MutableLiveData<ApiResult<List<SaleEntity>>> liveData = new MutableLiveData<>();
        liveData.setValue(ApiResult.loading());

        firestore.collection(SALES_COLLECTION)
                .orderBy("saleDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(executorService, task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot snapshot = task.getResult();
                        List<SaleEntity> sales = new ArrayList<>();
                        if (snapshot != null) {
                            for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                SaleEntity sale = mapSaleFromFirestore(doc);
                                if (sale != null) {
                                    sales.add(sale);
                                }
                            }
                        }
                        Log.d(TAG, "Synced " + sales.size() + " sales from Firestore");
                        liveData.postValue(ApiResult.success(sales));
                    } else {
                        Log.e(TAG, "Failed to sync sales", task.getException());
                        liveData.postValue(ApiResult.error("Failed to sync sales: " + 
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error")));
                    }
                });

        return liveData;
    }

    /**
     * Map SaleEntity to Firestore document
     */
    private Map<String, Object> mapSaleToFirestore(SaleEntity sale) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", sale.getId());
        data.put("cashierId", sale.getCashierId());
        data.put("saleDate", sale.getSaleDate() != null ? 
                sale.getSaleDate().toString() : OffsetDateTime.now().toString());
        data.put("totalAmount", sale.getTotalAmount() != null ? 
                sale.getTotalAmount().doubleValue() : 0.0);
        data.put("customerName", sale.getCustomerName());
        data.put("orderNumber", sale.getOrderNumber());
        data.put("paymentMethod", sale.getPaymentMethod());
        return data;
    }

    /**
     * Map Firestore document to SaleEntity
     */
    private SaleEntity mapSaleFromFirestore(DocumentSnapshot doc) {
        try {
            SaleEntity entity = new SaleEntity();
            
            // Handle ID
            Object idObj = doc.get("id");
            if (idObj != null) {
                if (idObj instanceof Long) {
                    entity.setId((Long) idObj);
                } else if (idObj instanceof Number) {
                    entity.setId(((Number) idObj).longValue());
                } else {
                    try {
                        entity.setId(Long.parseLong(doc.getId()));
                    } catch (NumberFormatException e) {
                        entity.setId(System.currentTimeMillis());
                    }
                }
            } else {
                try {
                    entity.setId(Long.parseLong(doc.getId()));
                } catch (NumberFormatException e) {
                    entity.setId(System.currentTimeMillis());
                }
            }

            Long cashierId = doc.getLong("cashierId");
            entity.setCashierId(cashierId != null ? cashierId : 1L);
            
            String saleDateStr = doc.getString("saleDate");
            if (saleDateStr != null) {
                try {
                    entity.setSaleDate(OffsetDateTime.parse(saleDateStr));
                } catch (Exception e) {
                    entity.setSaleDate(OffsetDateTime.now());
                }
            }
            
            Double totalAmount = doc.getDouble("totalAmount");
            entity.setTotalAmount(totalAmount != null ? 
                    BigDecimal.valueOf(totalAmount) : BigDecimal.ZERO);
            
            entity.setCustomerName(doc.getString("customerName"));
            entity.setOrderNumber(doc.getString("orderNumber"));
            entity.setPaymentMethod(doc.getString("paymentMethod"));
            
            return entity;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping sale entity", e);
            return null;
        }
    }

    /**
     * Map SaleItemEntity to Firestore document
     */
    private Map<String, Object> mapSaleItemToFirestore(SaleItemEntity item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("saleId", item.getSaleId());
        data.put("productId", item.getProductId());
        data.put("quantity", item.getQuantity());
        data.put("price", item.getPrice() != null ? item.getPrice().doubleValue() : 0.0);
        data.put("subtotal", item.getSubtotal() != null ? item.getSubtotal().doubleValue() : 0.0);
        data.put("size", item.getSize());
        data.put("productName", item.getProductName());
        return data;
    }

    /**
     * Map Firestore document to SaleItemEntity
     */
    private SaleItemEntity mapSaleItemFromFirestore(DocumentSnapshot doc) {
        try {
            SaleItemEntity entity = new SaleItemEntity();
            
            Object idObj = doc.get("id");
            if (idObj != null) {
                if (idObj instanceof Long) {
                    entity.setId((Long) idObj);
                } else if (idObj instanceof Number) {
                    entity.setId(((Number) idObj).longValue());
                }
            }
            
            Long saleId = doc.getLong("saleId");
            entity.setSaleId(saleId != null ? saleId : 0L);
            
            Long productId = doc.getLong("productId");
            entity.setProductId(productId != null ? productId : 0L);
            
            Long quantity = doc.getLong("quantity");
            entity.setQuantity(quantity != null ? quantity.intValue() : 0);
            
            Double price = doc.getDouble("price");
            entity.setPrice(price != null ? BigDecimal.valueOf(price) : BigDecimal.ZERO);
            
            Double subtotal = doc.getDouble("subtotal");
            entity.setSubtotal(subtotal != null ? BigDecimal.valueOf(subtotal) : BigDecimal.ZERO);
            
            entity.setSize(doc.getString("size"));
            entity.setProductName(doc.getString("productName"));
            
            return entity;
        } catch (Exception e) {
            Log.e(TAG, "Error mapping sale item entity", e);
            return null;
        }
    }

    /**
     * Clean up listeners
     */
    public void cleanup() {
        if (salesListener != null) {
            salesListener.remove();
            salesListener = null;
        }
    }
}

