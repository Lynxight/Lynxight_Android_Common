package com.lynxight.common.managers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.os.BatteryManager;
import android.os.PowerManager;
import android.util.Log;

import com.bugsnag.android.Bugsnag;
import com.lynxight.common.Communication.CommunicationService;
import com.lynxight.common.utils.ConfigPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

@Singleton
public class SystemResourceManager {
    private final static String TAG = SystemResourceManager.class.getSimpleName();

    @Inject
    Context context;

    private boolean isWifiSettings = false;
    private WifiInfo wifiInfo;
    private PowerManager.WakeLock wakeLock;

    private final PublishSubject<WifiInfo> wifiStatusSubject = PublishSubject.create();
    private final PublishSubject<Boolean> btEnabledSubject = PublishSubject.create();
    private final PublishSubject<Integer> batteryLevelSubject = PublishSubject.create();
    private final PublishSubject<String>  clockTimeSubject = PublishSubject.create();

    private final PublishSubject<Boolean> deviceIsChargingSubject = PublishSubject.create();

    public Observable<WifiInfo> onWifiStatus() { return wifiStatusSubject; }
    public Observable<Boolean> onBTEnabled() { return btEnabledSubject; }
    public Observable<Integer> onBatteryLevel() { return batteryLevelSubject; }
    public Observable<String> onClockTime() { return clockTimeSubject; }
    public Observable<Boolean> onDeviceIsCharging() { return deviceIsChargingSubject; }

    public void publishWifiStatus(final WifiInfo info) { wifiStatusSubject.onNext(info); }
    private void publishBatteryLevel(final int level) { batteryLevelSubject.onNext(level); }
    public void publishClockTime(final String timeString) { clockTimeSubject.onNext(timeString); }


    public void publishDeviceIsCharging(boolean isCharging) {
        deviceIsChargingSubject.onNext(isCharging);
    }


    @Inject
    public SystemResourceManager() {}

    public void init() {
        context.startForegroundService(new Intent(context, CommunicationService.class));

        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        registerBroadcastReceivers();

        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK |
                PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);

        wakeLock.acquire();
    }


    private BroadcastReceiver powerInfoReceiver;

    protected void registerBroadcastReceivers() {
        try {
            powerInfoReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int batteryLevel =
                            intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);

                    int pluggedState =
                            intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                    boolean isChargerPlugged =
                            (pluggedState == BatteryManager.BATTERY_PLUGGED_AC ||
                                    pluggedState == BatteryManager.BATTERY_PLUGGED_USB ||
                                    pluggedState == BatteryManager.BATTERY_PLUGGED_WIRELESS);

                    Log.i(TAG, "battery level: " + batteryLevel + " | charger " +
                            (isChargerPlugged ? " plugged in" : " unplugged"));

                    setBatteryLevel(batteryLevel);
                    setDeviceIsCharging(isChargerPlugged);
                }
            };
            this.context.registerReceiver(powerInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Exception e) {
            Log.e(TAG, "Broadcast receivers exception: ", e);
            SystemResourceManager.reportException(e);
        }
    }


    public void stop() {
        context.unregisterReceiver(powerInfoReceiver);
    }

    public void setBatteryLevel(int level) { publishBatteryLevel(level); }

    public void setDeviceIsCharging(boolean isCharging) {
        publishDeviceIsCharging(isCharging);
    }

    public static void reportException(Exception e) {
        if(!ConfigPreferences.isBugSnagInUse())
            return;
        Bugsnag.notify(e);
    }




    public synchronized void setInSettings(boolean isOn) { isWifiSettings = isOn; }

    public synchronized boolean getIsInSettings() { return isWifiSettings; }

    public synchronized void setWifiInfo(WifiInfo wifiInfo) {
        this.wifiInfo = wifiInfo;
        publishWifiStatus(wifiInfo);
    }

    public synchronized boolean isWifiConnected() {
        return wifiInfo != null &&
                wifiInfo.getSupplicantState().equals(SupplicantState.COMPLETED);
    }
}
