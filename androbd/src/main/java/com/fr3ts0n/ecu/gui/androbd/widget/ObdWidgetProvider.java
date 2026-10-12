package com.fr3ts0n.ecu.gui.androbd.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

/**
 * AppWidgetProvider for OBD gauge widgets.
 */
public class ObdWidgetProvider extends AppWidgetProvider
{
    public static final String ACTION_WIDGET_UPDATE = "com.novexdev.obddashwidget.ACTION_WIDGET_UPDATE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds)
    {
        for (int appWidgetId : appWidgetIds)
        {
            ObdWidgetManager.updateWidget(context, appWidgetManager, appWidgetId);
        }
        ObdForegroundService.startService(context);
    }

    @Override
    public void onEnabled(Context context)
    {
        super.onEnabled(context);
        ObdForegroundService.startService(context);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds)
    {
        for (int appWidgetId : appWidgetIds)
        {
            ObdWidgetPrefs.deleteWidgetConfig(context, appWidgetId);
        }
    }

    @Override
    public void onDisabled(Context context)
    {
        super.onDisabled(context);
        ObdForegroundService.stopService(context);
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        super.onReceive(context, intent);
        if (ACTION_WIDGET_UPDATE.equals(intent.getAction()))
        {
            ObdWidgetManager.updateAllWidgets(context);
        }
    }
}
