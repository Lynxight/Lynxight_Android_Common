package com.lynxight.common.utils;

import static com.lynxight.common.utils.SitePreferences.flushCache;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lynxight.common.data_model.User;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ConfigPreferences {
    private final static String TAG = ConfigPreferences.class.getSimpleName();

    private static SharedPreferences config;
    private static final Object configLock = new Object();

    private static final String KEY_DEVICE_ID = "lynxight.key.device.id";
    private static final String KEY_MUTE_TIMEOUT = "lynxight.key.mute.timeout";
    private static final String KEY_LOCALE_CHANGED = "KEY_LOCALE_CHANGED";
    private static final String KEY_SECONDS_IN_MOTION = "lynxight.key.seconds.in.motion";
    private static final String KEY_SELECTED_LOCALE = "lynxight.key.locale";
    private static final String BUGSNAG_IN_USE = "BUGSNAG_IN_USE";
    private static final String DEMO_MODE_TAG = "DEMO_MODE_TAG";
    private static final String KEY_WATCH_CONFIGURED = "lynxight.key.is.configured";
    private static final String KEY_SERIAL_NUMBER = "lynxight.key.serial.number";
    private static final String KEY_USER_LIST = "lynxight.key.user.list";
    private static final String KEY_CURRENT_USER = "lynxight.key.current.user";
    private static final String KEY_LAST_USER_UPDATE = "lynxight.key.last.user.update";
    private static final String SNAP_ENABLED = "lynxight.key.snap.enabled";


    private static final long USER_CACHING_PERIOD_MS = 60 * 2 * 1000; // 2 minutes


    public static void init(Context context) {
        config = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static String deviceId() {
        synchronized (configLock) {
            String serialNumber = config.getString(KEY_SERIAL_NUMBER, "");

            if (!serialNumber.isEmpty())
                return serialNumber;

            String watchId = config.getString(KEY_DEVICE_ID, "");

            if (watchId.isEmpty()) {
                watchId = UUID.randomUUID().toString();
                watchId = watchId.substring(0, Math.min(watchId.length(), 10));
                
                config.edit().putString(KEY_DEVICE_ID, watchId).apply();
            }
            return watchId;
        }
    }

    public static void saveSerialNumber(String serialNumber) {
        synchronized (configLock) {
            config.edit().putString(KEY_SERIAL_NUMBER, serialNumber).apply();
        }
    }

    public static int loadMinutesInMotion() {
        synchronized (configLock) {
            int sec = config.getInt(KEY_SECONDS_IN_MOTION, 0);
            return sec / 60;
        }
    }

    public static int loadSecondsInMotions() {
        synchronized (configLock) {
            return config.getInt(KEY_SECONDS_IN_MOTION, 0);
        }
    }

    public static void incrementSecondsInMotion(int seconds) {
        int sec = loadSecondsInMotions();
        synchronized (configLock) {
            config.edit().putInt(KEY_SECONDS_IN_MOTION, sec + seconds).apply();
        }
    }

    // server ip

    public static boolean isBugSnagInUse() {
        synchronized (configLock) {
            return config.getBoolean(BUGSNAG_IN_USE, true);
        }
    }

    public static void setBugsnagInUse(boolean inUse) {
        synchronized (configLock) {
            config.edit().putBoolean(BUGSNAG_IN_USE, inUse).apply();
        }
    }



    public static void saveMuteTimeout(long timeout) {
        synchronized (configLock) {
            config.edit().putLong(KEY_MUTE_TIMEOUT, timeout).apply();
        }
    }



    public static boolean didLocaleChange(){
        synchronized (configLock){
            return config.getBoolean(KEY_LOCALE_CHANGED, false);
        }
    }

    public static void dismissLocaleChanged(){
        synchronized (configLock){
            config.edit().putBoolean(KEY_LOCALE_CHANGED, false).apply();
        }
    }

    public static void saveLocalePref(String locale) {
        synchronized (configLock) {
            config.edit().putString(KEY_SELECTED_LOCALE, locale).apply();
            config.edit().putBoolean(KEY_LOCALE_CHANGED, true).apply();
        }
    }
    public static String getSelectedLocale() {
        return getSelectedLocale("");
    }
    public static String getSelectedLocale(String def) {
        String selected;

        synchronized (configLock) {
            selected = config.getString(KEY_SELECTED_LOCALE, "");
        }
        if(selected.isEmpty()){
            saveLocalePref(def);
            return def;
        }
        return selected;
    }

    public static boolean isDemoMode(){
        synchronized (configLock){
            return config.getBoolean(DEMO_MODE_TAG, false);
        }
    }

    public static void setDemoMode(boolean val){
        if(isDemoMode() == val)
            return;
        flushCache();
        synchronized (configLock) {
            config.edit().putBoolean(DEMO_MODE_TAG, val).apply();
        }
    }

    public static boolean isConfigured() {
        synchronized (configLock) {
            return config.getBoolean(KEY_WATCH_CONFIGURED, false);
        }
    }

    public static void setConfigured(boolean isConfigured) {
        synchronized (configLock) {
            config.edit().putBoolean(KEY_WATCH_CONFIGURED, isConfigured).apply();
        }
    }

    public static void saveUserList(ArrayList<User> users) {
        synchronized (configLock) {
            config.edit().putString(KEY_USER_LIST, new Gson().toJson(users)).apply();
        }
    }

    public static List<User> getUserList() {
        synchronized (configLock) {
            Gson gson = new Gson();
            Type arrayType = new TypeToken<ArrayList<User>>(){}.getType();

            return gson.fromJson(config.getString(KEY_USER_LIST, ""), arrayType);
        }
    }

    public static void handleNewUser(User user) {
        synchronized (configLock) {
            if (user == null) {
                config.edit().putString(KEY_CURRENT_USER, "").apply();
                return;
            }

            config.edit().putString(KEY_CURRENT_USER, new Gson().toJson(user)).apply();
            config.edit().putLong(KEY_LAST_USER_UPDATE, System.currentTimeMillis()).apply();
        }
    }

    public static User getCurrentUser() {
        synchronized (configLock) {
            String currentUserJson = config.getString(KEY_CURRENT_USER, "");
            long lastUserUpdateTime = config.getLong(KEY_LAST_USER_UPDATE, 0);

            if (currentUserJson.isEmpty())
                return null;

            if (System.currentTimeMillis() - lastUserUpdateTime > USER_CACHING_PERIOD_MS) { // User is invalid
                config.edit().putString(KEY_CURRENT_USER, "").apply();
                return null;
            }

            return new Gson().fromJson(currentUserJson, User.class);
        }
    }
    public static long getCurrentUserStartTime() {
        synchronized (configLock) {
            return config.getLong(KEY_LAST_USER_UPDATE, 0);
        }
    }

    public static boolean isSnapEnabled() {
        synchronized (configLock) {
            return config.getBoolean(SNAP_ENABLED, false);
        }
    }

    public static void setSnapStatus(boolean enabled) {
        synchronized (configLock) {
            config.edit().putBoolean(SNAP_ENABLED, enabled).apply();
        }
    }
}
