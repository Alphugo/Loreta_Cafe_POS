package com.loretacafe.pos.ui.reports;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.loretacafe.pos.PosApp;
import com.loretacafe.pos.data.remote.dto.SalesSummaryDto;
import com.loretacafe.pos.data.repository.ReportRepository;
import com.loretacafe.pos.data.util.ApiResult;

import java.time.OffsetDateTime;

public class ReportsViewModel extends AndroidViewModel {

    private final ReportRepository reportRepository;

    private final MediatorLiveData<ApiResult<SalesSummaryDto>> salesSummary = new MediatorLiveData<>();

    public ReportsViewModel(@NonNull Application application) {
        super(application);
        PosApp app = (PosApp) application;
        this.reportRepository = app.getRepositoryProvider().getReportRepository();
    }

    public LiveData<ApiResult<SalesSummaryDto>> getSalesSummary() {
        return salesSummary;
    }

    public void loadReport(String type, OffsetDateTime start, OffsetDateTime end) {
        LiveData<ApiResult<SalesSummaryDto>> source =
                reportRepository.generateReport(type, start, end);
        salesSummary.addSource(source, result -> {
            salesSummary.setValue(result);
            if (result.getStatus() != ApiResult.Status.LOADING) {
                salesSummary.removeSource(source);
            }
        });
    }
}


