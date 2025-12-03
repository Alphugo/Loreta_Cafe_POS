package com.loretacafe.pos.data.remote.api;

import com.loretacafe.pos.data.remote.dto.ProductPayloadDto;
import com.loretacafe.pos.data.remote.dto.ProductResponseDto;
import com.loretacafe.pos.data.remote.dto.StockAdjustmentRequestDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface InventoryApi {

    @GET("api/inventory")
    Call<List<ProductResponseDto>> getProducts();

    @POST("api/inventory")
    Call<ProductResponseDto> createProduct(@Body ProductPayloadDto body);

    @PUT("api/inventory/{id}")
    Call<ProductResponseDto> updateProduct(@Path("id") long id, @Body ProductPayloadDto body);

    @DELETE("api/inventory/{id}")
    Call<Void> deleteProduct(@Path("id") long id);

    @POST("api/inventory/adjust-stock")
    Call<Void> adjustStock(@Body StockAdjustmentRequestDto body);
}

