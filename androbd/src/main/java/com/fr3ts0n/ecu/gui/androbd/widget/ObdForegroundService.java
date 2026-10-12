package com.fr3ts0n.ecu.gui.androbd.widget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

import com.fr3ts0n.ecu.gui.androbd.CommService;
import com.fr3ts0n.ecu.gui.androbd.MainActivity;
import com.fr3ts0n.ecu.gui.androbd.R;
import com.fr3ts0n.ecu.prot.obd.ElmProt;

/**
 * Foreground Service to maintain Bluetooth/OBD communication and perform live widget updates
 * when the application is in the background or minimized.
 */
public class ObdForegroundService extends Service
{
    private static final String CHANNEL_ID = "obd_widget_live_service";
    private static final int NOTIFICATION_ID = 1001;

    public static final String ACTION_START_SERVICE = "com.novexdev.obddashwidget.ACTION_START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "com.novexdev.obddashwidget.ACTION_STOP_SERVICE";
    public static final String ACTION_UPDATE_STATUS = "com.novexdev.obddashwidget.ACTION_UPDATE_STATUS";
    public static final String ACTION_CONNECT = "com.novexdev.obddashwidget.ACTION_CONNECT";
    public static final String ACTION_DISCONNECT = "com.novexdev.obddashwidget.ACTION_DISCONNECT";

    public static final String EXTRA_IS_CONNECTED = "extra_is_connected";
    public static final String EXTRA_STATUS_DETAILS = "extra_status_details";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private boolean isRunning = false;

    private static boolean currentIsConnected = false;
    private static String currentStatusDetails = "";

    public static boolean isConnected()
    {
        return currentIsConnected;
    }

    public static void startService(Context context)
    {
        Intent intent = new Intent(context, ObdForegroundService.class);
        intent.setAction(ACTION_START_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            context.startForegroundService(intent);
        }
        else
        {
            context.startService(intent);
        }
    }

    public static void stopService(Context context)
    {
        Intent intent = new Intent(context, ObdForegroundService.class);
        intent.setAction(ACTION_STOP_SERVICE);
        context.startService(intent);
    }

    public static void updateStatus(Context context, boolean connected, String details)
    {
        currentIsConnected = connected;
        currentStatusDetails = details != null ? details : "";

        Intent intent = new Intent(context, ObdForegroundService.class);
        intent.setAction(ACTION_UPDATE_STATUS);
        intent.putExtra(EXTRA_IS_CONNECTED, connected);
        intent.putExtra(EXTRA_STATUS_DETAILS, currentStatusDetails);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            context.startForegroundService(intent);
        }
        else
        {
            context.startService(intent);
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null)
        {
            String action = intent.getAction();

            if (ACTION_STOP_SERVICE.equals(action))
            {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                {
                    stopForeground(STOP_FOREGROUND_REMOVE);
                }
                else
                {
                    stopForeground(true);
                }
                stopSelf();
                isRunning = false;
                return START_NOT_STICKY;
            }
            else if (ACTION_UPDATE_STATUS.equals(action))
            {
                currentIsConnected = intent.getBooleanExtra(EXTRA_IS_CONNECTED, currentIsConnected);
                currentStatusDetails = intent.getStringExtra(EXTRA_STATUS_DETAILS);
                if (currentStatusDetails == null)
                {
                    currentStatusDetails = "";
                }
            }
            else if (ACTION_DISCONNECT.equals(action))
            {
                // Disconnect action clicked from notification
                ElmProt.runDemo = false;
                CommService.elm.goToSleep();
                currentIsConnected = false;
                currentStatusDetails = getString(R.string.notification_disconnected);

                // Notify MainActivity if open
                Intent broadcast = new Intent("com.novexdev.obddashwidget.ACTION_REQUEST_DISCONNECT");
                sendBroadcast(broadcast);

                ObdWidgetManager.updateAllWidgets(this);
            }
        }

        // Always call startForegroundNotification() on every onStartCommand call to satisfy Android O+ requirement
        startForegroundNotification();
        startWidgetUpdateLoop();

        return START_STICKY;
    }

    private Notification buildNotification()
    {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        String statusText;
        if (currentIsConnected)
        {
            String details = currentStatusDetails != null && !currentStatusDetails.isEmpty()
                    ? currentStatusDetails
                    : getString(R.string.demo);
            statusText = getString(R.string.notification_connected, details);
        }
        else
        {
            statusText = currentStatusDetails != null && !currentStatusDetails.isEmpty()
                    ? currentStatusDetails
                    : getString(R.string.notification_disconnected);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(statusText)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        // Add Connect or Disconnect Action Button
        if (currentIsConnected)
        {
            Intent disconnectIntent = new Intent(this, ObdForegroundService.class);
            disconnectIntent.setAction(ACTION_DISCONNECT);
            PendingIntent disconnectPendingIntent = PendingIntent.getService(
                    this, 101, disconnectIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            builder.addAction(
                    R.drawable.ic_delete,
                    getString(R.string.notification_action_disconnect),
                    disconnectPendingIntent
            );
        }
        else
        {
            Intent connectIntent = new Intent(this, MainActivity.class);
            connectIntent.setAction("com.novexdev.obddashwidget.ACTION_REQUEST_CONNECT");
            connectIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent connectPendingIntent = PendingIntent.getActivity(
                    this, 102, connectIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
            );
            builder.addAction(
                    R.drawable.ic_build,
                    getString(R.string.notification_action_connect),
                    connectPendingIntent
            );
        }

        return builder.build();
    }

    private void startForegroundNotification()
    {
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
        }
        else
        {
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    private void updateForegroundNotification()
    {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null)
        {
            manager.notify(NOTIFICATION_ID, buildNotification());
        }
    }

    private void createNotificationChannel()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "OBD Widget Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for live OBD widget updates");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null)
            {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void startWidgetUpdateLoop()
    {
        if (isRunning) return;
        isRunning = true;

        updateRunnable = new Runnable()
        {
            @Override
            public void run()
            {
                if (!isRunning) return;

                // Update widget instances whose individual frequency is due
                ObdWidgetManager.updateDueWidgets(ObdForegroundService.this);

                // Tick base scheduling loop every 50 ms (20 Hz resolution)
                handler.postDelayed(this, 50);
            }
        };

        handler.post(updateRunnable);
    }

    private long getUpdateIntervalMs()
    {
        try
        {
            android.content.SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
            String val = prefs.getString("widget_update_frequency", "200");
            return Long.parseLong(val);
        }
        catch (Exception e)
        {
            return 200L;
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        isRunning = false;
        if (updateRunnable != null)
        {
            handler.removeCallbacks(updateRunnable);
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }
}
