package com.loretacafe.pos.data.remote.api;

import com.loretacafe.pos.data.remote.dto.ApiResponseDto;
import com.loretacafe.pos.data.remote.dto.SalesReportEmailRequestDto;
import com.loretacafe.pos.data.remote.dto.SalesReportRequestDto;
import com.loretacafe.pos.data.remote.dto.SalesSummaryDto;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ReportsApi {

    @POST("api/reports/sales")
    Call<SalesSummaryDto> generateSalesReport(@Body SalesReportRequestDto body);
    
    @POST("api/send-sales-report")
    Call<ApiResponseDto> sendSalesReportEmail(@Body SalesReportEmailRequestDto request);
}

