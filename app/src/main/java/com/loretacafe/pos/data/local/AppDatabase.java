package com.loretacafe.pos.data.local;

import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.loretacafe.pos.data.local.converter.RoomConverters;
import com.loretacafe.pos.data.local.dao.CategoryDao;
import com.loretacafe.pos.data.local.dao.IngredientDao;
import com.loretacafe.pos.data.local.dao.IngredientDeductionDao;
import com.loretacafe.pos.data.local.dao.PendingSyncDao;
import com.loretacafe.pos.data.local.dao.ProductDao;
import com.loretacafe.pos.data.local.dao.ReportDao;
import com.loretacafe.pos.data.local.dao.RecipeDao;
import com.loretacafe.pos.data.local.dao.SaleDao;
import com.loretacafe.pos.data.local.dao.SaleItemDao;
import com.loretacafe.pos.data.local.dao.ShiftDao;
import com.loretacafe.pos.data.local.dao.UserDao;
import com.loretacafe.pos.data.local.dao.VerificationCodeDao;
import com.loretacafe.pos.data.local.entity.CategoryEntity;
import com.loretacafe.pos.data.local.entity.IngredientEntity;
import com.loretacafe.pos.data.local.entity.IngredientDeductionEntity;
import com.loretacafe.pos.data.local.entity.PendingSyncEntity;
import com.loretacafe.pos.data.local.entity.ProductEntity;
import com.loretacafe.pos.data.local.entity.ReportEntity;
import com.loretacafe.pos.data.local.entity.RecipeEntity;
import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.entity.SaleItemEntity;
import com.loretacafe.pos.data.local.entity.ShiftEntity;
import com.loretacafe.pos.data.local.entity.UserEntity;
import com.loretacafe.pos.data.local.entity.VerificationCodeEntity;

@Database(
        entities = {
                UserEntity.class,
                ProductEntity.class,
                SaleEntity.class,
                SaleItemEntity.class,
                ReportEntity.class,
                PendingSyncEntity.class,
                VerificationCodeEntity.class,
                IngredientEntity.class,
                CategoryEntity.class,
                ShiftEntity.class,
                RecipeEntity.class,
                IngredientDeductionEntity.class
        },
        version = 9,
        exportSchema = true
)
@TypeConverters(RoomConverters.class)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DB_NAME = "loreta_pos.db";
    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();

    public abstract ProductDao productDao();

    public abstract SaleDao saleDao();

    public abstract SaleItemDao saleItemDao();

    public abstract ReportDao reportDao();

    public abstract PendingSyncDao pendingSyncDao();

    public abstract VerificationCodeDao verificationCodeDao();

    public abstract IngredientDao ingredientDao();

    public abstract CategoryDao categoryDao();

    public abstract ShiftDao shiftDao();

    public abstract RecipeDao recipeDao();

    public abstract IngredientDeductionDao ingredientDeductionDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DB_NAME
                            )
                            .fallbackToDestructiveMigration() // Automatically drops and recreates tables on version mismatch
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    @VisibleForTesting
    public static void resetInstance() {
        INSTANCE = null;
    }
    
    /**
     * Force reset the database instance (useful when schema changes)
     * Call this before getInstance() if you need to ensure a fresh database connection
     */
    public static void forceResetInstance() {
        synchronized (AppDatabase.class) {
            if (INSTANCE != null) {
                INSTANCE.close();
            }
            INSTANCE = null;
        }
    }
}

