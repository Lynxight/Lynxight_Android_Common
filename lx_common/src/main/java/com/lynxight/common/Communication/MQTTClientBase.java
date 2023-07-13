package com.lynxight.common.Communication;


import android.content.Context;
import android.text.format.Formatter;
import android.util.Log;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.lynxight.common.data_model.PoolFullData;
import com.lynxight.common.data_model.User;
import com.lynxight.common.managers.SystemResourceManager;
import com.lynxight.common.utils.ConfigPreferences;
import com.lynxight.common.utils.Constants;
import com.lynxight.common.utils.Utils;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MQTTClientBase {
    private final static String TAG = MQTTClientBase.class.getSimpleName();
    protected static final String MESSAGE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    @Inject
    SystemResourceManager systemResourceManager;

    @Inject
    Context context;

    @Inject
    public MQTTClientBase() {}

    /**
     * {@link MessageType} represents a type of message passed between watch and CVS
     * As part of their shared API, they identify messages by integer constants
     * representing the value of their {@link MessageType}
     * Therefore, the integer value of a {@link MessageType} <b>must</b> match its
     * integer value on the CVS
     */
    public enum MessageType {
        CvsStatus(1),
        CvsAlert(2),
        CvsInfo(4),
        CvsEnableErrorReports(13),
        CvsSiteName(14),
        CvsFeatures(20),
        CvsUserList(19),

        //BACK_COMPAT these are for backward compatibility for pre-swipper interface
        PoolImage(3),
        CvsPeriodicInfo(0),
        WatchCvsInfoRequest(5),

        // status
        WatchStatus(6),
        WatchAlertFeedback(7),
        WatchReport(8),
        WatchSwimmerCount(9),
        WatchTeamRequest(17),
        WatchTeamResponse(22),
        WatchConnected(23),
        // ack
        WatchAck(10),
        WatchError(11),
        WatchLifeguardNoticedAlert(12),
        WatchOwnership(18),
        WatchVoicemail(21),

        // Snap
        WatchGetSnap(15),
        WatchSnap(16);


        public final int value;

        MessageType(int value) {
            this.value = value;
        }

        public static MessageType getType(int value) {
            for (MessageType type : values()) {
                if (value == type.value) {
                    return type;
                }
            }
            return null;
        }
    }




    public abstract class Message {
        private final String TAG = Message.class.getSimpleName();

        public MessageType msgType;
        public String msgID;
        public String msgTime;
        public String msgSource;
        public String msgExpTime;

        public Message(String jsonStr) throws JSONException {
            JSONObject jo = new JSONObject(jsonStr);
            msgID = jo.getString("message_id");
            msgType = MessageType.getType(jo.getInt("type"));
            msgTime = jo.getString("time");
            msgSource = jo.getString("source");
            msgExpTime = jo.getString(("expiration_time"));
        }

        public Message(MessageType msgType,
                       String msgID,
                       String msgTime,
                       String msgSource,
                       String msgExpTime) {
            this.msgID = msgID;
            this.msgTime = msgTime;
            this.msgType = msgType;
            this.msgSource = msgSource;
            this.msgExpTime = msgExpTime;
        }

        public Message(MessageType msgType) {
            this(msgType, UUID.randomUUID().toString(),
                    Utils.getCurrentDateTimeString(MQTTClientBase.this.context.getResources(),
                            MESSAGE_TIME_FORMAT),
                    ConfigPreferences.deviceId(), "");
        }

        public JSONObject toJson() throws JSONException {
            JSONObject jo = new JSONObject();
            jo.put("message_id", msgID).
                    put("type", msgType.value).
                    put("time", msgTime).
                    put("source", msgSource).
                    put("expiration_time", msgExpTime);
            return jo;
        }

        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            return jo.toString();
        }

        protected void extractFromJson(JSONObject jo) throws JSONException {
        };
        protected void dataToJson(JSONObject jo) throws JSONException{
        };
    }

    //*************************************************************************

    public class PoolFullInfoMessage extends Message {
        private final String TAG = PoolFullInfoMessage.class.getSimpleName();

        public PoolFullData poolFullInfo;

        public PoolFullInfoMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            poolFullInfo = new Gson()
                    .fromJson(jo.getString("pool_info"),
                            new TypeToken<PoolFullData>() {
                            }.getType());


        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();
            String json = new Gson().toJson(poolFullInfo);
            jo.put("pool_info", json);
            return jo;
        }
    }

    public class OwnershipMessage extends Message {
        private final String TAG = OwnershipMessage.class.getSimpleName();

        public String alertId;
        public String user;

        public OwnershipMessage(
                String alertId,
                String user){
            super(MessageType.WatchOwnership);
            this.alertId = alertId;
            this.user = user;
        }


        public OwnershipMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            alertId = jo.getString("alert_id");
            user = jo.getString("user");
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();
            jo.put("alert_id", alertId);
            jo.put("user", user);
            return jo;
        }
    }

    public class TeamRequestMessage extends Notification {
        private final String TAG = TeamRequestMessage.class.getSimpleName();

        public String iconResource;
        public String iconCapture ;
        public String confirmationCapture ;
        public String messageText ;
        public String senderName ;
        public String senderWatchId ;
        public String poolDisplayName;

        public TeamRequestMessage(
                Severity severity,
                String iconResource,
                String iconCapture,
                String senderName,
                String sendedrWatchId,
                String messageText,
                String poolDisplayName,
                String confirmationCapture ){
            super(MessageType.WatchTeamRequest);
            this.severity = severity;
            this.iconResource = iconResource;
            this.iconCapture = iconCapture;
            this.confirmationCapture = confirmationCapture;
            this.poolDisplayName = poolDisplayName;
            this.senderName = senderName;
            messageText = messageText.replace("{pool}",poolDisplayName);
            messageText = messageText.replace("{user}",senderName);
            this.messageText = messageText;

            this.senderWatchId = sendedrWatchId;
        }


        public TeamRequestMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            severity = Severity.getSeverity(jo.getString("display_color"));
            iconResource = jo.getString("icon_resource");
            iconCapture = jo.getString("icon_capture");
            confirmationCapture = jo.getString("confirm_capture");
            senderName = jo.getString("sender_name");
            poolDisplayName = jo.getString("pool_display_name");
            messageText = jo.getString("message_text");
            senderWatchId = jo.getString("sender_watch_id");
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();

            jo.put("icon_resource",iconResource);
            jo.put("icon_capture", iconCapture);
            jo.put("confirm_capture", confirmationCapture);
            jo.put("sender_name", senderName);
            jo.put("pool_display_name", poolDisplayName);
            jo.put("message_text", messageText);
            jo.put("display_color", severity.value);
            jo.put("sender_watch_id",senderWatchId);
            return jo;
        }

        public int compare(TeamRequestMessage rhs){
            return this.severity.compare(rhs.severity);
        }


    }

    public class TeamResponseMessage extends Message {
        private final String TAG = TeamResponseMessage.class.getSimpleName();

        public String iconResource;
        public String messageText ;
        public String responderName ;
        public String senderWatchId ;
        public String originatingRequestId ;

        public TeamResponseMessage(String iconResource,
                                   String messageText,
                                   String responderName,
                                   String senderWatchId,
                                   String originatingRequestId){
            super(MessageType.WatchTeamResponse);
            this.iconResource =  iconResource;
            this.responderName =  responderName;
            messageText = messageText.replace("{responder}",responderName);
            this.messageText =  messageText;
            this.senderWatchId =  senderWatchId;
            this.originatingRequestId =  originatingRequestId;
        }

        public TeamResponseMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            iconResource = jo.getString("icon_resource");
            responderName = jo.getString("responder_name");
            messageText = jo.getString("message_text");
            originatingRequestId = jo.getString("request_id");
            senderWatchId = jo.getString("sender_watch_id");
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();

            jo.put("icon_resource",iconResource);
            jo.put("responder_name",responderName);
            jo.put("message_text",messageText);
            jo.put("request_id",originatingRequestId);
            jo.put("sender_watch_id",senderWatchId);
            return jo;
        }
    }

    public class WatchConnectedMessage extends Message {

        public WatchConnectedMessage(){
            super(MessageType.WatchConnected);
        }

        public WatchConnectedMessage(String jStr) throws JSONException {
            super(jStr);
        }
    }

    public class SiteNameMessage extends Message {
        private final String TAG = SiteNameMessage.class.getSimpleName();

        public String siteName;

        public SiteNameMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            if (jo.has("site_name")) {
                siteName = jo.getString("site_name");
            }
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();
            jo.put("site_name", siteName);
            return jo;
        }
    }

    public class EnableErrorReports extends Message {
        private final String TAG = EnableErrorReports.class.getSimpleName();

        public boolean errorReports;

        public EnableErrorReports(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            if (jo.has("report_watch_errors")) {
                errorReports = jo.getBoolean("report_watch_errors");
            }
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();
            jo.put("report_watch_errors", errorReports);
            return jo;
        }
    }

    public class FeaturesMessage extends Message {
        private final String TAG = EnableErrorReports.class.getSimpleName();

        public boolean snapEnabled;

        public FeaturesMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            if (jo.has("snap_enabled")) {
                snapEnabled = jo.getBoolean("snap_enabled");
            }
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();
            jo.put("snap_enabled", snapEnabled);
            return jo;
        }
    }


    /**
     * Sent from server to the watch
     * Represents the current status of the connection
     * between the server and the watch
     */
    public class PoolSwimmersCountMessage extends Message {
        private final String TAG = PoolSwimmersCountMessage.class.getSimpleName();

        public int swimmerCount;
        public String swimmerCountText;
//        public String sourceNotInUse;

        public PoolSwimmersCountMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            swimmerCount = jo.getInt("swimmer_count");
            swimmerCountText = getSwimmerCountText();
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();
            jo.put("swimmer_count", swimmerCount);
            return jo;
        }

        private String getSwimmerCountText() {
            if (swimmerCount < 0) {
                return Constants.SWIMMER_COUNT_PLACEHOLDER;
            }

            return String.valueOf(swimmerCount);
        }
    }


    public enum Severity {
        GRAY("GRAY"),
        BLUE("BLUE"),
        GREEN("GREEN"),
        YELLOW("YELLOW"),
        RED("RED");

        public final String value;

        Severity(String value) {
            this.value = value;
        }

        public static Severity getSeverity(String value) {
            for (Severity severity : values()) {
                if (value.equals(severity.value)) {
                    return severity;
                }
            }
            return GRAY;
        }
        public int getNumValue(){
            switch(value){
                case "RED": return 4;
                case "YELLOW": return 3;
                case "GREEN": return 2;
                case "BLUE": return 1;
                case "GRAY": return 0;
                default: return 0;
            }
        }
        public int compare(Severity rhs){
            if(getNumValue() <= rhs.getNumValue())
                return 1;
            return -1;
        }
    }

    public class Notification extends Message {
        public Severity severity = Severity.GRAY;

        public Notification(String jsonStr) throws JSONException {
            super(jsonStr);
        }

        public Notification(MessageType msgType, String msgID, String msgTime, String msgSource, String msgExpTime) {
            super(msgType, msgID, msgTime, msgSource, msgExpTime);
        }

        public Notification(MessageType msgType) {
            super(msgType);
        }
         public int prioritize(Notification rhs){
            return severity.compare(rhs.severity);
         }
         public boolean equals(Notification rhs){
            return msgID.equals(rhs.msgID);
         }
    }

    public class PoolAlertMessage extends Notification {
        public String alertType;
        public ArrayList<Pair<String, String>> images;
        public ArrayList<Float> worldCoordinates;

        // legacy coordinates (for back compatibility)
        public int coordinateX;
        public int coordinateY;

        public PoolAlertMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            alertType = jo.getString("alert_type");
            severity = Severity.getSeverity(jo.getString("display_color"));
            if (jo.has("location")) {
                final ArrayList<Float> coordinates = new Gson().fromJson(jo.getJSONArray("location").toString(),
                        new TypeToken<ArrayList<Float>>() {
                        }.getType());

                coordinateX = coordinates.get(0).intValue();
                coordinateY = coordinates.get(1).intValue();
            }

            if (jo.has("world_coordinates")) {
                worldCoordinates = new Gson()
                        .fromJson(jo.getJSONArray("world_coordinates").toString(),
                                new TypeToken<ArrayList<Float>>() {
                                }.getType());
            }

            if (jo.has("image_list")) {
                images = Utils.parseImageArray(jo.getJSONArray("image_list"));
            }
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();
            float[] location = new float[]{coordinateX, coordinateY};
            JSONArray jsonArray = new JSONArray(location);
            jo.put("location", jsonArray);
            jo.put("alert_type", alertType);
            jo.put("display_color", severity.value);

            jsonArray = new JSONArray(worldCoordinates);
            jo.put("world_coordinates", jsonArray);

            JSONArray imagesArray = new JSONArray();
            for (Pair<String, String> img : images) {
                JSONObject imgObj = new JSONObject();
                imgObj.put("timestamp", img.first);
                imgObj.put("image", img.second);
                imagesArray.put(imgObj);
            }
            jo.put("image_list", imagesArray);
            return jo;
        }


    }

    public class PoolSnapMessage extends Message {
        private final String TAG = PoolSnapMessage.class.getSimpleName();

        public ArrayList<Pair<String, String>> imageList;
        public String poolId;
        public String uuid;

        public PoolSnapMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            if (jo.has("image_list")) {
                imageList = Utils.parseSnapArray(jo.getJSONArray("image_list"));
            }

            if (jo.has("pool_id")) {
                poolId = jo.getString("pool_id");
            }

            if (jo.has("uuid")) {
                uuid = jo.getString("uuid");
            }
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();

            jo.put("pool_id", poolId);
            jo.put("uuid", uuid);

            JSONArray imagesArray = new JSONArray();
            for (Pair<String, String> img : imageList) {
                JSONObject imgObj = new JSONObject();
                imgObj.put("image_description", img.first);
                imgObj.put("image", img.second);
                imagesArray.put(imgObj);
            }
            jo.put("image_list", imagesArray);
            return jo;
        }
    }

    public class UserListMessage extends Message {
        private final String TAG = PoolSnapMessage.class.getSimpleName();

        public ArrayList<User> users;

        public UserListMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            if (jo.has("users_list")) {
                users = Utils.parseUserList(jo.getJSONArray("users_list"));
            }
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();

            JSONArray usersArray = new JSONArray();

            for (User user : users) {
                JSONObject userObject = new JSONObject();

                userObject.put("name", user.name);
                userObject.put("phone", user.phone);

                usersArray.put(userObject);
            }

            jo.put("users_list", usersArray);
            return jo;
        }
    }

    public class VoiceMessage extends Notification {

        public String username;
        public byte[] audio;

        public VoiceMessage(byte[] audio, String user){
            super(MessageType.WatchVoicemail);
            this.username = user;
            this.audio = audio;
            this.severity = Severity.GREEN ;
        }

        public VoiceMessage(String jStr) throws JSONException {
            super(jStr);
            extractFromJson(new JSONObject(jStr));
            this.severity = Severity.GREEN ;
        }

        @Override
        protected void extractFromJson(JSONObject jo) throws JSONException {
            if (jo.has("username")) {
                username = jo.getString("username");
            }

            if (jo.has("audio")) {
                audio = Base64.decodeBase64(jo.getString("audio"));
            }
        }

        @Override
        public JSONObject toJson() throws JSONException {
            JSONObject jo = super.toJson();
            jo.put("username", username);
            jo.put("audio", Base64.encodeBase64String(audio));
            return jo;
        }
    }

    //*************************************************************************

    /**
     * A status message sent from the smart watch to the server
     */
    public class WatchStatusMessage extends Message {
        private final String TAG = WatchStatusMessage.class.getSimpleName();

        public WatchStatusMessage() {
            super(MessageType.WatchStatus);
        }

        public String version;
        public long motionStatus;
        public float statusInterval;
        public String batteryStatus;
        public String wifiStrength;
        public String deviceManufacturer;
        public String deviceModel;
        public HashMap<String, Integer> pageTransitions = new HashMap<>();
        public int systemStatus;
        public boolean bluetoothEnabled;
        public boolean isWatchOnBody;
        public int ip;
        public boolean isCharging;
        public String lifeguardName;
        public String lifeguardPhone;

        @Override
        protected void dataToJson(JSONObject jo) throws JSONException {
            jo.put("version", version);
            jo.put("motion", motionStatus);
            jo.put("status_interval", statusInterval);
            jo.put("battery", batteryStatus);
            jo.put("wifi", wifiStrength);
            jo.put("device_manufacturer", deviceManufacturer);
            jo.put("device_model", deviceModel);
            jo.put("system_status", systemStatus);
            jo.put("bluetooth_enabled", bluetoothEnabled);
            jo.put("is_watch_on_body", isWatchOnBody);
            jo.put("ip", Formatter.formatIpAddress(ip));
            jo.put("page_transitions", pageTransitionsToJSON());
            jo.put("is_charging", isCharging);
            jo.put("lifeguard_name", lifeguardName);
            jo.put("lifeguard_phone", lifeguardPhone);
        }

        private JSONObject pageTransitionsToJSON() throws JSONException {
            JSONObject jo = new JSONObject();
            for (Map.Entry<String, Integer> entry : pageTransitions.entrySet()) {
                jo.put(entry.getKey(), entry.getValue());
            }
            return jo;
        }
        @Override
        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            JSONObject joData = new JSONObject();
            dataToJson(joData);
            jo.put("data", joData);
            return jo.toString();
        }

    }

    /**
     * Sent from watch to server
     * Lifeguard response to a notification
     */
    public class AlertFeedbackMessage extends Message {
        private final String TAG = AlertFeedbackMessage.class.getSimpleName();

        public String alertMsgId;
        public long responseTime;
        public boolean response;

        /**
         * Constructor
         *
         * @param notificationId the id of the notification the lifeguard is responding to
         * @param response       The lifeguard's response (0 - false alert, 1 - confirmed alert)
         */
        public AlertFeedbackMessage(String notificationId, long responseTime,
                                    boolean response) {
            super(MessageType.WatchAlertFeedback);
            alertMsgId = notificationId;
            this.responseTime = responseTime;
            this.response = response;
        }

        @Override
        protected void dataToJson(JSONObject jo) throws JSONException {
            jo.put("alert_message_id", alertMsgId);
            jo.put("response_time", responseTime);
            jo.put("response", response);
        }
        @Override
        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            JSONObject joData = new JSONObject();
            dataToJson(joData);
            jo.put("data", joData);
            return jo.toString();
        }
    }



    /**
     * Sent from watch to server
     * Should receive an AckMessage from server upon receipt
     */
    public class ReportMessage extends Message {
        private final String TAG = ReportMessage.class.getSimpleName();

        public int reportType;
        public String reportText;

        public ReportMessage(int reportType, String reportText) {
            super(MessageType.WatchReport);
            this.reportType = reportType;
            this.reportText = reportText;
        }

        @Override
        protected void dataToJson(JSONObject jo) throws JSONException {
            jo.put("note_type", reportType);
            jo.put("report_text", reportText);
        }
        @Override
        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            JSONObject joData = new JSONObject();
            dataToJson(joData);
            jo.put("data", joData);
            return jo.toString();
        }
    }

    /**
     * Sent from watch to server
     * Should receive an AckMessage from server upon receipt
     */
    public class SwimmerCountMessage extends Message {
        private final String TAG = SwimmerCountMessage.class.getSimpleName();

        public int swimmerCount;
        public int currentViewSwimmerCount;

        public SwimmerCountMessage(int swimmerCount, int currentViewSwimmerCount) {
            super(MessageType.WatchSwimmerCount);
            this.swimmerCount = swimmerCount;
            this.currentViewSwimmerCount = currentViewSwimmerCount;
        }

        @Override
        protected void dataToJson(JSONObject jo) throws JSONException {
            jo.put("swimmer_count", swimmerCount);
            jo.put("current_view_swimmer_count", currentViewSwimmerCount);
        }

        @Override
        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            JSONObject joData = new JSONObject();
            dataToJson(joData);
            jo.put("data", joData);
            return jo.toString();
        }

    }

    /**
     * Sent from either the watch or the server
     * Acknowledges the receipt of a message
     */
    public class AckMessage extends Message {
        private final String TAG = AckMessage.class.getSimpleName();

        public String ackMsgID;

        /**
         * Constructor used by the watch to construct outgoing acknowledgements to server
         *
         * @param msgId message id who's receipt is being acknowledged
         */
        public AckMessage(String msgId) {
            super(MessageType.WatchAck);
            ackMsgID = msgId;
        }

        @Override
        protected void dataToJson(JSONObject jo) throws JSONException {
            jo.put("acked_message_id", ackMsgID);
        }

        @Override
        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            JSONObject joData = new JSONObject();
            dataToJson(joData);
            jo.put("data", joData);
            return jo.toString();
        }

    }

    /**
     * Sent from watch to server
     * Should receive an AckMessage from server upon receipt
     */
    public class ErrorMessage extends Message {
        private final String TAG = ErrorMessage.class.getSimpleName();

        public String _errorText;

        public ErrorMessage(String errorText) {
            super(MessageType.WatchError);
            _errorText = errorText == null ? "!@#$% error message !@#$%" : errorText;
        }

        @Override
        protected void dataToJson(JSONObject jo) throws JSONException {
            jo.put("error_message", _errorText);
        }

        @Override
        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            JSONObject joData = new JSONObject();
            dataToJson(joData);
            jo.put("data", joData);
            return jo.toString();
        }
    }

    public class LifeguardNoticedAlert extends Message {
        private final String TAG = LifeguardNoticedAlert.class.getSimpleName();

        public String alertMsgId;

        public LifeguardNoticedAlert(String notificationId) {
            super(MessageType.WatchLifeguardNoticedAlert);
            alertMsgId = notificationId;
        }

        @Override
        protected void dataToJson(JSONObject jo) throws JSONException {
            jo.put("alert_message_id", alertMsgId);
        }

        @Override
        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            JSONObject joData = new JSONObject();
            dataToJson(joData);
            jo.put("data", joData);
            return jo.toString();
        }
    }

    public class GetPoolSnapMessage extends Message {
        private final String TAG = GetPoolSnapMessage.class.getSimpleName();

        public String poolId;
        public String uuid;

        public GetPoolSnapMessage(String poolId, String uuid) {
            super(MessageType.WatchGetSnap);
            this.poolId = poolId;
            this.uuid = uuid;
        }

        @Override
        protected void dataToJson(JSONObject jo) throws JSONException {
            jo.put("pool_id", poolId);
            jo.put("uuid", uuid);
        }
        @Override
        public String toJsonStr() throws JSONException {
            JSONObject jo = toJson();
            JSONObject joData = new JSONObject();
            dataToJson(joData);
            jo.put("data", joData);
            return jo.toString();
        }
    }


    /**
     * Parses a JSON string into a Message object
     *
     * @param jStr the string to be parsed
     * @return a Message object
     */
    public Message getMessageFromJson(String jStr) throws JSONException {
        MessageType msgType = getMsgTypeFromJsonStr(jStr);
        switch (msgType) {
            case CvsInfo:
                return new PoolFullInfoMessage(jStr);

            case CvsAlert:
                return new PoolAlertMessage(jStr);

            case CvsStatus:
                return new PoolSwimmersCountMessage(jStr);

            case WatchOwnership:
                return new OwnershipMessage(jStr);

            case WatchTeamRequest:
                return new TeamRequestMessage(jStr);

            case WatchTeamResponse:
                return new TeamResponseMessage(jStr);

            case WatchSnap:
                return new PoolSnapMessage(jStr);

            case CvsEnableErrorReports:
                return new EnableErrorReports(jStr);

            case CvsSiteName:
                return new SiteNameMessage(jStr);

            case CvsFeatures:
                return new FeaturesMessage(jStr);

            case CvsUserList:
                return new UserListMessage(jStr);

            case WatchVoicemail:
                return new VoiceMessage(jStr);

            case WatchConnected:
                return new WatchConnectedMessage(jStr);


            default:
                Log.e(TAG, "unhandled message type: " + msgType.name());
                return new Message(jStr) {
                    @Override
                    protected void extractFromJson(JSONObject jo) {
                    }
                };
        }
    }

    private static MessageType getMsgTypeFromJsonStr(String jStr)
            throws JSONException {
        JSONObject jo = new JSONObject(jStr);
        return MessageType.getType(jo.getInt("type"));
    }
}
