package com.loretacafe.pos.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import com.loretacafe.pos.data.local.entity.SaleEntity;
import com.loretacafe.pos.data.local.model.SaleWithItems;

import java.time.OffsetDateTime;
import java.util.List;

@Dao
public interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SaleEntity sale);

    @Transaction
    @Query("SELECT * FROM sales ORDER BY sale_date DESC")
    LiveData<List<SaleWithItems>> observeSalesWithItems();

    @Transaction
    @Query("SELECT * FROM sales ORDER BY sale_date DESC")
    List<SaleWithItems> getAllSalesWithItems();

    @Transaction
    @Query("SELECT * FROM sales WHERE DATE(sale_date) = DATE('now', 'localtime') ORDER BY sale_date DESC")
    List<SaleWithItems> getTodaySalesWithItems();

    @Transaction
    @Query("SELECT * FROM sales WHERE strftime('%Y-%m', sale_date) = strftime('%Y-%m', 'now', 'localtime') ORDER BY sale_date DESC")
    List<SaleWithItems> getCurrentMonthSalesWithItems();

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE DATE(sale_date) = DATE('now', 'localtime')")
    double getGrossDailySales();

    @Query("SELECT COUNT(*) FROM sales WHERE DATE(sale_date) = DATE('now', 'localtime')")
    int getTotalOrdersToday();

    @Query("SELECT COALESCE(SUM(total_amount), 0) FROM sales WHERE strftime('%Y-%m', sale_date) = strftime('%Y-%m', 'now', 'localtime')")
    double getMonthlyRevenue();

    @Query("SELECT * FROM sales WHERE sale_id = :saleId LIMIT 1")
    SaleEntity getSaleById(long saleId);

    @Query("SELECT * FROM sales WHERE sale_date >= :startDate AND sale_date < :endDate ORDER BY sale_date ASC")
    List<SaleEntity> getSalesByDateRange(OffsetDateTime startDate, OffsetDateTime endDate);

    @Query("DELETE FROM sales WHERE sale_id = :saleId")
    void deleteSale(long saleId);

    @Query("DELETE FROM sales")
    void clear();

    @Query("SELECT order_number FROM sales ORDER BY CAST(SUBSTR(order_number, 2) AS INTEGER) DESC LIMIT 1")
    String getLastOrderNumber();

    @Query("SELECT MAX(CAST(SUBSTR(order_number, 2) AS INTEGER)) FROM sales WHERE order_number LIKE '#%'")
    Integer getMaxOrderNumber();

    /**
     * Get max order number for a specific year (format: YYYYNNNN)
     * Extracts the numeric part after the year prefix (e.g., "001" from "2025001")
     * Pattern: Remove year prefix, remaining is the sequential number
     */
    @Query("SELECT MAX(CAST(SUBSTR(order_number, LENGTH(CAST(:year AS TEXT)) + 1) AS INTEGER)) " +
           "FROM sales WHERE order_number LIKE CAST(:year AS TEXT) || '%'")
    Integer getMaxOrderNumberForYear(int year);
}

