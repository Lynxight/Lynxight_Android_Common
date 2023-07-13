package com.lynxight.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lynxight.common.data_model.PoolData;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SitePreferences {
    private final static String TAG = SitePreferences.class.getSimpleName();


    private static SharedPreferences siteData;
    private static final Object dataLock = new Object();

    private static final String KEY_LAST_CONNECTED_CVS = "lynxight.key.last.connected.cvs";
    private static final String KEY_RECENT_POOLS = "KEY_RECENT_POOLS";
    private static final String KEY_RECENT_POOLS_KEYS = KEY_RECENT_POOLS + "_KEYS";
    private static final String KEY_POOL_UPDATE_TAG = "KEY_POOL_UPDATE_TAG_";
    private static final String KEY_POOL_SWIMMEMR_MAP_TAG = "KEY_POOL_SWIMMEMR_MAP_TAG" ;
    private static final String KEY_SERVER_IP = "lynxight.key.server.ip";
    private static final String SITE_NAME = "SITE_NAME";
    public static final String DEFAULT_IP = "192.168.0.0";


    public static final long UPDATE_GRACE_MS = 5*1000*60; // 5 minutes grace




    public static void init(Context context) {
        siteData = PreferenceManager.getDefaultSharedPreferences(context);
    }

    // last connected cvs
    public static PoolData loadLastConnectedCvs() {
        synchronized (dataLock) {
            String jsonStr = siteData.getString(KEY_LAST_CONNECTED_CVS, "");
            return jsonStr == null || jsonStr.isEmpty() ? null :
                    new Gson().fromJson(jsonStr, PoolData.class);
        }
    }

    public static void saveLastConnectedCvs(PoolData cvsData) {
        synchronized (dataLock) {
            String jsonStr = new Gson().toJson(cvsData);
            siteData.edit().putString(KEY_LAST_CONNECTED_CVS, jsonStr).apply();
        }
    }

    public static Map<String, PoolData> fetchPoolDataMapFromStorage(){
        Map<String,PoolData> map = new HashMap<>();
        synchronized (dataLock) {
            String jsonStr = siteData.getString(KEY_RECENT_POOLS_KEYS, "");
            if(jsonStr == null || jsonStr.isEmpty()) {
                return map;
            }
            Type arrayType = new TypeToken<ArrayList<String>>(){}.getType();
            ArrayList<String> mapKeys = new Gson().fromJson(jsonStr, arrayType);

            for (String key : mapKeys){ // Note: this is to avoid concurrent modification on mapKeys !!
                long lastUpdated = getLastUpdate(key);
                if(System.currentTimeMillis() - lastUpdated < UPDATE_GRACE_MS){
                    jsonStr = siteData.getString(KEY_RECENT_POOLS+"_"+key, "");
                    if(jsonStr != null && !jsonStr.isEmpty()) {
                        PoolData poolData = new Gson().fromJson(jsonStr, PoolData.class);
                        map.put(key,poolData);
                    }
                }else {
                    siteData.edit().remove(KEY_RECENT_POOLS+"_"+key).apply();
                }
            }
        }
        saveLatestPoolMap(map);
        return map;
    }

    public static void saveLatestPoolMap(Map<String, PoolData> map) {
        ArrayList<String> keys = new ArrayList<>();
        keys.addAll(map.keySet());
        synchronized (dataLock) {
            String jsonStr = new Gson().toJson(keys);
            siteData.edit().putString(KEY_RECENT_POOLS_KEYS, jsonStr).apply();

            for(String key : keys){
                jsonStr = new Gson().toJson(map.get(key));
                siteData.edit().putString(KEY_RECENT_POOLS+"_"+key, jsonStr).apply();
            }
        }
    }

    public static void poolUpdated(String poolId){
        String poolTag = KEY_POOL_UPDATE_TAG+poolId ;
        synchronized (dataLock){
            siteData.edit().putLong(poolTag, System.currentTimeMillis()).apply();
        }
    }

    public static long getLastUpdate(String poolId){
        synchronized (dataLock){
            return siteData.getLong(KEY_POOL_UPDATE_TAG+poolId, 0L);
        }
    }

    public static void saveSwimmerCountMap(Map<String, String> map){
        synchronized (dataLock){
            Type mapType = new TypeToken<Map<String,String>>(){}.getType();
            String jsonStr = new Gson().toJson(map,mapType);
            siteData.edit().putString(KEY_POOL_SWIMMEMR_MAP_TAG, jsonStr).apply();
        }
    }

    public static Map<String, String> getCachedSwimmersCount(){
        Map<String, String> map ;
        synchronized (dataLock){
            String json = siteData.getString(KEY_POOL_SWIMMEMR_MAP_TAG, "");
            if(json == null || json.isEmpty())
                return null;
            Type mapType = new TypeToken<Map<String,String>>(){}.getType();
            map = new Gson().fromJson(json,mapType);
        }
        ArrayList<String> tempKeysToAvoidConcurrentModification = new ArrayList<>(map.keySet());
        for(String poolId : tempKeysToAvoidConcurrentModification){
            long lastUpdated = getLastUpdate(poolId);
            if(System.currentTimeMillis() - lastUpdated > UPDATE_GRACE_MS) {
                map.remove(poolId);
            }
        }
        return map;
    }

    private static String thisWatch = null;
    private static final String KEY_CONNECTED_WATCHES_MAP = "KEY_CONNECTED_WATCHES_MAP" ;
    public static void updateWatch(String watchId){
        if(thisWatch == null)
            thisWatch = ConfigPreferences.deviceId();
        if(watchId.equals(thisWatch))
            return;
        Map<String, Long> watchMap;
        synchronized (dataLock) {
            watchMap = getWatchUpdateMap();
            watchMap.put(watchId,System.currentTimeMillis());
            Type mapType = new TypeToken<Map<String,Long>>(){}.getType();
            String jsonStr = new Gson().toJson(watchMap,mapType);
            siteData.edit().putString(KEY_CONNECTED_WATCHES_MAP, jsonStr).apply();
        }
    }

    public static int getCachedWatchCount(){
        Map<String, Long> watchMap;
        synchronized (dataLock) {
            watchMap = getWatchUpdateMap();
        }
        if (watchMap == null) return 0;
        ArrayList<String> tempKeysToAvoidConcurrentModification = new ArrayList<>(watchMap.keySet());
        int count = 0;
        for(String watchId : tempKeysToAvoidConcurrentModification){
            if(System.currentTimeMillis() - watchMap.get(watchId) < UPDATE_GRACE_MS) {
                ++count;
            }
        }
        return count;
    }

    @Nullable
    private static Map<String, Long> getWatchUpdateMap() {
        Map<String, Long> watchMap;

        String json = siteData.getString(KEY_CONNECTED_WATCHES_MAP, "");
        if(json == null || json.isEmpty())
            return new HashMap<>();
        Type mapType = new TypeToken<Map<String,Long>>(){}.getType();
        watchMap = new Gson().fromJson(json,mapType);
        return watchMap;
    }


    public static void flushCache() {
        saveLatestPoolMap(new HashMap<>());
        saveSwimmerCountMap(new HashMap<>());
        synchronized (dataLock) {
            siteData.edit().putString(KEY_LAST_CONNECTED_CVS, "").apply();
        }
    }

    public static String loadServerIp() {
        synchronized (dataLock) {
            return siteData.getString(KEY_SERVER_IP, DEFAULT_IP);
        }
    }

    public static void saveServerIp(String serverIp) {
        synchronized (dataLock) {
            siteData.edit().putString(KEY_SERVER_IP, serverIp).apply();
        }
    }

    public static String getSiteName() {
        synchronized (dataLock) {
            return siteData.getString(SITE_NAME, "Unknown");
        }
    }

    public static void saveSiteName(String name) {
        synchronized (dataLock) {
            siteData.edit().putString(SITE_NAME, name).apply();
        }
    }

}
