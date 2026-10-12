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

        float cx = BITMAP_SIZE / 2f;

        // Background Card Fill & Border
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#12161E"));
        canvas.drawRoundRect(2f, 2f, BITMAP_SIZE - 2f, BITMAP_SIZE - 2f, 16f, 16f, bgPaint);

        Paint bezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bezelPaint.setStyle(Paint.Style.STROKE);
        bezelPaint.setStrokeWidth(2f);
        bezelPaint.setColor(Color.parseColor("#2A313E"));
        canvas.drawRoundRect(2f, 2f, BITMAP_SIZE - 2f, BITMAP_SIZE - 2f, 16f, 16f, bezelPaint);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#263238"));
        gridPaint.setStrokeWidth(1f);

        Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(2.5f);
        linePaint.setColor(isConnected ? Color.parseColor("#00E5FF") : Color.parseColor("#64748B"));

        Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        fillPaint.setStyle(Paint.Style.FILL);

        // Chart Bounds
        float paddingLeft = 14f;
        float paddingRight = 14f;
        float chartTop = 18f;
        float chartBottom = 100f;
        float chartWidth = BITMAP_SIZE - paddingLeft - paddingRight;
        float chartHeight = chartBottom - chartTop;

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
            canvas.drawCircle(lastX, lastY, 3.5f, dotPaint);
        }

        // Draw Value / Disconnected Text
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (!isConnected)
        {
            textPaint.setTextSize(11f);
            textPaint.setColor(Color.parseColor("#E53935"));
            textPaint.setFakeBoldText(true);
            canvas.drawText("Disconnected", cx, 128f, textPaint);
        }
        else if (currentVal != null)
        {
            textPaint.setTextSize(18f);
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

            canvas.drawText(formattedVal, cx, 125f, textPaint);

            // Unit and Time Window Label
            String windowLabel = timeWindowSeconds >= 60 ? (timeWindowSeconds / 60) + "m" : timeWindowSeconds + "s";
            String subText = String.format("%s (%s)", unit != null ? unit : "", windowLabel).trim();

            textPaint.setTextSize(9f);
            textPaint.setColor(Color.parseColor("#94A3B8"));
            textPaint.setFakeBoldText(false);
            canvas.drawText(subText, cx, 142f, textPaint);
        }
        else
        {
            textPaint.setTextSize(16f);
            textPaint.setColor(Color.parseColor("#64748B"));
            canvas.drawText("--", cx, 128f, textPaint);
        }

        // Draw Title Below Chart
        if (title != null && !title.isEmpty())
        {
            textPaint.setTextSize(10f);
            textPaint.setColor(Color.parseColor("#CBD5E1"));
            textPaint.setFakeBoldText(false);

            float maxTitleWidth = BITMAP_SIZE - 16f;
            float currentWidth = textPaint.measureText(title);
            if (currentWidth > maxTitleWidth)
            {
                textPaint.setTextSize(Math.max(6f, 10f * (maxTitleWidth / currentWidth)));
            }

            canvas.drawText(title, cx, BITMAP_SIZE - 6f, textPaint);
        }

        return bitmap;
    }
}
