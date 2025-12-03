package com.loretacafe.pos.di;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loretacafe.pos.data.local.AppDatabase;
import com.loretacafe.pos.data.remote.ApiClient;
import com.loretacafe.pos.data.remote.api.AuthApi;
import com.loretacafe.pos.data.remote.api.InventoryApi;
import com.loretacafe.pos.data.remote.api.ReportsApi;
import com.loretacafe.pos.data.remote.api.SalesApi;
import com.loretacafe.pos.data.repository.AuthRepository;
import com.loretacafe.pos.data.repository.InventoryRepository;
import com.loretacafe.pos.data.repository.ReportRepository;
import com.loretacafe.pos.data.repository.SalesRepository;
import com.loretacafe.pos.data.repository.SyncRepository;
import com.loretacafe.pos.data.session.SessionManager;
import com.loretacafe.pos.data.util.AppExecutors;

import retrofit2.Retrofit;

public class RepositoryProvider {

    private final Context applicationContext;
    private final AppDatabase database;
    private final SessionManager sessionManager;
    private final Retrofit retrofit;
    private final Gson gson;
    private final AppExecutors executors;

    private AuthRepository authRepository;
    private InventoryRepository inventoryRepository;
    private SalesRepository salesRepository;
    private ReportRepository reportRepository;
    private SyncRepository syncRepository;

    public RepositoryProvider(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.sessionManager = new SessionManager(applicationContext);
        this.database = AppDatabase.getInstance(applicationContext);
        this.retrofit = ApiClient.getRetrofit(applicationContext);
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
                .create();
        this.executors = AppExecutors.createDefault();
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public AuthRepository getAuthRepository() {
        if (authRepository == null) {
            authRepository = new AuthRepository(
                    retrofit.create(AuthApi.class),
                    database.userDao(),
                    sessionManager,
                    executors.io(),
                    gson
            );
        }
        return authRepository;
    }

    public InventoryRepository getInventoryRepository() {
        if (inventoryRepository == null) {
            inventoryRepository = new InventoryRepository(
                    retrofit.create(InventoryApi.class),
                    database.productDao()
            );
        }
        return inventoryRepository;
    }

    public SalesRepository getSalesRepository() {
        if (salesRepository == null) {
            salesRepository = new SalesRepository(
                    retrofit.create(SalesApi.class),
                    database.saleDao(),
                    database.saleItemDao(),
                    database.productDao(),
                    database.pendingSyncDao(),
                    gson
            );
        }
        return salesRepository;
    }

    public ReportRepository getReportRepository() {
        if (reportRepository == null) {
            reportRepository = new ReportRepository(
                    retrofit.create(ReportsApi.class),
                    database.reportDao(),
                    sessionManager
            );
        }
        return reportRepository;
    }

    public SyncRepository getSyncRepository() {
        if (syncRepository == null) {
            syncRepository = new SyncRepository(
                    database.pendingSyncDao(),
                    retrofit.create(SalesApi.class),
                    retrofit.create(InventoryApi.class),
                    gson
            );
        }
        return syncRepository;
    }

    public AppDatabase getDatabase() {
        return database;
    }
}

