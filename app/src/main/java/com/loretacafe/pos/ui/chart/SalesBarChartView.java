package com.loretacafe.pos.ui.chart;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SalesBarChartView extends View {

    private List<BarData> barDataList = new ArrayList<>();
    private Paint barPaint;
    private Paint textPaint;
    private Paint axisPaint;
    private int selectedIndex = -1;
    private OnBarClickListener onBarClickListener;

    private static final int MAX_BARS = 7;
    // Using theme colors: deep_brown (#603813) and warm_tan (#E0B07D)
    private static final int[] BAR_COLORS = {0xFF603813, 0xFFE0B07D}; // Primary brown and warm tan
    private static final int SELECTED_COLOR = 0xFF3E3328; // Dark taupe for selected

    public interface OnBarClickListener {
        void onBarClick(int index, BarData data);
    }

    public static class BarData {
        public String label;
        public float value;
        public String date;

        public BarData(String label, float value, String date) {
            this.label = label;
            this.value = value;
            this.date = date;
        }
    }

    public SalesBarChartView(Context context) {
        super(context);
        init();
    }

    public SalesBarChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SalesBarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF3E3328); // dark_taupe for better contrast
        textPaint.setTextSize(36f); // 12sp converted to pixels
        textPaint.setTextAlign(Paint.Align.CENTER);

        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(0xFFD6C7B4); // colorOutline for subtle axis lines
        axisPaint.setStrokeWidth(1.5f);
    }

    public void setBarData(List<BarData> data) {
        if (data != null && data.size() <= MAX_BARS) {
            this.barDataList = new ArrayList<>(data);
        } else if (data != null && data.size() > MAX_BARS) {
            // Take only the last 7 items
            this.barDataList = new ArrayList<>(data.subList(data.size() - MAX_BARS, data.size()));
        } else {
            this.barDataList = new ArrayList<>();
        }
        invalidate();
    }

    public void setOnBarClickListener(OnBarClickListener listener) {
        this.onBarClickListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (barDataList.isEmpty()) {
            return;
        }

        int width = getWidth();
        int height = getHeight();

        // Use larger left padding so Y-axis labels (0–1000+) never overlap bars
        int paddingLeft = 96;
        int paddingRight = 48;
        int paddingTop = 48;
        int paddingBottom = 48;

        int chartWidth = width - paddingLeft - paddingRight;
        int chartHeight = height - paddingTop - paddingBottom;

        // Find max value for scaling
        float maxValue = 0;
        for (BarData data : barDataList) {
            if (data.value > maxValue) {
                maxValue = data.value;
            }
        }
        if (maxValue == 0) maxValue = 10000; // Default max

        // Draw Y-axis labels
        int yAxisSteps = 5;
        textPaint.setTextSize(28f); // Slightly smaller for better fit on small screens
        textPaint.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= yAxisSteps; i++) {
            float value = maxValue * (yAxisSteps - i) / yAxisSteps;
            float y = paddingTop + (chartHeight * i / yAxisSteps);
            // Position labels just to the left of the chart area
            canvas.drawText(String.format("%.0f", value),
                    paddingLeft - 8,
                    y + textPaint.getTextSize() / 3,
                    textPaint);
        }

        // Draw bars
        int barCount = barDataList.size();
        float barWidth = chartWidth / (barCount * 2f);
        float spacing = barWidth;

        for (int i = 0; i < barCount; i++) {
            BarData data = barDataList.get(i);
            float barHeight = (data.value / maxValue) * chartHeight;
            float left = paddingLeft + (i * (barWidth + spacing)) + spacing / 2;
            float right = left + barWidth;
            float top = paddingTop + chartHeight - barHeight;
            float bottom = paddingTop + chartHeight;

            // Set bar color
            if (selectedIndex == i) {
                barPaint.setColor(SELECTED_COLOR);
            } else {
                barPaint.setColor(BAR_COLORS[i % BAR_COLORS.length]);
            }

            // Draw bar
            RectF barRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(barRect, 4f, 4f, barPaint);

            // Draw X-axis label - show every other label to prevent overlap
            // For day view: show all labels but smaller, for month view: show every 2nd label
            boolean shouldShowLabel = (i % 2 == 0) || barCount <= 4; // Show all if 4 or fewer bars
            if (shouldShowLabel) {
                float labelY = paddingTop + chartHeight + 36;
                textPaint.setTextSize(28f); // Smaller text to prevent overlap
                textPaint.setColor(0xFF3E3328); // dark_taupe for labels
                
                // Rotate labels slightly or use shorter text
                String displayLabel = data.label;
                // Truncate long labels
                if (displayLabel.length() > 8) {
                    displayLabel = displayLabel.substring(0, 6) + "..";
                }
                
                canvas.drawText(displayLabel, left + barWidth / 2, labelY, textPaint);
            }
        }

        // Draw X-axis line
        canvas.drawLine(paddingLeft, paddingTop + chartHeight, paddingLeft + chartWidth, paddingTop + chartHeight, axisPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && onBarClickListener != null) {
            int width = getWidth();
            int height = getHeight();
            int padding = 40;
            int chartWidth = width - (padding * 2);
            int chartHeight = height - (padding * 2);

            int barCount = barDataList.size();
            float barWidth = chartWidth / (barCount * 2f);
            float spacing = barWidth;

            float x = event.getX();
            float y = event.getY();

            for (int i = 0; i < barCount; i++) {
                float left = padding + (i * (barWidth + spacing)) + spacing / 2;
                float right = left + barWidth;
                float top = padding;
                float bottom = padding + chartHeight;

                if (x >= left && x <= right && y >= top && y <= bottom) {
                    selectedIndex = i;
                    invalidate();
                    if (onBarClickListener != null) {
                        onBarClickListener.onBarClick(i, barDataList.get(i));
                    }
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }
}
