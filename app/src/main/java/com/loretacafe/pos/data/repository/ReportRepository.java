package com.loretacafe.pos.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.loretacafe.pos.data.local.dao.ReportDao;
import com.loretacafe.pos.data.local.entity.ReportEntity;
import com.loretacafe.pos.data.mapper.DataMappers;
import com.loretacafe.pos.data.remote.api.ReportsApi;
import com.loretacafe.pos.data.remote.dto.SalesReportRequestDto;
import com.loretacafe.pos.data.remote.dto.SalesSummaryDto;
import com.loretacafe.pos.data.session.SessionManager;
import com.loretacafe.pos.data.util.ApiResult;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Response;

public class ReportRepository {

    private final ReportsApi reportsApi;
    private final ReportDao reportDao;
    private final SessionManager sessionManager;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public ReportRepository(ReportsApi reportsApi, ReportDao reportDao, SessionManager sessionManager) {
        this.reportsApi = reportsApi;
        this.reportDao = reportDao;
        this.sessionManager = sessionManager;
    }

    public LiveData<java.util.List<ReportEntity>> observeReports() {
        return reportDao.observeReports();
    }

    public LiveData<ApiResult<SalesSummaryDto>> generateReport(String type, OffsetDateTime start, OffsetDateTime end) {
        MutableLiveData<ApiResult<SalesSummaryDto>> liveData = new MutableLiveData<>(ApiResult.loading());

        executorService.execute(() -> {
            try {
                SalesReportRequestDto request = new SalesReportRequestDto(
                        type,
                        formatDate(start),
                        formatDate(end)
                );
                Response<SalesSummaryDto> response = reportsApi.generateSalesReport(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    SalesSummaryDto dto = response.body();
                    long userId = sessionManager.getUserId();
                    ReportEntity entity = DataMappers.toEntity(dto, type, userId);
                    reportDao.insert(entity);
                    liveData.postValue(ApiResult.success(dto));
                } else {
                    liveData.postValue(ApiResult.error(extractError(response)));
                }
            } catch (IOException e) {
                liveData.postValue(ApiResult.error(e.getMessage()));
            }
        });

        return liveData;
    }

    private String formatDate(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
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

