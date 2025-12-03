package com.loretacafe.pos.util;

/**
 * Utility class to calculate stock status based on total inventory
 */
public class StockStatusCalculator {
    
    public enum StockStatus {
        HIGH,    // > 100 items
        MEDIUM,  // 30-100 items
        LOW      // < 30 items
    }
    
    public static class StockInfo {
        private final StockStatus status;
        private final String message;
        private final int colorResId;
        private final int imageResId;
        
        public StockInfo(StockStatus status, String message, int colorResId, int imageResId) {
            this.status = status;
            this.message = message;
            this.colorResId = colorResId;
            this.imageResId = imageResId;
        }
        
        public StockStatus getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public int getColorResId() {
            return colorResId;
        }
        
        public int getImageResId() {
            return imageResId;
        }
    }
    
    /**
     * Calculate stock status based on total inventory count
     * @param totalStock Total number of items in inventory
     * @return StockInfo with status, message, and color
     */
    public static StockInfo calculateStockStatus(int totalStock) {
        if (totalStock > 100) {
            return new StockInfo(
                StockStatus.HIGH,
                "All stocks are in\ngood condition.",
                android.R.color.holo_green_dark,
                com.loretacafe.pos.R.drawable.ic_stock_good
            );
        } else if (totalStock >= 30) {
            return new StockInfo(
                StockStatus.MEDIUM,
                "A few items need\nrefilling.",
                android.R.color.holo_orange_light,
                com.loretacafe.pos.R.drawable.ic_stock_medium
            );
        } else {
            return new StockInfo(
                StockStatus.LOW,
                "Some items are\nrunning low!",
                android.R.color.holo_red_light,
                com.loretacafe.pos.R.drawable.ic_stock_low
            );
        }
    }
}

