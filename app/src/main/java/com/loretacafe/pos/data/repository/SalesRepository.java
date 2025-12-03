package com.loretacafe.pos.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.loretacafe.pos.data.local.dao.PendingSyncDao;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.dao.SaleDao;
import com.loretacafe.pos.data.local.dao.SaleItemDao;
import com.loretacafe.pos.data.local.entity.PendingSyncEntity;
import com.loretacafe.pos.data.local.entity.PendingSyncType;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.entity.SaleItemEntity;
import com.loretacafe.pos.data.local.model.SaleWithItems;
import com.loretacafe.pos.data.mapper.DataMappers;
import com.loretacafe.pos.data.remote.api.SalesApi;
import com.loretacafe.pos.data.remote.dto.SaleRequestDto;
import com.loretacafe.pos.data.remote.dto.SaleResponseDto;
import com.loretacafe.pos.data.remote.dto.SaleResponseItemDto;
import com.loretacafe.pos.data.util.ApiResult;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class SalesRepository {

    private final SalesApi salesApi;
    private final SaleDao saleDao;
    private final SaleItemDao saleItemDao;
    private final ProductDao productDao;
    private final PendingSyncDao pendingSyncDao;
    private final Gson gson;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SalesRepository(SalesApi salesApi,
                           SaleDao saleDao,
                           SaleItemDao saleItemDao,
                           ProductDao productDao,
                           PendingSyncDao pendingSyncDao,
                           Gson gson) {
        this.salesApi = salesApi;
        this.saleDao = saleDao;
        this.saleItemDao = saleItemDao;
        this.productDao = productDao;
        this.pendingSyncDao = pendingSyncDao;
        this.gson = gson;
    }

    public LiveData<List<SaleWithItems>> observeSales() {
        return saleDao.observeSalesWithItems();
    }

    public LiveData<ApiResult<SaleResponseDto>> createSale(SaleRequestDto requestDto, boolean isOnline) {
        MutableLiveData<ApiResult<SaleResponseDto>> liveData = new MutableLiveData<>(ApiResult.loading());

        executorService.execute(() -> {
            if (isOnline) {
                try {
                    Response<SaleResponseDto> response = salesApi.createSale(requestDto).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        persistSale(response.body());
                        liveData.postValue(ApiResult.success(response.body()));
                    } else {
                        liveData.postValue(ApiResult.error(extractError(response)));
                    }
                } catch (IOException e) {
                    enqueuePending(requestDto);
                    applyLocalStockChange(requestDto);
                    liveData.postValue(ApiResult.error(e.getMessage()));
                }
            } else {
                enqueuePending(requestDto);
                applyLocalStockChange(requestDto);
                liveData.postValue(ApiResult.error("Offline mode - queued for sync"));
            }
        });

        return liveData;
    }

    private void persistSale(SaleResponseDto dto) {
        SaleEntity saleEntity = DataMappers.toEntity(dto);
        saleDao.insert(saleEntity);

        List<SaleItemEntity> itemEntities = DataMappers.toSaleItems(dto.getSaleId(), dto.getItems());
        saleItemDao.insertAll(itemEntities);

        if (dto.getItems() != null) {
            for (SaleResponseItemDto itemDto : dto.getItems()) {
                ProductEntity product = productDao.getById(itemDto.getProductId());
                if (product != null) {
                    product.setQuantity(Math.max(0, product.getQuantity() - itemDto.getQuantity()));
                    productDao.update(product);
                }
            }
        }
    }

    private void applyLocalStockChange(SaleRequestDto requestDto) {
        if (requestDto.getItems() == null) {
            return;
        }
        for (com.loretacafe.pos.data.remote.dto.SaleItemRequestDto itemDto : requestDto.getItems()) {
            ProductEntity entity = productDao.getById(itemDto.getProductId());
            if (entity != null) {
                entity.setQuantity(Math.max(0, entity.getQuantity() - itemDto.getQuantity()));
                productDao.update(entity);
            }
        }
    }

    private void enqueuePending(SaleRequestDto requestDto) {
        PendingSyncEntity pending = new PendingSyncEntity();
        pending.setType(PendingSyncType.CREATE_SALE);
        pending.setPayload(gson.toJson(requestDto));
        pending.setCreatedAt(OffsetDateTime.now());
        pendingSyncDao.insert(pending);
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

