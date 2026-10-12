package com.fr3ts0n.ecu.gui.androbd.widget;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * SharedPreferences helper to manage AppWidget configurations
 * (e.g. appWidgetId -> PID Mnemonic, Widget Type, Min/Max/Redline limits, Chart Time Window).
 */
public class ObdWidgetPrefs
{
    private static final String PREFS_NAME = "com.novexdev.obddashwidget.ObdWidgetPrefs";

    private static final String KEY_PID = "appwidget_pid_";
    private static final String KEY_TYPE = "appwidget_type_";
    private static final String KEY_MIN = "appwidget_min_";
    private static final String KEY_MAX = "appwidget_max_";
    private static final String KEY_REDLINE = "appwidget_redline_";
    private static final String KEY_TIME_WINDOW = "appwidget_timewindow_";

    public static final String TYPE_DIGITAL = "DIGITAL";
    public static final String TYPE_GAUGE = "GAUGE";
    public static final String TYPE_CHART = "CHART";

    public static void saveWidgetConfig(Context context, int appWidgetId, String mnemonic, String type, float minVal, float maxVal, float redlineVal, int timeWindowSeconds)
    {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        prefs.putString(KEY_PID + appWidgetId, mnemonic);
        prefs.putString(KEY_TYPE + appWidgetId, type);
        prefs.putFloat(KEY_MIN + appWidgetId, minVal);
        prefs.putFloat(KEY_MAX + appWidgetId, maxVal);
        prefs.putFloat(KEY_REDLINE + appWidgetId, redlineVal);
        prefs.putInt(KEY_TIME_WINDOW + appWidgetId, timeWindowSeconds);
        prefs.apply();
    }

    public static String getWidgetPid(Context context, int appWidgetId)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PID + appWidgetId, "engine_speed");
    }

    public static String getWidgetType(Context context, int appWidgetId)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_TYPE + appWidgetId, TYPE_DIGITAL);
    }

    public static float getWidgetMin(Context context, int appWidgetId)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_MIN + appWidgetId, 0f);
    }

    public static float getWidgetMax(Context context, int appWidgetId)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_MAX + appWidgetId, 8000f);
    }

    public static float getWidgetRedline(Context context, int appWidgetId)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_REDLINE + appWidgetId, 6000f);
    }

    public static int getWidgetTimeWindow(Context context, int appWidgetId)
    {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_TIME_WINDOW + appWidgetId, 60);
    }

    public static void deleteWidgetConfig(Context context, int appWidgetId)
    {
        SharedPreferences.Editor prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        prefs.remove(KEY_PID + appWidgetId);
        prefs.remove(KEY_TYPE + appWidgetId);
        prefs.remove(KEY_MIN + appWidgetId);
        prefs.remove(KEY_MAX + appWidgetId);
        prefs.remove(KEY_REDLINE + appWidgetId);
        prefs.remove(KEY_TIME_WINDOW + appWidgetId);
        prefs.apply();
    }
}
