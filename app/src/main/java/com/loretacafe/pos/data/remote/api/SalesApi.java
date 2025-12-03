package com.loretacafe.pos.data.remote.api;

import com.loretacafe.pos.data.remote.dto.SaleRequestDto;
import com.loretacafe.pos.data.remote.dto.SaleResponseDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface SalesApi {

    @POST("api/sales")
    Call<SaleResponseDto> createSale(@Body SaleRequestDto body);
}

