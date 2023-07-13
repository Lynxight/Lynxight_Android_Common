package com.lynxight.common.utils;

import static android.content.Context.WIFI_SERVICE;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.DrawableContainer;
import android.graphics.drawable.GradientDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.lynxight.common.data_model.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Utils {
    private final static String TAG = Utils.class.getSimpleName();

    public static String resourceImageToBase64(Context context, int resource) {
        Resources res = context.getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(res, resource);
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Base64.encodeToString(byteStream.toByteArray(), Base64.DEFAULT);
    }

    public static Bitmap base64ToBitmap(String base64Image) {
        byte[] imageBytes = Base64.decode(base64Image, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }

    public static String getCurrentDateTimeString(Resources resources, String format) {
        return new SimpleDateFormat(format, getLocale(resources)).format(new Date());
    }

    public static String getCurrentDateTimeString(String format) {
        return new SimpleDateFormat(format).format(new Date());
    }


    public static Date getDateFromString(Resources resources, String format, String dateStr) {
        return new SimpleDateFormat(format,
                getLocale(resources)).parse(dateStr, new ParsePosition(0));
    }

    private static Locale getLocale(Resources resources) {
        return resources.getConfiguration().getLocales().get(0);
    }

    public static String getMacAddress() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase(Constants.MAC_NETWORK_INT_NAME)) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
            Log.e(TAG, "MAC address get failed: " + ex.toString());
            ex.printStackTrace();
        }
        return Constants.DEFAULT_MAC;
    }

//    public static ArrayList<Integer> jsonArray2IntArray(JSONArray jsonArray) {
//        ArrayList<Integer> intArray = new ArrayList<>();
//        for (int i = 0; i < jsonArray.length(); ++i) {
//            intArray.add(jsonArray.optInt(i));
//        }
//        return intArray;
//    }

//    public static ArrayList<Float> jsonArray2FloatArray(JSONArray jsonArray) {
//        ArrayList<Float> intArray = new ArrayList<>();
//        for (int i = 0; i < jsonArray.length(); ++i) {
//            intArray.add((float) jsonArray.optDouble(i));
//        }
//        return intArray;
//    }

    public static ArrayList<Pair<String, String>> parseImageArray(JSONArray jsonArray)
            throws JSONException
    {
        ArrayList<Pair<String, String>> imageList = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); ++i) {
            JSONObject imageObj = jsonArray.getJSONObject(i);
            imageList.add(
                    new Pair<>(
                            imageObj.getString("timestamp"),
                            imageObj.getString("image")));
        }
        return imageList;
    }

    public static ArrayList<Pair<String, String>> parseSnapArray(JSONArray jsonArray)
            throws JSONException
    {
        ArrayList<Pair<String, String>> imageList = new ArrayList<>();
        Log.d(TAG, "parsed snap");
        for (int i = 0; i < jsonArray.length(); ++i) {
            JSONObject imageObj = jsonArray.getJSONObject(i);
            imageList.add(
                    new Pair<>(
                            imageObj.keys().next(),
                            imageObj.getString(imageObj.keys().next())));
        }
        return imageList;
    }

    public static ArrayList<User> parseUserList(JSONArray jsonArray) throws JSONException {
        ArrayList<User> users = new ArrayList<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject userObj = jsonArray.getJSONObject(i);
            try{
                users.add(new User(userObj.getString("name"), userObj.getString("phone"), userObj.getString("acronym")));
            } catch(JSONException e){
                users.add(new User(userObj.getString("name"), userObj.getString("phone"), "J.F.K"));
            }
        }
        return users;
    }

    public static void setTextViewBackgroundColor(TextView textView, int color) {
        Drawable background = textView.getBackground();
        DrawableContainer.DrawableContainerState drawableContainerState =
                (DrawableContainer.DrawableContainerState) background.getConstantState();
        assert drawableContainerState != null;
        GradientDrawable selectedDrawable =
                (GradientDrawable) drawableContainerState.getChildren()[0];
        selectedDrawable.setColor(color);
    }



    public static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }



    @NonNull
    public static String getIPString(Context context) {
        WifiInfo wifiInfo = ((WifiManager) context.getSystemService(WIFI_SERVICE)).getConnectionInfo();
        byte[] watchIPBytes = BigInteger.valueOf(wifiInfo.getIpAddress()).toByteArray();
        List<Byte> byteList = convertBytesToList(watchIPBytes);
        Collections.reverse(byteList);
        try {
            InetAddress watchIP = InetAddress.getByAddress(convertListToBytes(byteList));
            if (watchIP != null) {
                return watchIP.getHostAddress();
            }
        } catch (UnknownHostException e) {
            Log.e(TAG, "UnknownHostException: " + e);
            e.printStackTrace();
        }
        return "";
    }

    public static List<Byte> convertBytesToList(byte[] bytes) {
        final List<Byte> list = new ArrayList<>();
        for (byte b : bytes) {
            list.add(b);
        }
        return list;
    }

    public static byte[] convertListToBytes(List<Byte> bytesList) {
        final byte[] bytes = new byte[bytesList.size()];
        for (int i = 0; i < bytesList.size(); i++) {
            bytes[i] = bytesList.get(i);
        }
        return bytes;
    }

}
