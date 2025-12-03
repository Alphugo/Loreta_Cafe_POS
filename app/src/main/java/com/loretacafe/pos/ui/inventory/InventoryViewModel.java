package com.loretacafe.pos.ui.inventory;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.loretacafe.pos.PosApp;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.remote.dto.ProductPayloadDto;
import com.loretacafe.pos.data.repository.InventoryRepository;
import com.loretacafe.pos.data.util.ApiResult;

import java.math.BigDecimal;
import java.util.List;

public class InventoryViewModel extends AndroidViewModel {

    private final InventoryRepository inventoryRepository;
    private final MediatorLiveData<ApiResult<?>> operationResult = new MediatorLiveData<>();
    private final LiveData<List<ProductEntity>> products;

    public InventoryViewModel(@NonNull Application application) {
        super(application);
        PosApp app = (PosApp) application;
        this.inventoryRepository = app.getRepositoryProvider().getInventoryRepository();
        this.products = inventoryRepository.observeProducts();
        // Don't call refresh() here - it tries to sync from backend
        // The LiveData from observeProducts() will automatically load from local database
        // refresh() should only be called explicitly when user wants to sync from backend
    }

    public LiveData<List<ProductEntity>> getProducts() {
        return products;
    }

    public LiveData<ApiResult<?>> getOperationResult() {
        return operationResult;
    }

    public void refresh() {
        LiveData<ApiResult<Void>> source = inventoryRepository.refreshProducts();
        operationResult.addSource(source, result -> {
            operationResult.setValue(result);
            operationResult.removeSource(source);
        });
    }

    public void createProduct(String name,
                              String category,
                              String supplier,
                              BigDecimal cost,
                              BigDecimal price,
                              int quantity) {
        ProductPayloadDto payload = new ProductPayloadDto(name, category, supplier, cost, price, quantity);
        LiveData<ApiResult<ProductEntity>> source = inventoryRepository.createProduct(payload);
        operationResult.addSource(source, result -> {
            operationResult.setValue(result);
            operationResult.removeSource(source);
        });
    }

    public void updateProduct(long id,
                              String name,
                              String category,
                              String supplier,
                              BigDecimal cost,
                              BigDecimal price,
                              int quantity) {
        ProductPayloadDto payload = new ProductPayloadDto(name, category, supplier, cost, price, quantity);
        LiveData<ApiResult<ProductEntity>> source = inventoryRepository.updateProduct(id, payload);
        operationResult.addSource(source, result -> {
            operationResult.setValue(result);
            operationResult.removeSource(source);
        });
    }

    public void deleteProduct(long id) {
        LiveData<ApiResult<Void>> source = inventoryRepository.deleteProduct(id);
        operationResult.addSource(source, result -> {
            operationResult.setValue(result);
            operationResult.removeSource(source);
        });
    }

    public void adjustStock(long id, int quantityChange) {
        LiveData<ApiResult<Void>> source = inventoryRepository.adjustStock(id, quantityChange);
        operationResult.addSource(source, result -> {
            operationResult.setValue(result);
            operationResult.removeSource(source);
        });
    }

    /**
     * Update product with auto-save: updates locally first, then syncs with backend
     */
    public void updateProductAutoSave(long id,
                                     String name,
                                     String category,
                                     String supplier,
                                     BigDecimal cost,
                                     BigDecimal price,
                                     int quantity,
                                     String status) {
        LiveData<ApiResult<ProductEntity>> source = inventoryRepository.updateProductLocalFirst(
                id, name, category, supplier, cost, price, quantity, status);
        operationResult.addSource(source, result -> {
            operationResult.setValue(result);
            operationResult.removeSource(source);
        });
    }
}

