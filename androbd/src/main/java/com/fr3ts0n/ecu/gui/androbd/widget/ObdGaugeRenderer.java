package com.fr3ts0n.ecu.gui.androbd.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.Locale;

/**
 * Custom Canvas renderer for drawing minimalist analog gauge views into a Bitmap for AppWidget.
 */
public class ObdGaugeRenderer
{
    private static final int BITMAP_SIZE = 180;
    private static final float START_ANGLE = 140f;
    private static final float SWEEP_ANGLE = 260f;

    public static Bitmap renderGauge(
            String title,
            Float value,
            String unit,
            float minVal,
            float maxVal,
            float redlineVal,
            boolean isConnected)
    {
        Bitmap bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        // Paints
        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(10f);
        trackPaint.setColor(Color.parseColor("#263238"));
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        Paint normalArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        normalArcPaint.setStyle(Paint.Style.STROKE);
        normalArcPaint.setStrokeWidth(10f);
        normalArcPaint.setColor(Color.parseColor("#00E5FF"));
        normalArcPaint.setStrokeCap(Paint.Cap.ROUND);

        Paint redlineArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redlineArcPaint.setStyle(Paint.Style.STROKE);
        redlineArcPaint.setStrokeWidth(10f);
        redlineArcPaint.setColor(Color.parseColor("#FF5252"));
        redlineArcPaint.setStrokeCap(Paint.Cap.ROUND);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setColor(isConnected ? Color.parseColor("#FF3D00") : Color.parseColor("#78909C"));

        Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setStyle(Paint.Style.FILL);
        hubPaint.setColor(Color.parseColor("#ECEFF1"));

        // Dimensions
        float cx = BITMAP_SIZE / 2f;
        float cy = BITMAP_SIZE / 2f;
        float radius = BITMAP_SIZE * 0.38f;
        RectF arcBounds = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        // Draw Base Track
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, trackPaint);

        // Calculate Redline Portion
        float effectiveMax = maxVal > minVal ? maxVal : minVal + 100f;
        float effectiveRedline = Math.max(minVal, Math.min(effectiveMax, redlineVal));

        float normalRatio = (effectiveRedline - minVal) / (effectiveMax - minVal);
        float normalSweep = SWEEP_ANGLE * normalRatio;
        float redlineSweep = SWEEP_ANGLE * (1f - normalRatio);

        // Draw Normal Arc
        if (normalSweep > 0)
        {
            canvas.drawArc(arcBounds, START_ANGLE, normalSweep, false, normalArcPaint);
        }

        // Draw Redline Arc
        if (redlineSweep > 0)
        {
            canvas.drawArc(arcBounds, START_ANGLE + normalSweep, redlineSweep, false, redlineArcPaint);
        }

        // Draw Scale Ticks
        Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.parseColor("#B0BEC5"));
        tickPaint.setStrokeWidth(1.5f);

        int numTicks = 6;
        for (int i = 0; i < numTicks; i++)
        {
            float tickRatio = i / (float) (numTicks - 1);
            double angleRad = Math.toRadians(START_ANGLE + tickRatio * SWEEP_ANGLE);

            float innerR = radius - 11f;
            float outerR = radius - 5f;

            float x1 = (float) (cx + innerR * Math.cos(angleRad));
            float y1 = (float) (cy + innerR * Math.sin(angleRad));
            float x2 = (float) (cx + outerR * Math.cos(angleRad));
            float y2 = (float) (cy + outerR * Math.sin(angleRad));

            canvas.drawLine(x1, y1, x2, y2, tickPaint);
        }

        // Draw Title (Top/Center - with dynamic text auto-scaling to prevent collision with arc)
        if (title != null)
        {
            textPaint.setTextSize(10f);
            textPaint.setColor(Color.parseColor("#B0BEC5"));
            textPaint.setFakeBoldText(true);

            float maxTitleWidth = radius * 1.3f;
            float currentWidth = textPaint.measureText(title);
            if (currentWidth > maxTitleWidth)
            {
                textPaint.setTextSize(Math.max(6f, 10f * (maxTitleWidth / currentWidth)));
            }

            canvas.drawText(title.toUpperCase(Locale.ROOT), cx, cy - radius * 0.40f, textPaint);
        }

        // Calculate Needle Angle
        float currentVal = value != null ? value : minVal;
        float valRatio = (currentVal - minVal) / (effectiveMax - minVal);
        valRatio = Math.max(0f, Math.min(1f, valRatio));
        float needleAngle = START_ANGLE + valRatio * SWEEP_ANGLE;
        double needleRad = Math.toRadians(needleAngle);

        // Draw Needle Pointer
        float needleLength = radius - 12f;
        float tipX = (float) (cx + needleLength * Math.cos(needleRad));
        float tipY = (float) (cy + needleLength * Math.sin(needleRad));

        double perpRad = needleRad + Math.PI / 2;
        float baseWidth = 4f;
        float baseLeftX = (float) (cx + baseWidth * Math.cos(perpRad));
        float baseLeftY = (float) (cy + baseWidth * Math.sin(perpRad));
        float baseRightX = (float) (cx - baseWidth * Math.cos(perpRad));
        float baseRightY = (float) (cy - baseWidth * Math.sin(perpRad));

        Path needlePath = new Path();
        needlePath.moveTo(baseLeftX, baseLeftY);
        needlePath.lineTo(tipX, tipY);
        needlePath.lineTo(baseRightX, baseRightY);
        needlePath.close();

        canvas.drawPath(needlePath, needlePaint);

        // Draw Center Hub
        canvas.drawCircle(cx, cy, 6f, hubPaint);

        // Draw Value / Disconnected Text (Center Bottom)
        if (!isConnected)
        {
            textPaint.setTextSize(11f);
            textPaint.setColor(Color.parseColor("#FF5252"));
            textPaint.setFakeBoldText(true);
            canvas.drawText("Disconnected", cx, cy + radius * 0.45f, textPaint);
        }
        else if (value != null)
        {
            textPaint.setTextSize(18f);
            textPaint.setColor(Color.WHITE);
            textPaint.setFakeBoldText(true);

            String formattedVal;
            if (value.floatValue() == (float) Math.round(value.floatValue()))
            {
                formattedVal = String.valueOf(value.intValue());
            }
            else
            {
                formattedVal = String.format(Locale.getDefault(), "%.1f", value);
            }

            canvas.drawText(formattedVal, cx, cy + radius * 0.40f, textPaint);

            // Unit text
            if (unit != null && !unit.isEmpty())
            {
                textPaint.setTextSize(9f);
                textPaint.setColor(Color.parseColor("#80CBC4"));
                textPaint.setFakeBoldText(false);
                canvas.drawText(unit, cx, cy + radius * 0.65f, textPaint);
            }
        }
        else
        {
            textPaint.setTextSize(16f);
            textPaint.setColor(Color.parseColor("#78909C"));
            canvas.drawText("--", cx, cy + radius * 0.45f, textPaint);
        }

        return bitmap;
    }
}
