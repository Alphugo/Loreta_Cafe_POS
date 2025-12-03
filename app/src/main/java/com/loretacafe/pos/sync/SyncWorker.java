package com.loretacafe.pos.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.loretacafe.pos.PosApp;
import com.loretacafe.pos.data.repository.SyncRepository;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        PosApp app = (PosApp) getApplicationContext();
        SyncRepository syncRepository = app.getRepositoryProvider().getSyncRepository();
        syncRepository.syncPending();
        return Result.success();
    }
}

