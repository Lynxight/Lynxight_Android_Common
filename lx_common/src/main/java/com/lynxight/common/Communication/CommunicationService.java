package com.lynxight.common.Communication;

import static androidx.core.app.NotificationCompat.PRIORITY_MAX;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.bugsnag.android.Bugsnag;
import com.lynxight.common.Reports.PeriodicReport;
import com.lynxight.common.managers.SiteData;
import com.lynxight.common.managers.SystemResourceManager;
import com.lynxight.common.utils.ConfigPreferences;
import com.lynxight.common.utils.Constants;
import com.lynxight.common.utils.Utils;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.rxjava3.disposables.Disposable;


public class CommunicationService extends Service {
    private final static String TAG = CommunicationService.class.getSimpleName();

    @Inject
    public MQTTClient mqttClient;
    @Inject
    SystemResourceManager systemResourceManager;
    @Inject
    SiteData siteData;


    public static final int WIFI_CONNECTIVITY_STATUS_INTERVAL_SECONDS = 6;
    public static final int WATCH_STATUS_TIMER_INTERVAL = 60 * Constants.TIMER_TICK_INTERVAL;

    public static final int CLOCK_UPDATE_TIMER_INTERVAL = 10 * Constants.TIMER_TICK_INTERVAL;

    private WifiManager wifiManager;

    protected ScheduledFuture<?> wifiReconnectionFuture;
    protected ScheduledFuture<?> wifiConnectivityStatusFuture;
    protected Disposable wifiStatusDisposable;
    protected Disposable batteryLevelDisposable;
    protected Disposable isChargingDisposable;
    protected int batteryLevel = 0;
    protected int wifiRssi = 0;
    protected int watchIpAddress;
    protected boolean bTEnabled;
    protected boolean isCharging;


    protected final ArrayList<ScheduledExecutorService> executors = new ArrayList<>();

    private final CountDownTimer clockUpdateTimer =
            new CountDownTimer(CLOCK_UPDATE_TIMER_INTERVAL , Constants.TIMER_TICK_INTERVAL) {
                public void onTick(long millisUntilFinished) {}

                public void onFinish() {
                    Log.d("TIMETITLE",CommunicationService.class.getSimpleName()+" trigger clock update");
                    String clockValue = Utils.getCurrentDateTimeString(getResources(),Constants.TIME_FORMAT);
                    systemResourceManager.publishClockTime(clockValue);
                    clockUpdateTimer.start();
                }
            };
    private final CountDownTimer periodicReportToBugSnug =
            new CountDownTimer(Constants.BUGSNUG_TIMER_INTERVAL , Constants.TIMER_TICK_INTERVAL) {
                public void onTick(long millisUntilFinished) {}

                public void onFinish() {
                    if(ConfigPreferences.isBugSnagInUse()) {
                        Bugsnag.notify(new PeriodicReport("Periodic Status Message (Demo = " + ConfigPreferences.isDemoMode() + ")"));
                        periodicReportToBugSnug.start();
                    }
                }
            };

    private class ReconnectToWifi implements Runnable {
        private void enableWifi() {
            if (!wifiManager.isWifiEnabled()) {
                Log.i(TAG, "enabling wifi adapter");
                boolean success = wifiManager.setWifiEnabled(true);
                if (!success) {
                    Log.w(TAG, "failed to enable wifi adapter");
                }
            }
        }

        private void connectWifi() {
            if (!wifiManager.isWifiEnabled()) {
                Log.e(TAG, "connection to wifi failed - adapter is off");
                return;
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null &&
                    !wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED))
            {
                if (!wifiManager.reconnect()) {
                    Log.e(TAG, "reconnection to wifi failed");
                } else {
                    Log.i(TAG, "reconnected to wifi");
                }
            }
        }

        @Override
        public void run() {
            enableWifi();
            connectWifi();
        }
    }

    private class ConnectivityStatusRunnable implements Runnable {
        @Override
        public void run() {
            systemResourceManager.setWifiInfo(wifiManager.getConnectionInfo());

            if (ConfigPreferences.isConfigured() && !mqttClient.getIsInit() && (systemResourceManager.isWifiConnected() || ConfigPreferences.isDemoMode())) {
                mqttClient.init();
            }
        }
    }


    public CommunicationService() {}

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this, createNotificationChannel());
        Notification notification = builder.setOngoing(true)
                .setPriority(PRIORITY_MAX)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(101, notification);
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        registerBroadcastReceivers();
    }

    @Override
    public void onDestroy() {
        if (wifiReconnectionFuture != null)
            wifiReconnectionFuture.cancel(true);
        if (wifiConnectivityStatusFuture != null)
            wifiConnectivityStatusFuture.cancel(true);
        mqttClient.stop();
        siteData.stop();
        disposeSubscriptions();
        unregisterBroadcastReceivers();

        clockUpdateTimer.cancel();
        periodicReportToBugSnug.cancel();

        for (ScheduledExecutorService tempExecutor : executors)
            tempExecutor.shutdownNow();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        WifiManager.WifiLock wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL,
                Constants.WIFI_LOCK_TAG);

        wifiLock.acquire();

        wifiStatusDisposable = systemResourceManager.onWifiStatus()
                .subscribe(this::setWifiInfo);
        batteryLevelDisposable = systemResourceManager.onBatteryLevel()
                .subscribe(this::setBatteryLevel);
        isChargingDisposable = systemResourceManager.onDeviceIsCharging()
                        .subscribe(this::setCharging);
        startWifiReconnectionThread();
        startWifiConnectivityStatusThread();

        String clockValue = Utils.getCurrentDateTimeString(getResources(),Constants.TIME_FORMAT);
        systemResourceManager.publishClockTime(clockValue);
        clockUpdateTimer.start();
        periodicReportToBugSnug.start();

        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new CommunicationServiceBinder();
    }

    public class CommunicationServiceBinder extends Binder {
        public CommunicationService getInstance() {
            return CommunicationService.this;
        }
    }

    private String createNotificationChannel() {
        NotificationChannel notificationChannel = new NotificationChannel("commService",
                "Communication Service", NotificationManager.IMPORTANCE_NONE);
        notificationChannel.setLightColor(Color.BLUE);
        notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(notificationChannel);
        return "commService";
    }

    protected void registerBroadcastReceivers() {
        Log.i(TAG, "registerBroadcastReceivers");
    }

    protected void unregisterBroadcastReceivers() {

    }

    protected void disposeSubscriptions() {
        if(wifiStatusDisposable != null) wifiStatusDisposable.dispose() ;
        if(batteryLevelDisposable != null) batteryLevelDisposable.dispose();
        if(isChargingDisposable != null) isChargingDisposable.dispose();
    }


    /**
     * WIFI
     */
    private void startWifiReconnectionThread() {
        if (wifiReconnectionFuture != null && !wifiReconnectionFuture.isDone()) {
            return;
        }
        ScheduledExecutorService tempExecutor = Executors.newSingleThreadScheduledExecutor();
        executors.add(tempExecutor);
        wifiReconnectionFuture =
                tempExecutor.scheduleAtFixedRate(
                        new ReconnectToWifi(), 0,
                        Constants.WIFI_RECONNECTION_INTERVAL_SECONDS,
                        TimeUnit.SECONDS);
    }

    private void startWifiConnectivityStatusThread() {
        if (!ConfigPreferences.isDemoMode() && wifiConnectivityStatusFuture != null && !wifiConnectivityStatusFuture.isDone()) {
            return;
        }
        ScheduledExecutorService tempExecutor = Executors.newSingleThreadScheduledExecutor();
        executors.add(tempExecutor);
        wifiConnectivityStatusFuture =
                tempExecutor.scheduleAtFixedRate(
                        new ConnectivityStatusRunnable(), 0,
                        WIFI_CONNECTIVITY_STATUS_INTERVAL_SECONDS,
                        TimeUnit.SECONDS);
    }

    /**
     * WIFI
     */
    private void setWifiInfo(WifiInfo info) {
        synchronized (this) {
            wifiRssi = info.getRssi();
            watchIpAddress = info.getIpAddress();
        }
    }


    private void setCharging(boolean charging) {
        synchronized (this) {
            isCharging = charging;
        }
    }



    /**
     * BATTERY
     */
    private void setBatteryLevel(int batteryLevel) {
        synchronized (this) {
            this.batteryLevel = batteryLevel;
        }
    }

}