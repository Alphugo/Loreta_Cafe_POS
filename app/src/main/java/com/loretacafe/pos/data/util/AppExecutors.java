package com.loretacafe.pos.data.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class AppExecutors {

    private final ExecutorService ioExecutor;

    private AppExecutors(int poolSize) {
        this.ioExecutor = Executors.newFixedThreadPool(poolSize);
    }

    public static AppExecutors createDefault() {
        int cores = Runtime.getRuntime().availableProcessors();
        // Ensure at least two threads so disk + network work can overlap
        int poolSize = Math.max(2, cores);
        return new AppExecutors(poolSize);
    }

    public ExecutorService io() {
        return ioExecutor;
    }
}

