package com.lynxight.common.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;

import com.bugsnag.android.Bugsnag;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class InteractionsCollector {
    private final static String TAG = InteractionsCollector.class.getSimpleName();
    protected static final String TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String BASE_INTERACTION_KEY = "interaction.{}";
    private static final String LAST_INTERACTION_TIMESTAMP_KEY = "timestamp_interaction";
    private static String lastTimestamp = "";

    private static SharedPreferences interactions;
    private static final Object interactionsLock = new Object();

    /*
        Interaction JSON structure:
            * interaction: str
            * count: int
     */

    public static void init(Context context) {
        interactions = PreferenceManager.getDefaultSharedPreferences(context);
    }

    private static String getInteractionId(String interactionJson) {
        try {
            return new JSONObject(interactionJson).getString("interaction");
        } catch (JSONException ignored) {
            return "unknown-interaction";
        }
    }

    public static void saveInteraction(View view) {
        String resourceId = getId(view);

        if (resourceId == null || resourceId.equals("no-id"))
            return;

        saveInteraction(resourceId);
    }

    public static void saveInteraction(String resourceId) {
        if (!ConfigPreferences.isBugSnagInUse() || !Bugsnag.isStarted()) // Don't write to SharedPreferences if bugsnag is off, since that way it'll never be cleared.
            return;

        synchronized (interactionsLock) {
            String timestamp = Utils.getCurrentDateTimeString(TIME_FORMAT);
            String keyWithTimestamp = BASE_INTERACTION_KEY.replace("{}", timestamp);

            writeUpdatedInteraction(resourceId, timestamp, keyWithTimestamp);

            // Update timestamp:

            lastTimestamp = timestamp;
            interactions.edit().putString(LAST_INTERACTION_TIMESTAMP_KEY, lastTimestamp).apply();
        }
    }

    private static void writeUpdatedInteraction(String resourceId, String timestamp, String keyWithTimestamp) { // Note: not to be called unless in a synchronized block
        // Retrieve last timestamp & interaction:

        lastTimestamp = interactions.getString(LAST_INTERACTION_TIMESTAMP_KEY, "");
        String lastInteractionJSON = interactions.getString("interaction." + lastTimestamp, "");
        String lastInteractionId = getInteractionId(lastInteractionJSON);

        JSONObject jo = updateInteractionJSON(resourceId, lastInteractionJSON, lastInteractionId); // JSON example: {"2023-01-01 11:56:03":"{\"interaction\":\"Swipe to: Test4\",\"count\":2}"

        // Remove old timestamp key:

        if (lastInteractionId.equals(resourceId) && !lastTimestamp.equals(timestamp)) { // Same interaction, but with older timestamp -> remove.
            interactions.edit().remove("interaction." + lastTimestamp).apply();
        }

        interactions.edit().putString(keyWithTimestamp, jo.toString()).apply();
    }

    private static JSONObject updateInteractionJSON(String resourceId, String lastInteractionJSON, String lastInteractionId) {
        JSONObject jo = new JSONObject();
        if (lastInteractionId.equals(resourceId)) { // Same as last interaction
            try {
                jo = new JSONObject(lastInteractionJSON);
                int count = jo.getInt("count");
                jo.put("count", ++count);
            } catch (JSONException ignored) {}
        }
        else {
            try {
                jo.put("interaction", resourceId);
                jo.put("count", 1);
            } catch (JSONException ignored) {}
        }
        return jo;
    }

    public static void clearInteractions() {
        synchronized (interactionsLock) {
            Set<String> interactionKeys = interactions.getAll().keySet();

            for (String interactionKey : interactionKeys) {
                if (interactionKey.startsWith("interaction.")) {
                    interactions.edit().remove(interactionKey).apply();
                }
            }
        }
    }

    public static String getInteractions() {
        synchronized (interactionsLock) {
            JSONObject jsonObject = new JSONObject();
            Map<String, ?> prefsMap = interactions.getAll();
            SortedSet<String> keys = new TreeSet<>(prefsMap.keySet());
            for (String key : keys) {
                if (key.startsWith("interaction.")) {
                    Object value = prefsMap.get(key);
                    try {
                        String correctTime = key.substring(Math.min(key.lastIndexOf(".") + 1, key.length() - 1));
                        jsonObject.put(correctTime, value == null ? "" : value.toString());
                    } catch (JSONException ignored) {}
                }
            }

            return jsonObject.toString();
        }
    }

    public static String getId(View view) {
        if (view.getId() == View.NO_ID)
            return "no-id";

        String viewStringId = view.getResources().getResourceName(view.getId());

        if (viewStringId.contains("/"))
            return viewStringId.substring(Math.min(viewStringId.indexOf("/") + 1, viewStringId.length() - 1));

        return "no-id";
    }
}
