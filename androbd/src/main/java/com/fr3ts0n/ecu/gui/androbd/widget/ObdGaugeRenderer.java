package com.fr3ts0n.ecu.gui.androbd.widget;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.Locale;

/**
 * Custom Canvas renderer for drawing analog gauge views matching modern car dashboard instruments.
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
        float cy = BITMAP_SIZE / 2f - 6f; // Slightly shift dial up to leave space for title at bottom
        float radius = BITMAP_SIZE * 0.36f;
        RectF arcBounds = new RectF(cx - radius, cy - radius, cx + radius, cy + radius);

        // Paints
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.parseColor("#12161E"));
        canvas.drawCircle(cx, cy, radius + 10f, bgPaint);

        Paint bezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bezelPaint.setStyle(Paint.Style.STROKE);
        bezelPaint.setStrokeWidth(2f);
        bezelPaint.setColor(Color.parseColor("#2A313E"));
        canvas.drawCircle(cx, cy, radius + 8f, bezelPaint);

        Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(3f);
        trackPaint.setColor(Color.parseColor("#2A313E"));

        Paint redlineArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redlineArcPaint.setStyle(Paint.Style.STROKE);
        redlineArcPaint.setStrokeWidth(5f);
        redlineArcPaint.setColor(Color.parseColor("#E53935"));

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextAlign(Paint.Align.CENTER);

        Paint scaleNumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scaleNumPaint.setTextAlign(Paint.Align.CENTER);
        scaleNumPaint.setColor(Color.parseColor("#CBD5E1"));
        scaleNumPaint.setTextSize(9f);

        Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.FILL);
        needlePaint.setColor(isConnected ? Color.parseColor("#E53935") : Color.parseColor("#64748B"));

        // Draw Base Track
        canvas.drawArc(arcBounds, START_ANGLE, SWEEP_ANGLE, false, trackPaint);

        // Calculate Redline Portion
        float effectiveMax = maxVal > minVal ? maxVal : minVal + 100f;
        float effectiveRedline = Math.max(minVal, Math.min(effectiveMax, redlineVal));

        float normalRatio = (effectiveRedline - minVal) / (effectiveMax - minVal);
        float normalSweep = SWEEP_ANGLE * normalRatio;
        float redlineSweep = SWEEP_ANGLE * (1f - normalRatio);

        // Draw Redline Arc if applicable
        if (redlineSweep > 0 && redlineSweep < SWEEP_ANGLE)
        {
            canvas.drawArc(arcBounds, START_ANGLE + normalSweep, redlineSweep, false, redlineArcPaint);
        }

        // Draw Scale Ticks & Numbers
        Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(Color.parseColor("#E2E8F0"));
        tickPaint.setStrokeWidth(1.5f);

        boolean isRpm = maxVal > 1000f;
        int numTicks = isRpm ? 8 : 5;

        for (int i = 0; i < numTicks; i++)
        {
            float tickRatio = i / (float) (numTicks - 1);
            double angleRad = Math.toRadians(START_ANGLE + tickRatio * SWEEP_ANGLE);

            float innerR = radius - 7f;
            float outerR = radius + 2f;

            float x1 = (float) (cx + innerR * Math.cos(angleRad));
            float y1 = (float) (cy + innerR * Math.sin(angleRad));
            float x2 = (float) (cx + outerR * Math.cos(angleRad));
            float y2 = (float) (cy + outerR * Math.sin(angleRad));

            canvas.drawLine(x1, y1, x2, y2, tickPaint);

            // Scale Numbers inside ticks
            float numVal = minVal + tickRatio * (effectiveMax - minVal);
            String numText;
            if (isRpm)
            {
                numText = String.valueOf(Math.round(numVal / 1000f));
            }
            else
            {
                numText = String.valueOf(Math.round(numVal));
            }

            float textR = radius - 18f;
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
        float needleLength = radius - 10f;
        float tipX = (float) (cx + needleLength * Math.cos(needleRad));
        float tipY = (float) (cy + needleLength * Math.sin(needleRad));

        double perpRad = needleRad + Math.PI / 2;
        float baseWidth = 3.5f;
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

        // Draw Center Hub (Dark Cap + Red Accent Ring)
        Paint hubPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubPaint.setColor(Color.parseColor("#1E232D"));
        canvas.drawCircle(cx, cy, 7f, hubPaint);

        Paint hubRingPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        hubRingPaint.setStyle(Paint.Style.STROKE);
        hubRingPaint.setStrokeWidth(2f);
        hubRingPaint.setColor(Color.parseColor("#E53935"));
        canvas.drawCircle(cx, cy, 7f, hubRingPaint);

        // Draw Value / Disconnected Text (Center Bottom)
        if (!isConnected)
        {
            textPaint.setTextSize(10f);
            textPaint.setColor(Color.parseColor("#E53935"));
            textPaint.setFakeBoldText(true);
            canvas.drawText("Disconnected", cx, cy + radius * 0.45f, textPaint);
        }
        else if (value != null)
        {
            textPaint.setTextSize(20f);
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

            canvas.drawText(formattedVal, cx, cy + radius * 0.40f, textPaint);

            // Unit text below value
            String displayUnit = isRpm ? "x1000" : unit;
            if (displayUnit != null && !displayUnit.isEmpty())
            {
                textPaint.setTextSize(9f);
                textPaint.setColor(Color.parseColor("#94A3B8"));
                textPaint.setFakeBoldText(false);
                canvas.drawText(displayUnit, cx, cy + radius * 0.65f, textPaint);
            }
        }
        else
        {
            textPaint.setTextSize(16f);
            textPaint.setColor(Color.parseColor("#64748B"));
            canvas.drawText("--", cx, cy + radius * 0.45f, textPaint);
        }

        // Draw Title Below Dial Card
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

            canvas.drawText(title, cx, BITMAP_SIZE - 4f, textPaint);
        }

        return bitmap;
    }
}
