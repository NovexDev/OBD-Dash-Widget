package com.fr3ts0n.ecu.gui.androbd.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;

import java.util.List;
import java.util.Locale;

/**
 * Custom Canvas renderer for drawing live trend chart curves into a Bitmap for AppWidget.
 */
public class ObdChartRenderer
{
    private static final int BITMAP_SIZE = 180;

    public static Bitmap renderChart(
            String title,
            Float currentVal,
            String unit,
            List<ObdHistoryTracker.HistoryPoint> points,
            int timeWindowSeconds,
            boolean isConnected)
    {
        Bitmap bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#263238"));
        gridPaint.setStrokeWidth(1f);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setColor(isConnected ? Color.parseColor("#00E5FF") : Color.parseColor("#78909C"));

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        // Chart Bounds
        float paddingLeft = 12f;
        float paddingRight = 12f;
        float chartTop = 26f;
        float chartBottom = 115f;
        float chartWidth = BITMAP_SIZE - paddingLeft - paddingRight;
        float chartHeight = chartBottom - chartTop;

        // Draw Title (Top)
        textPaint.setTextSize(10f);
        textPaint.setColor(Color.parseColor("#B0BEC5"));
        textPaint.setFakeBoldText(true);
        if (title != null)
        {
            float maxTitleWidth = BITMAP_SIZE - 20f;
            float measuredWidth = textPaint.measureText(title);
            if (measuredWidth > maxTitleWidth)
            {
                textPaint.setTextSize(10f * (maxTitleWidth / measuredWidth));
            }
            canvas.drawText(title.toUpperCase(Locale.ROOT), BITMAP_SIZE / 2f, 16f, textPaint);
        }

        // Draw Grid Lines
        canvas.drawLine(paddingLeft, chartTop, paddingLeft + chartWidth, chartTop, gridPaint);
        canvas.drawLine(paddingLeft, chartTop + chartHeight / 2f, paddingLeft + chartWidth, chartTop + chartHeight / 2f, gridPaint);
        canvas.drawLine(paddingLeft, chartBottom, paddingLeft + chartWidth, chartBottom, gridPaint);

        // Calculate Min and Max from history points
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        if (points != null && !points.isEmpty())
        {
            for (ObdHistoryTracker.HistoryPoint p : points)
            {
                if (p.value < min) min = p.value;
                if (p.value > max) max = p.value;
            }
        }

        if (currentVal != null)
        {
            if (currentVal < min) min = currentVal;
            if (currentVal > max) max = currentVal;
        }

        if (min == Float.MAX_VALUE || max == -Float.MAX_VALUE || min == max)
        {
            min = 0f;
            max = (currentVal != null && currentVal > 0) ? currentVal * 1.2f : 100f;
        }

        // Add 5% padding to scale limits
        float range = max - min;
        if (range <= 0) range = 10f;
        min -= range * 0.05f;
        max += range * 0.05f;
        range = max - min;

        // Time limits
        long now = System.currentTimeMillis();
        long windowMs = Math.max(10000L, timeWindowSeconds * 1000L);
        long startTime = now - windowMs;

        // Draw Line Path
        if (isConnected && points != null && !points.isEmpty())
        {
            Path linePath = new Path();
            Path fillPath = new Path();

            boolean first = true;
            float lastX = paddingLeft + chartWidth;
            float lastY = chartBottom;

            for (ObdHistoryTracker.HistoryPoint p : points)
            {
                float timeRatio = (p.timestampMs - startTime) / (float) windowMs;
                timeRatio = Math.max(0f, Math.min(1f, timeRatio));
                float x = paddingLeft + timeRatio * chartWidth;

                float valRatio = (p.value - min) / range;
                valRatio = Math.max(0f, Math.min(1f, valRatio));
                float y = chartBottom - valRatio * chartHeight;

                if (first)
                {
                    linePath.moveTo(x, y);
                    fillPath.moveTo(x, chartBottom);
                    fillPath.lineTo(x, y);
                    first = false;
                }
                else
                {
                    linePath.lineTo(x, y);
                    fillPath.lineTo(x, y);
                }
                lastX = x;
                lastY = y;
            }

            fillPath.lineTo(lastX, chartBottom);
            fillPath.close();

            // Fill under curve
            Shader fillShader = new LinearGradient(
                    0, chartTop, 0, chartBottom,
                    Color.parseColor("#4000E5FF"), Color.parseColor("#0000E5FF"),
                    Shader.TileMode.CLAMP
            );
            fillPaint.setShader(fillShader);
            canvas.drawPath(fillPath, fillPaint);

            // Draw Curve
            canvas.drawPath(linePath, linePaint);

            // Draw Latest Point Dot
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(Color.parseColor("#00E5FF"));
            canvas.drawCircle(lastX, lastY, 4f, dotPaint);
        }

        // Draw Bottom Readout
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (!isConnected)
        {
            textPaint.setTextSize(14f);
            textPaint.setColor(Color.parseColor("#FF5252"));
            textPaint.setFakeBoldText(true);
            canvas.drawText("Disconnected", BITMAP_SIZE / 2f, 145f, textPaint);
        }
        else if (currentVal != null)
        {
            textPaint.setTextSize(22f);
            textPaint.setColor(Color.WHITE);
            textPaint.setFakeBoldText(true);

            String formattedVal;
            if (currentVal.floatValue() == (float) Math.round(currentVal.floatValue()))
            {
                formattedVal = String.valueOf(currentVal.intValue());
            }
            else
            {
                formattedVal = String.format(Locale.getDefault(), "%.1f", currentVal);
            }

            canvas.drawText(formattedVal, BITMAP_SIZE / 2f, 142f, textPaint);

            // Unit and Time Window Label
            String windowLabel = timeWindowSeconds >= 60 ? (timeWindowSeconds / 60) + "m" : timeWindowSeconds + "s";
            String subText = String.format("%s (%s Verlauf)", unit != null ? unit : "", windowLabel).trim();

            textPaint.setTextSize(9f);
            textPaint.setColor(Color.parseColor("#80CBC4"));
            textPaint.setFakeBoldText(false);
            canvas.drawText(subText, BITMAP_SIZE / 2f, 162f, textPaint);
        }
        else
        {
            textPaint.setTextSize(18f);
            textPaint.setColor(Color.parseColor("#78909C"));
            canvas.drawText("--", BITMAP_SIZE / 2f, 145f, textPaint);
        }

        return bitmap;
    }
}
