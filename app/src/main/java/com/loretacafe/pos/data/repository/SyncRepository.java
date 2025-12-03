package com.loretacafe.pos.data.repository;

import android.util.Log;

import com.google.gson.Gson;
import com.loretacafe.pos.data.local.dao.PendingSyncDao;
import com.loretacafe.pos.data.local.entity.PendingSyncEntity;
import com.loretacafe.pos.data.local.entity.PendingSyncType;
import com.loretacafe.pos.data.remote.api.InventoryApi;
import com.loretacafe.pos.data.remote.api.SalesApi;
import com.loretacafe.pos.data.remote.dto.SaleRequestDto;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class SyncRepository {

    private static final String TAG = "SyncRepository";
    private static final int MAX_RETRIES = 5;

    private final PendingSyncDao pendingSyncDao;
    private final SalesApi salesApi;
    private final InventoryApi inventoryApi;
    private final Gson gson;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public SyncRepository(PendingSyncDao pendingSyncDao,
                          SalesApi salesApi,
                          InventoryApi inventoryApi,
                          Gson gson) {
        this.pendingSyncDao = pendingSyncDao;
        this.salesApi = salesApi;
        this.inventoryApi = inventoryApi;
        this.gson = gson;
    }

    public void syncPending() {
        executorService.execute(() -> {
            try {
                List<PendingSyncEntity> pendingList = pendingSyncDao.getPending();
                if (pendingList == null || pendingList.isEmpty()) {
                    return; // No pending items to sync
                }
                
                for (PendingSyncEntity pending : pendingList) {
                    boolean success = false;
                    try {
                        success = processPending(pending);
                    } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
                        // Network timeout or connection failure - fail silently for offline mode
                        Log.d(TAG, "Sync skipped (offline mode): " + e.getClass().getSimpleName());
                        // Keep pending item for later sync when online
                        continue;
                    } catch (IOException e) {
                        Log.d(TAG, "Sync failed (offline mode): " + e.getMessage());
                        // Keep pending item for later sync when online
                        continue;
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected sync error", e);
                        // Keep pending item for later sync when online
                        continue;
                    }

                    if (success) {
                        pendingSyncDao.delete(pending);
                    } else {
                        pending.setRetryCount(pending.getRetryCount() + 1);
                        if (pending.getRetryCount() >= MAX_RETRIES) {
                            pendingSyncDao.delete(pending);
                        } else {
                            pendingSyncDao.update(pending);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error in syncPending", e);
            }
        });
    }

    private boolean processPending(PendingSyncEntity pending) throws IOException {
        // Skip sync in offline mode - return false to keep pending
        // This prevents network timeout errors
        if (salesApi == null || inventoryApi == null) {
            return false;
        }
        
        if (pending.getType() == PendingSyncType.CREATE_SALE) {
            try {
                SaleRequestDto requestDto = gson.fromJson(pending.getPayload(), SaleRequestDto.class);
                Response<?> response = salesApi.createSale(requestDto).execute();
                return response.isSuccessful();
            } catch (java.net.SocketTimeoutException | java.net.ConnectException e) {
                // Network unavailable - fail silently
                throw new IOException("Network unavailable", e);
            }
        }
        // Other sync operations (inventory, etc.) can be added here.
        return false;
    }
}

