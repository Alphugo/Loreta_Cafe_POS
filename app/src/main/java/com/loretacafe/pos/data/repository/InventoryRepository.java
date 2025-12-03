package com.loretacafe.pos.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.mapper.DataMappers;
import com.loretacafe.pos.data.remote.api.InventoryApi;
import com.loretacafe.pos.data.remote.dto.ProductPayloadDto;
import com.loretacafe.pos.data.remote.dto.ProductResponseDto;
import com.loretacafe.pos.data.remote.dto.StockAdjustmentRequestDto;
import com.loretacafe.pos.data.util.ApiResult;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class InventoryRepository {

    private final InventoryApi inventoryApi;
    private final ProductDao productDao;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public InventoryRepository(InventoryApi inventoryApi, ProductDao productDao) {
        this.inventoryApi = inventoryApi;
        this.productDao = productDao;
    }

    public LiveData<List<ProductEntity>> observeProducts() {
        return productDao.observeAll();
    }

    public LiveData<ApiResult<Void>> refreshProducts() {
        MutableLiveData<ApiResult<Void>> result = new MutableLiveData<>();
        result.setValue(ApiResult.loading());

        executorService.execute(() -> {
            try {
                Response<List<ProductResponseDto>> response = inventoryApi.getProducts().execute();
                if (response.isSuccessful()) {
                    List<ProductResponseDto> body = response.body();
                    // If backend returns a non-empty list, sync it to local DB.
                    // If it's null or empty, keep existing local products so
                    // Create Order and Inventory still work offline and with
                    // locally-seeded menu items.
                    if (body != null && !body.isEmpty()) {
                        List<ProductEntity> entities = DataMappers.toProductEntities(body);
                        productDao.clear();
                        productDao.insertAll(entities);
                    }
                    result.postValue(ApiResult.success(null));
                } else {
                    result.postValue(ApiResult.error(extractError(response)));
                }
            } catch (IOException e) {
                result.postValue(ApiResult.error(e.getMessage()));
            }
        });

        return result;
    }

    public LiveData<ApiResult<ProductEntity>> createProduct(ProductPayloadDto payload) {
        MutableLiveData<ApiResult<ProductEntity>> liveData = new MutableLiveData<>(ApiResult.loading());

        executorService.execute(() -> {
            try {
                Response<ProductResponseDto> response = inventoryApi.createProduct(payload).execute();
                if (response.isSuccessful() && response.body() != null) {
                    ProductEntity entity = DataMappers.toEntity(response.body());
                    productDao.insert(entity);
                    liveData.postValue(ApiResult.success(entity));
                } else {
                    liveData.postValue(ApiResult.error(extractError(response)));
                }
            } catch (IOException e) {
                liveData.postValue(ApiResult.error(e.getMessage()));
            }
        });

        return liveData;
    }

    public LiveData<ApiResult<ProductEntity>> updateProduct(long id, ProductPayloadDto payload) {
        MutableLiveData<ApiResult<ProductEntity>> liveData = new MutableLiveData<>(ApiResult.loading());

        executorService.execute(() -> {
            try {
                Response<ProductResponseDto> response = inventoryApi.updateProduct(id, payload).execute();
                if (response.isSuccessful() && response.body() != null) {
                    ProductEntity entity = DataMappers.toEntity(response.body());
                    productDao.insert(entity);
                    liveData.postValue(ApiResult.success(entity));
                } else {
                    liveData.postValue(ApiResult.error(extractError(response)));
                }
            } catch (IOException e) {
                liveData.postValue(ApiResult.error(e.getMessage()));
            }
        });

        return liveData;
    }

    public LiveData<ApiResult<Void>> deleteProduct(long id) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>(ApiResult.loading());

        executorService.execute(() -> {
            try {
                Response<Void> response = inventoryApi.deleteProduct(id).execute();
                if (response.isSuccessful()) {
                    productDao.delete(id);
                    liveData.postValue(ApiResult.success(null));
                } else {
                    liveData.postValue(ApiResult.error(extractError(response)));
                }
            } catch (IOException e) {
                liveData.postValue(ApiResult.error(e.getMessage()));
            }
        });

        return liveData;
    }

    public LiveData<ApiResult<Void>> adjustStock(long productId, int quantityChange) {
        MutableLiveData<ApiResult<Void>> liveData = new MutableLiveData<>(ApiResult.loading());

        executorService.execute(() -> {
            try {
                Response<Void> response = inventoryApi.adjustStock(
                        new StockAdjustmentRequestDto(productId, quantityChange)
                ).execute();

                if (response.isSuccessful()) {
                    ProductEntity entity = productDao.getById(productId);
                    if (entity != null) {
                        entity.setQuantity(entity.getQuantity() + quantityChange);
                        productDao.update(entity);
                    }
                    liveData.postValue(ApiResult.success(null));
                } else {
                    liveData.postValue(ApiResult.error(extractError(response)));
                }
            } catch (IOException e) {
                liveData.postValue(ApiResult.error(e.getMessage()));
            }
        });

        return liveData;
    }

    /**
     * Update product locally immediately (for auto-save and offline support)
     * Then sync with backend in the background
     */
    public LiveData<ApiResult<ProductEntity>> updateProductLocalFirst(long id,
                                                                      String name,
                                                                      String category,
                                                                      String supplier,
                                                                      java.math.BigDecimal cost,
                                                                      java.math.BigDecimal price,
                                                                      int quantity,
                                                                      String status) {
        MutableLiveData<ApiResult<ProductEntity>> liveData = new MutableLiveData<>(ApiResult.loading());

        executorService.execute(() -> {
            // First, update locally immediately
            ProductEntity entity = productDao.getById(id);
            if (entity != null) {
                entity.setName(name);
                entity.setCategory(category);
                entity.setSupplier(supplier);
                entity.setCost(cost);
                entity.setPrice(price);
                entity.setQuantity(quantity);
                entity.setStatus(status);
                entity.setUpdatedAt(java.time.OffsetDateTime.now());
                productDao.update(entity);
                
                // CRITICAL: Room LiveData will automatically trigger observers
                // This ensures RealTimeAvailabilityManager immediately recalculates availability
                // Works 100% offline - no internet required
                android.util.Log.d("InventoryRepository", "Updated product locally: " + entity.getName() + 
                    " (quantity: " + quantity + ") - Room LiveData will trigger availability recalculation");
                
                // Post success immediately for UI update
                liveData.postValue(ApiResult.success(entity));
            } else {
                liveData.postValue(ApiResult.error("Product not found"));
                return;
            }

            // Then, sync with backend in the background (non-blocking)
            try {
                ProductPayloadDto payload = new ProductPayloadDto(name, category, supplier, cost, price, quantity);
                Response<ProductResponseDto> response = inventoryApi.updateProduct(id, payload).execute();
                if (response.isSuccessful() && response.body() != null) {
                    ProductEntity updatedEntity = DataMappers.toEntity(response.body());
                    productDao.insert(updatedEntity); // Update with backend response
                    // Don't post again - already posted success above
                }
                // If backend fails, local update is already done, so we don't show error
            } catch (IOException e) {
                // Silently fail - local update is already done
                android.util.Log.w("InventoryRepository", "Backend sync failed, but local update succeeded", e);
            }
        });

        return liveData;
    }

    private String extractError(Response<?> response) {
        if (response == null) {
            return "Unknown error";
        }
        try {
            if (response.errorBody() != null) {
                return response.errorBody().string();
            }
        } catch (IOException ignored) {
        }
        return "Request failed with code " + response.code();
    }
}

