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
 * Custom Canvas renderer for drawing live trend chart views matching Card 3 in the mockup.
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

        // --- Top Header: Chart Icon + Title ---
        float iconX = 14f;
        float iconY = 16f;

        // Draw Chart Icon
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        iconPaint.setColor(Color.parseColor("#E2E8F0"));
        iconPaint.setStrokeWidth(1.5f);
        iconPaint.setStyle(Paint.Style.STROKE);

        Path chartIconPath = new Path();
        chartIconPath.moveTo(iconX, iconY + 3f);
        chartIconPath.lineTo(iconX + 3f, iconY - 2f);
        chartIconPath.lineTo(iconX + 6f, iconY + 1f);
        chartIconPath.lineTo(iconX + 10f, iconY - 4f);
        canvas.drawPath(chartIconPath, iconPaint);

        // Draw Uppercase Title
        if (title != null && !title.isEmpty())
        {
            textPaint.setTextAlign(Paint.Align.LEFT);
            textPaint.setTextSize(9.5f);
            textPaint.setColor(Color.parseColor("#E2E8F0"));
            textPaint.setFakeBoldText(true);

            String uppercaseTitle = title.toUpperCase(Locale.ROOT);
            float maxTitleWidth = BITMAP_SIZE - iconX - 22f;
            float currentWidth = textPaint.measureText(uppercaseTitle);
            if (currentWidth > maxTitleWidth)
            {
                textPaint.setTextSize(Math.max(6f, 9.5f * (maxTitleWidth / currentWidth)));
            }

            canvas.drawText(uppercaseTitle, iconX + 15f, iconY + 3.5f, textPaint);
        }

        // --- Chart Area & Grid ---
        float paddingLeft = 14f;
        float paddingRight = 36f; // Space for right-side Y-axis labels
        float chartTop = 32f;
        float chartBottom = 125f;
        float chartWidth = BITMAP_SIZE - paddingLeft - paddingRight;
        float chartHeight = chartBottom - chartTop;

        Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#1F2735"));
        gridPaint.setStrokeWidth(1f);

        // Horizontal Grid Lines
        canvas.drawLine(paddingLeft, chartTop, paddingLeft + chartWidth, chartTop, gridPaint);
        canvas.drawLine(paddingLeft, chartTop + chartHeight / 2f, paddingLeft + chartWidth, chartTop + chartHeight / 2f, gridPaint);
        canvas.drawLine(paddingLeft, chartBottom, paddingLeft + chartWidth, chartBottom, gridPaint);

        // Vertical Grid Lines
        int numVertLines = 4;
        for (int i = 0; i <= numVertLines; i++)
        {
            float vx = paddingLeft + (i / (float) numVertLines) * chartWidth;
            canvas.drawLine(vx, chartTop, vx, chartBottom, gridPaint);
        }

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

        // Draw Right Y-Axis Labels (100%, 50%, 0% or max, mid, min)
        Paint yLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        yLabelPaint.setTextAlign(Paint.Align.LEFT);
        yLabelPaint.setTextSize(8f);
        yLabelPaint.setColor(Color.parseColor("#94A3B8"));

        boolean isPercent = "%".equals(unit) || max <= 100f;
        String maxText = isPercent ? String.format("%.0f%%", max) : String.format("%.0f", max);
        String midText = isPercent ? String.format("%.0f%%", min + range / 2f) : String.format("%.0f", min + range / 2f);
        String minText = isPercent ? String.format("%.0f%%", min) : String.format("%.0f", min);

        canvas.drawText(maxText, paddingLeft + chartWidth + 4f, chartTop + 3f, yLabelPaint);
        canvas.drawText(midText, paddingLeft + chartWidth + 4f, chartTop + chartHeight / 2f + 3f, yLabelPaint);
        canvas.drawText(minText, paddingLeft + chartWidth + 4f, chartBottom + 3f, yLabelPaint);

        // Time limits
        long now = System.currentTimeMillis();
        long windowMs = Math.max(10000L, timeWindowSeconds * 1000L);
        long startTime = now - windowMs;

        // Draw Red Line Curve + Translucent Gradient Fill
        if (isConnected && points != null && !points.isEmpty())
        {
            Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            linePaint.setStyle(Paint.Style.STROKE);
            linePaint.setStrokeWidth(2.5f);
            linePaint.setColor(Color.parseColor("#E53935"));

            Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fillPaint.setStyle(Paint.Style.FILL);

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

            // Gradient fill under red curve
            Shader fillShader = new LinearGradient(
                    0, chartTop, 0, chartBottom,
                    Color.parseColor("#50E53935"), Color.parseColor("#00E53935"),
                    Shader.TileMode.CLAMP
            );
            fillPaint.setShader(fillShader);
            canvas.drawPath(fillPath, fillPaint);

            // Draw Curve
            canvas.drawPath(linePath, linePaint);

            // Draw Red End Dot
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(Color.parseColor("#E53935"));
            canvas.drawCircle(lastX, lastY, 3.5f, dotPaint);
        }

        // Draw X-Axis Time Labels (Bottom: 2:00  1:30  1:00  0:30  0:00)
        Paint xLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        xLabelPaint.setTextAlign(Paint.Align.CENTER);
        xLabelPaint.setTextSize(8f);
        xLabelPaint.setColor(Color.parseColor("#94A3B8"));

        int numTimeTicks = 5;
        for (int i = 0; i < numTimeTicks; i++)
        {
            float ratio = i / (float) (numTimeTicks - 1);
            float tx = paddingLeft + ratio * chartWidth;
            int remainingSecs = Math.round((1f - ratio) * timeWindowSeconds);
            int mins = remainingSecs / 60;
            int secs = remainingSecs % 60;
            String timeLabel = String.format(Locale.US, "%d:%02d", mins, secs);

            canvas.drawText(timeLabel, tx, chartBottom + 13f, xLabelPaint);
        }

        // Draw Subtitle Below X-Axis (e.g. "Letzte 2 Minuten")
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(9f);
        textPaint.setColor(Color.parseColor("#94A3B8"));
        textPaint.setFakeBoldText(false);

        String windowLabel = timeWindowSeconds >= 60 ? (timeWindowSeconds / 60) + " Minute" + (timeWindowSeconds >= 120 ? "n" : "") : timeWindowSeconds + " Sekunden";
        String subText = String.format("Letzte %s", windowLabel);

        canvas.drawText(subText, paddingLeft + chartWidth / 2f, BITMAP_SIZE - 6f, textPaint);

        return bitmap;
    }
}
