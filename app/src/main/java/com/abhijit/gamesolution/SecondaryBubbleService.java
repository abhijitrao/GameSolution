package com.abhijit.gamesolution;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.WindowManager;

public class SecondaryBubbleService extends Service {
    private static final String CHANNEL_ID = "game_solution_secondary_bubbles";
    private MultiBubbleManager manager;

    @Override public void onCreate() {
        super.onCreate();
        createChannel();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(11, buildNotification());
        }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return; }
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        manager = new MultiBubbleManager(this, wm);
        manager.startTracking(BubbleSettings.isRecentAppBubbleEnabled(this), BubbleSettings.isActivitySwitchBubbleEnabled(this));
    }

    private Notification buildNotification() {
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("GameSolution")
                .setContentText("Secondary bubbles are active")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Secondary bubbles", NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (manager != null) manager.startTracking(
                BubbleSettings.isRecentAppBubbleEnabled(this),
                BubbleSettings.isActivitySwitchBubbleEnabled(this));
        return START_STICKY;
    }

    @Override public void onDestroy() {
        if (manager != null) manager.removeAllSecondaryBubbles();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
