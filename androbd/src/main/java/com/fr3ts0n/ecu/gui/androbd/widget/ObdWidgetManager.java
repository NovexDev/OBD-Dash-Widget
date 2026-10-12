package com.fr3ts0n.ecu.gui.androbd.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.RemoteViews;

import com.fr3ts0n.ecu.EcuDataItem;
import com.fr3ts0n.ecu.EcuDataItems;
import com.fr3ts0n.ecu.EcuDataPv;
import com.fr3ts0n.ecu.gui.androbd.R;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages rendering and per-instance update frequency scheduling for OBD widgets (Digital, Gauge, and Chart modes).
 */
public class ObdWidgetManager
{
    private static final Map<Integer, Long> lastRenderMap = new ConcurrentHashMap<>();

    /**
     * Update a single widget instance by ID.
     */
    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId)
    {
        try
        {
            String mnemonic = ObdWidgetPrefs.getWidgetPid(context, appWidgetId);
            String type = ObdWidgetPrefs.getWidgetType(context, appWidgetId);
            boolean isConnected = ObdForegroundService.isConnected();

            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.obd_widget);

            EcuDataItem item = null;
            if (EcuDataItems.byMnemonic.containsKey(mnemonic))
            {
                item = EcuDataItems.byMnemonic.get(mnemonic);
            }

            String defaultLabel = item != null && item.label != null && !item.label.isEmpty() ? item.label : mnemonic;
            String title = com.fr3ts0n.ecu.prot.obd.Messages.getString(mnemonic, defaultLabel);
            String units = item != null && item.pv != null ? item.pv.getUnits() : "";
            Object rawVal = item != null && item.pv != null ? item.pv.get(EcuDataPv.FID_VALUE) : null;

            Float numValue = null;
            if (rawVal instanceof Number)
            {
                numValue = ((Number) rawVal).floatValue();
                if (isConnected)
                {
                    ObdHistoryTracker.addPoint(mnemonic, numValue, System.currentTimeMillis());
                }
            }

            if (ObdWidgetPrefs.TYPE_GAUGE.equals(type))
            {
                // --- ANALOG GAUGE MODE ---
                views.setViewVisibility(R.id.widget_digital_container, View.GONE);
                views.setViewVisibility(R.id.widget_gauge_image, View.VISIBLE);

                float minVal = ObdWidgetPrefs.getWidgetMin(context, appWidgetId);
                float maxVal = ObdWidgetPrefs.getWidgetMax(context, appWidgetId);
                float redlineVal = ObdWidgetPrefs.getWidgetRedline(context, appWidgetId);

                Bitmap gaugeBitmap = ObdGaugeRenderer.renderGauge(
                        title,
                        numValue,
                        units,
                        minVal,
                        maxVal,
                        redlineVal,
                        isConnected
                );
                views.setImageViewBitmap(R.id.widget_gauge_image, gaugeBitmap);
            }
            else if (ObdWidgetPrefs.TYPE_CHART.equals(type))
            {
                // --- TREND CHART MODE ---
                views.setViewVisibility(R.id.widget_digital_container, View.GONE);
                views.setViewVisibility(R.id.widget_gauge_image, View.VISIBLE);

                int timeWindowSeconds = ObdWidgetPrefs.getWidgetTimeWindow(context, appWidgetId);
                List<ObdHistoryTracker.HistoryPoint> points = ObdHistoryTracker.getPoints(mnemonic, timeWindowSeconds * 1000L);

                Bitmap chartBitmap = ObdChartRenderer.renderChart(
                        title,
                        numValue,
                        units,
                        points,
                        timeWindowSeconds,
                        isConnected
                );
                views.setImageViewBitmap(R.id.widget_gauge_image, chartBitmap);
            }
            else
            {
                // --- DIGITAL MODE ---
                views.setViewVisibility(R.id.widget_digital_container, View.VISIBLE);
                views.setViewVisibility(R.id.widget_gauge_image, View.GONE);

                // Choose icon based on PID mnemonic
                int iconRes = R.drawable.ic_widget_temp;
                String lowerMnemonic = mnemonic != null ? mnemonic.toLowerCase(Locale.ROOT) : "";
                if (lowerMnemonic.contains("speed") || lowerMnemonic.contains("rpm"))
                {
                    iconRes = R.drawable.ic_widget_gauge;
                }
                else if (lowerMnemonic.contains("load") || lowerMnemonic.contains("trim"))
                {
                    iconRes = R.drawable.ic_widget_trend;
                }
                views.setImageViewResource(R.id.widget_digital_icon, iconRes);

                views.setTextViewText(R.id.widget_title, title != null ? title.toUpperCase(Locale.ROOT) : "");
                views.setTextViewText(R.id.widget_unit, units);

                if (!isConnected)
                {
                    views.setTextViewText(R.id.widget_status, "");
                    views.setTextViewText(R.id.widget_value, "--");
                }
                else
                {
                    views.setTextViewText(R.id.widget_status, "");
                    if (numValue != null)
                    {
                        String formattedValue;
                        if (numValue.floatValue() == (float) Math.round(numValue.floatValue()))
                        {
                            formattedValue = String.valueOf(numValue.intValue());
                        }
                        else
                        {
                            formattedValue = String.format(Locale.getDefault(), "%.1f", numValue);
                        }
                        views.setTextViewText(R.id.widget_value, formattedValue);
                    }
                    else
                    {
                        views.setTextViewText(R.id.widget_value, "--");
                    }
                }
            }

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
        catch (Exception e)
        {
            // Catch any IPC or rendering exceptions safely
        }
    }

    /**
     * Update widget instances that are due based on their individual configured update frequency.
     */
    public static void updateDueWidgets(Context context)
    {
        try
        {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, ObdWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            long nowMs = System.currentTimeMillis();

            for (int appWidgetId : appWidgetIds)
            {
                int intervalMs = ObdWidgetPrefs.getWidgetUpdateInterval(context, appWidgetId);
                Long lastRenderTime = lastRenderMap.get(appWidgetId);

                if (lastRenderTime == null || (nowMs - lastRenderTime) >= intervalMs)
                {
                    lastRenderMap.put(appWidgetId, nowMs);
                    updateWidget(context, appWidgetManager, appWidgetId);
                }
            }
        }
        catch (Exception e)
        {
            // Catch any IPC exceptions safely
        }
    }

    /**
     * Update all active OBD widget instances immediately.
     */
    public static void updateAllWidgets(Context context)
    {
        try
        {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentName = new ComponentName(context, ObdWidgetProvider.class);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            for (int appWidgetId : appWidgetIds)
            {
                lastRenderMap.put(appWidgetId, System.currentTimeMillis());
                updateWidget(context, appWidgetManager, appWidgetId);
            }
        }
        catch (Exception e)
        {
            // Catch any IPC exceptions safely
        }
    }
}
