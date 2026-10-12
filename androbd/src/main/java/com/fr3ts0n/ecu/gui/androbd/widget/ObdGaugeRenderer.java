package com.fr3ts0n.ecu.gui.androbd.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.Locale;

/**
 * Custom Canvas renderer for drawing analog gauge views matching Card 1 in the mockup.
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

        float cx = BITMAP_SIZE / 2f;
        float cy = BITMAP_SIZE / 2f + 10f; // Shift dial slightly down to accommodate top header
        float radius = BITMAP_SIZE * 0.33f;
        RectF arcBounds = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        // Paints
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        // --- Top Header: Gauge Icon + Title ---
        if (title != null && !title.isEmpty())
        {
            float iconX = 14f;
            float iconY = 16f;

            // Draw Gauge Icon (Speedometer)
            Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            iconPaint.setStyle(Paint.Style.STROKE);
            iconPaint.setStrokeWidth(1.5f);
            iconPaint.setColor(Color.parseColor("#E2E8F0"));

            RectF iconBounds = new RectF(iconX, iconY - 5f, iconX + 10f, iconY + 5f);
            canvas.drawArc(iconBounds, 140f, 260f, false, iconPaint);

            Paint needleIconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            needleIconPaint.setColor(Color.parseColor("#E2E8F0"));
            needleIconPaint.setStrokeWidth(1.5f);
            canvas.drawLine(iconX + 5f, iconY, iconX + 8f, iconY - 3f, needleIconPaint);

            // Draw Uppercase Title
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

        // --- Dial Body ---
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#12161E"));
        canvas.drawCircle(cx, cy, radius + 8f, bgPaint);

        Paint bezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bezelPaint.setStyle(Paint.Style.STROKE);
        bezelPaint.setStrokeWidth(2f);
        bezelPaint.setColor(Color.parseColor("#2D3544"));
        canvas.drawCircle(cx, cy, radius + 6f, bezelPaint);

        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(2.5f);
        trackPaint.setColor(Color.parseColor("#2D3544"));

        Paint redlineArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redlineArcPaint.setStyle(Paint.Style.STROKE);
        redlineArcPaint.setStrokeWidth(4.5f);
        redlineArcPaint.setColor(Color.parseColor("#E53935"));

        // Draw Base Track
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, trackPaint);

        // Calculate Redline Portion
        float effectiveMax = maxVal > minVal ? maxVal : minVal + 100f;
        float effectiveRedline = Math.max(minVal, Math.min(effectiveMax, redlineVal));

        float normalRatio = (effectiveRedline - minVal) / (effectiveMax - minVal);
        float normalSweep = SWEEP_ANGLE * normalRatio;
        float redlineSweep = SWEEP_ANGLE * (1f - normalRatio);

        if (redlineSweep > 0 && redlineSweep < SWEEP_ANGLE)
        {
            canvas.drawArc(arcBounds, START_ANGLE + normalSweep, redlineSweep, false, redlineArcPaint);
        }

        // Draw Scale Ticks & Numbers
        Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.parseColor("#E2E8F0"));
        tickPaint.setStrokeWidth(1.2f);

        Paint scaleNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scaleNumPaint.setTextAlign(Paint.Align.CENTER);
        scaleNumPaint.setColor(Color.parseColor("#CBD5E1"));
        scaleNumPaint.setTextSize(8.5f);

        boolean isRpm = maxVal > 1000f;
        int numTicks = isRpm ? 8 : 5;

        for (int i = 0; i < numTicks; i++)
        {
            float tickRatio = i / (float) (numTicks - 1);
            double angleRad = Math.toRadians(START_ANGLE + tickRatio * SWEEP_ANGLE);

            float innerR = radius - 6f;
            float outerR = radius + 2f;

            float x1 = (float) (cx + innerR * Math.cos(angleRad));
            float y1 = (float) (cy + innerR * Math.sin(angleRad));
            float x2 = (float) (cx + outerR * Math.cos(angleRad));
            float y2 = (float) (cy + outerR * Math.sin(angleRad));

            canvas.drawLine(x1, y1, x2, y2, tickPaint);

            // Scale Numbers inside ticks
            float numVal = minVal + tickRatio * (effectiveMax - minVal);
            String numText = isRpm ? String.valueOf(Math.round(numVal / 1000f)) : String.valueOf(Math.round(numVal));

            float textR = radius - 16f;
            float tx = (float) (cx + textR * Math.cos(angleRad));
            float ty = (float) (cy + textR * Math.sin(angleRad)) + 3f;

            canvas.drawText(numText, tx, ty, scaleNumPaint);
        }

        // Calculate Needle Angle
        float currentVal = value != null ? value : minVal;
        float valRatio = (currentVal - minVal) / (effectiveMax - minVal);
        valRatio = Math.max(0f, Math.min(1f, valRatio));
        float needleAngle = START_ANGLE + valRatio * SWEEP_ANGLE;
        double needleRad = Math.toRadians(needleAngle);

        // Draw Tapered Red Needle Pointer
        Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setColor(isConnected ? Color.parseColor("#E53935") : Color.parseColor("#64748B"));

        float needleLength = radius - 8f;
        float tipX = (float) (cx + needleLength * Math.cos(needleRad));
        float tipY = (float) (cy + needleLength * Math.sin(needleRad));

        double perpRad = needleRad + Math.PI / 2;
        float baseWidth = 3f;
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
        Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setColor(Color.parseColor("#1A1F29"));
        canvas.drawCircle(cx, cy, 6.5f, hubPaint);

        Paint hubRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubRingPaint.setStyle(Paint.Style.STROKE);
        hubRingPaint.setStrokeWidth(1.5f);
        hubRingPaint.setColor(Color.parseColor("#E53935"));
        canvas.drawCircle(cx, cy, 6.5f, hubRingPaint);

        // Draw Center Readout (Large bold white number + unit below)
        textPaint.setTextAlign(Paint.Align.CENTER);
        if (!isConnected)
        {
            textPaint.setTextSize(9.5f);
            textPaint.setColor(Color.parseColor("#E53935"));
            textPaint.setFakeBoldText(true);
            canvas.drawText("OFFLINE", cx, cy + radius * 0.45f, textPaint);
        }
        else if (value != null)
        {
            textPaint.setTextSize(19f);
            textPaint.setColor(Color.WHITE);
            textPaint.setFakeBoldText(true);

            String formattedVal;
            if (isRpm)
            {
                formattedVal = String.format(Locale.getDefault(), "%.1f", value / 1000f);
            }
            else if (value.floatValue() == (float) Math.round(value.floatValue()))
            {
                formattedVal = String.valueOf(value.intValue());
            }
            else
            {
                formattedVal = String.format(Locale.getDefault(), "%.1f", value);
            }

            canvas.drawText(formattedVal, cx, cy + radius * 0.38f, textPaint);

            // Unit text below value
            String displayUnit = isRpm ? "x1000" : unit;
            if (displayUnit != null && !displayUnit.isEmpty())
            {
                textPaint.setTextSize(8.5f);
                textPaint.setColor(Color.parseColor("#CBD5E1"));
                textPaint.setFakeBoldText(false);
                canvas.drawText(displayUnit, cx, cy + radius * 0.65f, textPaint);
            }
        }
        else
        {
            textPaint.setTextSize(15f);
            textPaint.setColor(Color.parseColor("#64748B"));
            canvas.drawText("--", cx, cy + radius * 0.45f, textPaint);
        }

        return bitmap;
    }
}
