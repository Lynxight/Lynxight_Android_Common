package com.lynxight.common.Communication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.lynxight.common.utils.ConfigPreferences;
import com.lynxight.common.utils.Constants;
import com.lynxight.common.utils.SitePreferences;

public class ExtConfigReceiver extends BroadcastReceiver {
    private final static String TAG = ExtConfigReceiver.class.getSimpleName();

    public static final String EXTRA_SERVER_IP = "extra.server.ip";
    public static final String EXTRA_USE_BUGSNUG = "extra.bugsnug";
    public static final String EXTRA_SITE_NAME = "extra.sitename";
    public static final String EXTRA_SERIAL_NUMBER = "extra.serial.number";

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            String tmp = intent.getStringExtra(EXTRA_SERVER_IP);
            if (tmp != null) {
                SitePreferences.saveServerIp(tmp);
                context.sendBroadcast(new Intent(Constants.ACTION_EXT_SET_SERVER_IP));
                return;
            }

            tmp = intent.getStringExtra(EXTRA_USE_BUGSNUG);
            if(tmp != null) {
                boolean useBugSnug = Boolean.parseBoolean(tmp);
                Log.d(TAG, "received a setting for BugSnug use = " + useBugSnug);
                if (ConfigPreferences.isBugSnagInUse() != useBugSnug) {
                    ConfigPreferences.setBugsnagInUse(useBugSnug);
                }
            }
            tmp = intent.getStringExtra(EXTRA_SITE_NAME);
            if(tmp != null) {
                SitePreferences.saveSiteName(tmp.trim());
            }

            tmp = intent.getStringExtra(EXTRA_SERIAL_NUMBER);

            if (tmp != null) {
                ConfigPreferences.saveSerialNumber(tmp);
            }
            tmp = intent.getStringExtra(EXTRA_SERIAL_NUMBER);

            if (tmp != null) {
                ConfigPreferences.saveSerialNumber(tmp);
            }
        } catch (Exception e) {
            Log.e(TAG, "onReceive exception");
        }
    }
}
