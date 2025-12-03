package com.loretacafe.pos.ui.order;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.loretacafe.pos.CartItem;
import com.loretacafe.pos.PosApp;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.remote.dto.SaleItemRequestDto;
import com.loretacafe.pos.data.remote.dto.SaleRequestDto;
import com.loretacafe.pos.data.remote.dto.SaleResponseDto;
import com.loretacafe.pos.data.repository.InventoryRepository;
import com.loretacafe.pos.data.repository.SalesRepository;
import com.loretacafe.pos.data.session.SessionManager;
import com.loretacafe.pos.data.util.ApiResult;

import java.util.ArrayList;
import java.util.List;

public class OrderViewModel extends AndroidViewModel {

    private final InventoryRepository inventoryRepository;
    private final SalesRepository salesRepository;
    private final SessionManager sessionManager;

    private final LiveData<List<ProductEntity>> products;
    // Result of submitting a sale (checkout)
    private final MediatorLiveData<ApiResult<SaleResponseDto>> saleResult = new MediatorLiveData<>();
    // Result of refreshing products from the backend into Room
    private final MediatorLiveData<ApiResult<Void>> productRefreshResult = new MediatorLiveData<>();

    public OrderViewModel(@NonNull Application application) {
        super(application);
        PosApp app = (PosApp) application;
        this.inventoryRepository = app.getRepositoryProvider().getInventoryRepository();
        this.salesRepository = app.getRepositoryProvider().getSalesRepository();
        this.sessionManager = app.getRepositoryProvider().getSessionManager();
        this.products = inventoryRepository.observeProducts();
        // Don't auto-refresh on creation - let activities call refreshProducts() explicitly
        // This prevents clearing local data when backend is unavailable
        // refreshProducts();
    }

    public LiveData<List<ProductEntity>> getProducts() {
        return products;
    }

    public LiveData<ApiResult<SaleResponseDto>> getSaleResult() {
        return saleResult;
    }

    public LiveData<ApiResult<Void>> getProductRefreshResult() {
        return productRefreshResult;
    }

    public void refreshProducts() {
        LiveData<ApiResult<Void>> source = inventoryRepository.refreshProducts();
        productRefreshResult.addSource(source, result -> {
            productRefreshResult.setValue(result);
            if (result.getStatus() != ApiResult.Status.LOADING) {
                productRefreshResult.removeSource(source);
            }
        });
    }

    public void submitSale(List<SaleItemRequestDto> items) {
        long cashierId = sessionManager.getUserId();
        if (cashierId <= 0) {
            saleResult.setValue(ApiResult.error("Session expired. Please log in again."));
            return;
        }

        SaleRequestDto requestDto = new SaleRequestDto(cashierId, items);
        LiveData<ApiResult<SaleResponseDto>> source = salesRepository.createSale(requestDto, true);
        saleResult.addSource(source, result -> {
            saleResult.setValue(result);
            if (result.getStatus() != ApiResult.Status.LOADING) {
                saleResult.removeSource(source);
            }
        });
    }

    public List<SaleItemRequestDto> mapCartItems(List<CartItem> cartItems) {
        List<SaleItemRequestDto> saleItems = new ArrayList<>();
        if (cartItems == null) {
            return saleItems;
        }
        for (CartItem cartItem : cartItems) {
            saleItems.add(new SaleItemRequestDto(cartItem.getProductId(), cartItem.getQuantity()));
        }
        return saleItems;
    }
}

