package com.loretacafe.pos.data.local;

import android.util.Log;

import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.entity.ProductEntity;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SeedDataInitializer {

    private static final String TAG = "SeedDataInitializer";

    private SeedDataInitializer() {
    }

    public static void seed(ProductDao productDao) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                if (productDao.getById(-1) != null) {
                    return;
                }
                ProductEntity espresso = new ProductEntity();
                espresso.setId(-1);
                espresso.setName("Espresso");
                espresso.setCategory("Beverage");
                espresso.setSupplier("Loreta Cafe");
                espresso.setCost(new BigDecimal("50.00"));
                espresso.setPrice(new BigDecimal("120.00"));
                espresso.setQuantity(50);
                espresso.setStatus("IN_STOCK");
                productDao.insert(espresso);

                ProductEntity cheesecake = new ProductEntity();
                cheesecake.setId(-2);
                cheesecake.setName("Oreo Cheesecake");
                cheesecake.setCategory("Dessert");
                cheesecake.setSupplier("Loreta Cafe");
                cheesecake.setCost(new BigDecimal("80.00"));
                cheesecake.setPrice(new BigDecimal("150.00"));
                cheesecake.setQuantity(20);
                cheesecake.setStatus("IN_STOCK");
                productDao.insert(cheesecake);
            } catch (Exception e) {
                Log.e(TAG, "Failed to seed data", e);
            }
        });
    }
}

