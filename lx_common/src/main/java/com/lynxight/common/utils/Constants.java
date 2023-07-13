package com.lynxight.common.utils;

public class Constants {


    /**
     * Permission request codes
     */
    public static final int PERMISSION_INTERNET_REQUEST = 1002;
    public static final int PERMISSION_WIFI_STATE_REQUEST = 1008;
    public static final int PERMISSION_BLUETOOTH_REQUEST = 1009;
    public static final int PERMISSION_BLUETOOTH_ADMIN_REQUEST = 1010;
    public static final int PERMISSION_SYSTEM_ALERT_WINDOW_REQUEST = 1012;
    public static final int PERMISSION_OVERLAY_REQUEST = 1013;
    public static final int PERMISSION_AUDIO_RECORD_REQUEST = 1014;

    /**
     * Actions
     */

    public final static String ACTION_EXT_SET_SERVER_IP = "action.ext.set.server.ip";
    public final static String ACTION_SET_SCREEN_BACKLIGHT = "action.set.screen.backlight";
    public final static String ACTION_SCREEN_TOUCHED = "action.screen.touched";


    public static final String EXTRA_BACKLIGHT_MODE = "extra.backlight.mode";

    /**
     * App preference keys
     */


    /**
     * Global settings tags
     */
    public static final String TAG_ROOT = "lynxight";
    public static final String TAG_SETTINGS = "settings";
    public static final String TAG_SERVER_IP = "serverIp";


    /**
     * Timer settings
     */
    public static final int TIMER_TICK_INTERVAL = 1000;

    public static final int MAIN_DEMO_TAP_TIMER_INTERVAL = 3 * TIMER_TICK_INTERVAL;
    public static final int DEV_PANEL_TAP_TIMER_INTERVAL = 3 * TIMER_TICK_INTERVAL;
    public static final int MAIN_DISCONNECTED_STATE_TIMER_INTERVAL = 10 * TIMER_TICK_INTERVAL;
    public static final int MAIN_ERROR_STATE_TIMER_INTERVAL = 5 * TIMER_TICK_INTERVAL;


    public static final int POOL_DISPLAY_TIMER_INTERVAL = 15 * TIMER_TICK_INTERVAL;
    public static final int BUGSNUG_TIMER_INTERVAL = 30 * 60 * TIMER_TICK_INTERVAL; // 30 minutes

    public static final float STATUS_INTERVAL_NANO_DIVIDER = 1000000000.0f;
    public static final int STATUS_INTERVAL_TIMEOUT_MINUTES = 60;
    public static final int WIFI_RECONNECTION_INTERVAL_SECONDS = 20;

    /**
     * Main page config
     */
    public static final int TAPS_DEMO_MODE = 2;
    public static final int TAPS_DEV_PANEL = 7;
    public static final int TAPS_DEV_SHOW_TAPS = 3;

    /**
     * Main UI config
     */
    public static final String SWIMMER_COUNT_PLACEHOLDER = "--";


    /**
     * Battery info settings
     */
    public static final int BATTERY_INFO_STEP_1 = 5;
    public static final int BATTERY_INFO_STEP_2 = 40;
    public static final int BATTERY_INFO_STEP_3 = 90;


    /**
     * Connection status texts
     */
    public static final String CONNECTED_STATUS_ADAPTER_OFF = "Adapter OFF";
    public static final String CONNECTED_STATUS_UNKNOWN = "Unknown";


    /**
     * screen dim
     */
    public final static float SCREEN_BRIGHTNESS_ON_VALUE = (float) 0.6;
    public final static float SCREEN_BRIGHTNESS_DIM_VALUE = (float) 0.3;
    public final static float SCREEN_BRIGHTNESS_DARK_VALUE = (float) 0;

    public final static int CHECK_SCREEN_DIM_INTERVAL = 10 * 1000;

    /**
     * Misc
     */
    public static final String TIME_FORMAT = "HH:mm";
    public static final String WIFI_LOCK_TAG = "watchWifiLock";

    public static final String DEFAULT_MAC = "02:00:00:00:00:00";
    public static final String MAC_NETWORK_INT_NAME = "wlan0";
    public static final int VIBRATION_MILLIS = 200;
    public static final int IP_SUBNET_COUNT = 4;
    public static final int IP_SUBNET_MIN = 0;
    public static final int IP_SUBNET_MAX = 255;
    public static final int RECOURSE_BUFFER_CHUNK_SIZE = 1024;


}