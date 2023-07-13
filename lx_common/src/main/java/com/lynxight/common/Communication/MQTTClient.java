package com.lynxight.common.Communication;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static com.lynxight.common.utils.Constants.ACTION_EXT_SET_SERVER_IP;
import static com.lynxight.common.utils.Constants.TIMER_TICK_INTERVAL;
import static org.eclipse.paho.client.mqttv3.MqttException.REASON_CODE_SERVER_CONNECT_ERROR;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.util.Log;

import com.bugsnag.android.Bugsnag;
import com.lynxight.common.data_model.Event;
import com.lynxight.common.data_model.PoolData;
import com.lynxight.common.managers.SiteData;
import com.lynxight.common.managers.SystemResourceManager;
import com.lynxight.common.utils.ConfigPreferences;
import com.lynxight.common.utils.SitePreferences;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONException;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Queue;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

/**
 * An instance of a MQTTClient represents a communicator to the Lynxight server
 */
@Singleton
public class MQTTClient extends MQTTClientBase {
    final String TAG = MQTTClient.class.getSimpleName();

    public static final String SECURE_SERVER_PORT = "8883";
    public static final String ASCII_SERVER_PORT = "1883";
    public static final String ASCII_TRANSPORT_PROTOCOL = "tcp";
    public static final String SECURE_TRANSPORT_PROTOCOL = "ssl";
    public static final String POOL_MAIN_TOPIC_ALIAS = "cvs_messages";
    public static final String LIFEGUARD_MAIN_TOPIC_ALIAS = "lifeguard_messages";

    public static final String ROOT_TOPIC = "lynxight/";
    public static final String MAIN_TOPIC = ROOT_TOPIC + "main/";

    public static final String LIFEGUARD_MAIN_TOPIC_SUFFIX = "/" + LIFEGUARD_MAIN_TOPIC_ALIAS;

    public static final String POOL_SWIMMERS_COUNT_TOPIC_SUFFIX =
            "/" + POOL_MAIN_TOPIC_ALIAS + "/status";

    public static final String POOL_ALERT_TOPIC_SUFFIX =
            "/" + POOL_MAIN_TOPIC_ALIAS + "/alert";
    /**
     * QOS
     */
    public static final int QOS_0 = 0;
    //    public static final int QOS_1 = 1;
    public static final int QOS_2 = 2;
    public static final int ALERT_LAST_MSG_IDS_MAX = 5;
    public static final int CVS_DOWN_TIMEOUT = 30 * TIMER_TICK_INTERVAL;
    public final static String ACTION_MQTT_STOP = "action.mqtt.stop";
    


    @Inject
    Context context;
    @Inject
    SiteData siteData;

    private BroadcastReceiver mqttStopReceiver;
    private BroadcastReceiver extSetServerIpReceiver;

    private boolean secure = true;

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }




    public void generateDemoAlert() {
        if(ConfigPreferences.isDemoMode() && mqttAndroidClient instanceof DemoAndroidClient){
            ((DemoAndroidClient) mqttAndroidClient).generateAlert();
        }
    }

    public enum MqttConnectionStatus {
        INIT_DISCONNECTED,  // initial disconnected state (normal)
        DISCONNECTED,       // disconnected from wifi (error)
        NO_BROKER,           // disconnected from MQTT broker (error)
        CONNECTED,           // connected with pools (normal)
        NO_DATA             // connected to broker but no data received (error)
    }

    private ServerInfo serverInfo;
    private final PublishSubject<PoolFullInfoMessage> poolFullInfoSubject =
            PublishSubject.create();
    private final PublishSubject<PoolSwimmersCountMessage> poolSwimmersCountSubject =
            PublishSubject.create();
    private final PublishSubject<MqttConnectionStatusInfo> clientConnectionStatusSubject =
            PublishSubject.create();
    private final PublishSubject<String> connectedToCvsSubject =
            PublishSubject.create();
    private final PublishSubject<Object> serverTransition =
            PublishSubject.create();
    private final PublishSubject<OwnershipMessage> ownershipSubject =
            PublishSubject.create();
    private final PublishSubject<Event<TeamResponseMessage>> internalResponseSubject =
            PublishSubject.create();
    private final PublishSubject<PoolSnapMessage> poolSnapImageSubject =
            PublishSubject.create();


    public Observable<PoolFullInfoMessage> onPoolFullInfo() { return poolFullInfoSubject; }
    public Observable<PoolSwimmersCountMessage> onPoolSwimmerCount() { return poolSwimmersCountSubject; }
    public Observable<MqttConnectionStatusInfo> onClientConnectionStatus() {
        return clientConnectionStatusSubject;
    }

    public Observable<Event<TeamResponseMessage>> onTeamResponse() {
        return internalResponseSubject;
    }


    public Observable<PoolSnapMessage> onPoolSnap() {
        return poolSnapImageSubject;
    }
    public Observable<OwnershipMessage> onOwnership() {
        return ownershipSubject;
    }
    public Observable<Object> onServerTransition() {
        return serverTransition;
    }
    private void publishServerTransition() {
        serverTransition.onNext(new Object());
    }
    private void publishWatchOwnership(final OwnershipMessage message) {
        ownershipSubject.onNext(message);
    }
    private void publishPoolFullInfo(final PoolFullInfoMessage message) {
        poolFullInfoSubject.onNext(message);
    }
    private void publishPoolSnap(final PoolSnapMessage message) {
        poolSnapImageSubject.onNext(message);
    }
    private void publishInternalResponse(final TeamResponseMessage message) {
        internalResponseSubject.onNext(new Event<>(message));
    }



    private void publishSwimmersCount(final PoolSwimmersCountMessage message) {
        poolSwimmersCountSubject.onNext(message);
    }

    private void publishClientConnectionStatus(final MqttConnectionStatusInfo status) {
        clientConnectionStatusSubject.onNext(status);
    }
    private void publishConnectedToCvs(final String cvsId) {
        connectedToCvsSubject.onNext(cvsId);
    }

    private final MqttConnectionStatusInfo _currentConnectionStatus =
            new MqttConnectionStatusInfo(
                    MqttConnectionStatus.DISCONNECTED,
                    MqttConnectionStatus.DISCONNECTED);

    private final MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    private final Queue<String> lastAlertMsgIds =
            new CircularFifoQueue<>(ALERT_LAST_MSG_IDS_MAX);

    private MqttAndroidClient mqttAndroidClient;
    private PoolData connectedPoolData;

    private boolean _isInit = false;

    public boolean getIsInit() { return _isInit; }

    private final CountDownTimer mqttBrokerDownTimer =
            new CountDownTimer(CVS_DOWN_TIMEOUT, TIMER_TICK_INTERVAL) {
        public void onTick(long millisUntilFinished) {}

        public void onFinish() {
            if (mqttAndroidClient.isConnected()) {
                Log.i(TAG, ">>> mqttBrokerDownTimer TIMEOUT (mqttAndroidClient CONNECTED)");
                setConnectionStatus(MqttConnectionStatus.NO_DATA);
            } else {
                Log.i(TAG, ">>> mqttBrokerDownTimer TIMEOUT (mqttAndroidClient DISCONNECTED)");
                setConnectionStatus(MqttConnectionStatus.NO_BROKER);
                mqttBrokerDownTimer.start();
            }
        }
    };

    private final CountDownTimer disconnectedTimer =
            new CountDownTimer(CVS_DOWN_TIMEOUT / 3, TIMER_TICK_INTERVAL) {
        public void onTick(long millisUntilFinished) {}

        public void onFinish() {
            try {
                if (mqttAndroidClient == null || !mqttAndroidClient.isConnected()) {
                    NetworkInfo info = ((ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE)).getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    setConnectionStatus(info.isConnected() ? MqttConnectionStatus.NO_BROKER : MqttConnectionStatus.DISCONNECTED);
                    disconnectedTimer.start();
                }
            } catch (IllegalArgumentException ignored) {}
        }
    };

    public static class MqttConnectionStatusInfo {
        public MqttConnectionStatus connectionStatus;
        public MqttConnectionStatus prevConnectionStatus;

        public MqttConnectionStatusInfo(MqttConnectionStatus status,
                                        MqttConnectionStatus prevStatus)
        {
            this.connectionStatus = status;
            this.prevConnectionStatus = prevStatus;
        }

        public void updateStatus(MqttConnectionStatus status) {
            prevConnectionStatus = connectionStatus;
            connectionStatus = status;
        }
    }

    public static class ServerInfo {
        private final String serverIp;


        private final String serverPort;
        private final String transportProtocol;
        private final String cvsMainTopicAlias;
        private final String lifeguardMainTopicAlias;

        public ServerInfo(String serverIp, String serverPort, String transportProtocol)
        {
            this.serverIp = serverIp;
            this.serverPort = serverPort;
            this.transportProtocol = transportProtocol;
            this.cvsMainTopicAlias = "cvs_messages";
            this.lifeguardMainTopicAlias = "lifeguard_messages";
        }

        public String getServerIp() { return serverIp; }

        public String getServerURI() {
            return transportProtocol + "://" + serverIp + ":" + serverPort;
        }

        public String getCvsMainTopic() {
            return MAIN_TOPIC + cvsMainTopicAlias;
        }


        public String getServerPort() {
            return serverPort;
        }
    }


    private class MQTTConnectionListener implements IMqttActionListener {
        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            setConnectionStatus(MqttConnectionStatus.NO_DATA);
            startMqttCountdown();
            DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
            disconnectedBufferOptions.setBufferEnabled(true);
            disconnectedBufferOptions.setBufferSize(100);
            disconnectedBufferOptions.setPersistBuffer(false);
            disconnectedBufferOptions.setDeleteOldestMessages(false);
            mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
            subscribeToMainTopics();
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            Log.i(TAG, "failed to connect to: " + serverInfo.getServerURI());
            setConnectionStatus(MqttConnectionStatus.NO_BROKER);
            if((exception instanceof  MqttException) && ((MqttException)exception).getReasonCode() == MqttException.REASON_CODE_SERVER_CONNECT_ERROR){
                setSecure(!isSecure());
            }
            try {
                mqttAndroidClient.disconnect();
            } catch (MqttException e) {
                Bugsnag.notify(e);
            }
            catch (Exception ignored) {}
            startDisconnection();
            if (mqttBrokerDownTimer != null){
                mqttBrokerDownTimer.cancel();
            }
            _isInit = false;
        }
    }

    private void registerBroadcastReceivers() {
        Log.i(TAG, "registerBroadcastReceivers");
        if(ConfigPreferences.isDemoMode() || mqttStopReceiver != null)
            return;
        try {
            mqttStopReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    stop();
                }
            };
            context.registerReceiver(mqttStopReceiver,
                    new IntentFilter(ACTION_MQTT_STOP));

            extSetServerIpReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    restartClient();
                }
            };
            context.registerReceiver(extSetServerIpReceiver,
                    new IntentFilter(ACTION_EXT_SET_SERVER_IP));
        } catch (Exception e) {
            Log.e(TAG, "Broadcast receivers exception: " + e.toString());
        }
    }

    public static SSLSocketFactory getSingleSocketFactory() throws Exception {
        @SuppressLint("CustomX509TrustManager") TrustManager [] trustAllCerts = new TrustManager[] {new X509ExtendedTrustManager() {
            @Override
            public void checkClientTrusted (X509Certificate [] chain, String authType, Socket socket) {

            }

            @Override
            public void checkServerTrusted (X509Certificate [] chain, String authType, Socket socket) {

            }

            @Override
            public void checkClientTrusted (X509Certificate [] chain, String authType, SSLEngine engine) {

            }

            @Override
            public void checkServerTrusted (X509Certificate [] chain, String authType, SSLEngine engine) {

            }

            @Override
            public X509Certificate [] getAcceptedIssuers () {
                return null;
            }

            @Override
            public void checkClientTrusted (X509Certificate [] certs, String authType) {
            }

            @Override
            public void checkServerTrusted (X509Certificate [] certs, String authType) {
            }

        }};

        SSLContext sc = SSLContext.getInstance("TLSv1.2");
        sc.init (null, trustAllCerts, new java.security.SecureRandom());

        return sc.getSocketFactory();
    }

    private void unregisterBroadcastReceivers() {
        if(mqttStopReceiver == null)
            return;
        context.unregisterReceiver(mqttStopReceiver);
        context.unregisterReceiver(extSetServerIpReceiver);
        mqttStopReceiver = extSetServerIpReceiver = null;
    }

    @Inject
    public MQTTClient() {
        super();
    }

    public void init() {
        Log.i(TAG, "init()");
        _isInit = true;
        connectedPoolData = SitePreferences.loadLastConnectedCvs();
        setServerIp(SitePreferences.loadServerIp(), isSecure());
        MemoryPersistence persistence = new MemoryPersistence();
        if(ConfigPreferences.isDemoMode()){
            mqttAndroidClient = new DemoAndroidClient(context, serverInfo.getServerURI(), ConfigPreferences.deviceId(), persistence);
        } else {
            mqttAndroidClient = new MqttAndroidClient(context, serverInfo.getServerURI(), randomUUID(), persistence);
            registerBroadcastReceivers();
            mqttConnectOptions.setAutomaticReconnect(true);
            mqttConnectOptions.setCleanSession(true);
            mqttConnectOptions.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
            mqttConnectOptions.setConnectionTimeout(0); // "0" == infinity, default is 30
            mqttConnectOptions.setKeepAliveInterval(60); // This is the default
            mqttConnectOptions.setMaxInflight(10); // This is default
            mqttConnectOptions.setUserName("watch");
            mqttConnectOptions.setPassword("lx_rnd1234".toCharArray());
            if (serverInfo.getServerURI().endsWith(SECURE_SERVER_PORT)) { // Set socket factory only if we're trying to connect on secure port.
                try {
                    mqttConnectOptions.setSocketFactory(getSingleSocketFactory());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else {
                mqttConnectOptions.setSocketFactory(null);
            }
        }
        MqttCallback mqttCallback = new MqttCallback();
        mqttAndroidClient.setCallback(mqttCallback);
        Log.i(TAG, "mqttConnectOptions: " + mqttConnectOptions.toString());
        connectToServer();
    }

    private static String uuidToBase64(String str) {
        UUID uuid = UUID.fromString(str);
        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return Base64.encodeBase64URLSafeString(bb.array());
    }

    public static String randomUUID() { // Note: Some mosquitto servers limit id length to 23. uuidToBase64 shortens uuid to 22 characters.
        return uuidToBase64(UUID.randomUUID().toString());
    }


    public class MqttCallback implements MqttCallbackExtended {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (reconnect) {
                Log.i(TAG, "reconnected to: " + serverURI);
                startMqttCountdown();
                subscribeToMainTopics();  // due to 'Clean' session, must re-subscribe
            } else {
                Log.i(TAG, "connected to: " + serverURI);
            }
            setConnectionStatus(MqttConnectionStatus.NO_DATA);
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.i(TAG, "connection to server was lost");
            startDisconnection();
            if (mqttBrokerDownTimer != null){
                mqttBrokerDownTimer.cancel();
            }
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            System.gc();
            startMqttCountdown();
            if (!_currentConnectionStatus.connectionStatus
                    .equals(MqttConnectionStatus.CONNECTED))
            {
                setConnectionStatus(MqttConnectionStatus.CONNECTED);
            }
            handleReceivedMessage(new String(message.getPayload(), StandardCharsets.UTF_8));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {}
    }





    public void stop() {
        try {
            Log.i(TAG, "stopping MQTTClient...");
            unregisterBroadcastReceivers();
            if (mqttBrokerDownTimer != null){
                mqttBrokerDownTimer.cancel();
            }
            disconnectFromServer();
        } catch(MqttException | RuntimeException e) {
            Log.e(TAG, "disconnection from server failed: " + e.toString());
        }
    }

    public ServerInfo getServerInfo() { return serverInfo; }

    public void setServerIp(String serverIp, boolean secure) {
        Log.i(TAG, "setting server IP: " + serverIp);
        serverInfo = new ServerInfo(serverIp,
                secure ? SECURE_SERVER_PORT : ASCII_SERVER_PORT,
                secure ? SECURE_TRANSPORT_PROTOCOL : ASCII_TRANSPORT_PROTOCOL);
    }

    private synchronized void connectToServer() {
        try {
            Log.i(TAG, "connecting to " + serverInfo.getServerURI());
            if (mqttAndroidClient.isConnected())
                return;
            mqttAndroidClient.connect(mqttConnectOptions, null, new MQTTConnectionListener());
        } catch (MqttException e) {
            Log.e(TAG, "connection to server failed: " + e.toString());
            SystemResourceManager.reportException(e);
        }
    }

    public boolean isConnected() {
        return mqttAndroidClient != null && mqttAndroidClient.isConnected();
    }

    public MqttConnectionStatus getConnectionStatus() {
        return _currentConnectionStatus.connectionStatus;
    }

    private void setConnectionStatus(MqttConnectionStatus status) {
        publishClientConnectionStatus(new MqttConnectionStatusInfo(status,
                _currentConnectionStatus.connectionStatus));
        _currentConnectionStatus.updateStatus(status);
    }

    public boolean connectToCvs(PoolData cvsData) {
        if(!getIsInit())
            return false;
        Log.i(TAG, "connecting to CVS: " + cvsData.poolId);
        if(connectedPoolData != null)
            unsubscribeFromCvsAlerts (connectedPoolData.topicAlias);
        connectedPoolData = cvsData; // _cvsDataMap.get(poolId);
        SitePreferences.saveLastConnectedCvs(connectedPoolData);
        return subscribeToCvsAlerts(connectedPoolData.topicAlias);
    }

    public boolean disconnectFromServer() throws MqttException {
        if (mqttAndroidClient.isConnected()) {
            Log.i(TAG, "disconnecting from broker...");
            mqttAndroidClient.unregisterResources();
            mqttAndroidClient.clearAbortBroadcast();
            mqttAndroidClient.disconnect();
            mqttAndroidClient.close();
            unregisterBroadcastReceivers();
            return true;
        }
        return false;
    }

    public void restartClient() {
        try {
            if (disconnectFromServer()) {
                setConnectionStatus(MqttConnectionStatus.NO_BROKER);
            }
        } catch (MqttException | RuntimeException e) {
            Log.e(TAG, "disconnection from broker failed: " + e.toString());
            e.printStackTrace();
        }
        siteData.clearPools();
        SitePreferences.flushCache();
        publishServerTransition();
        init();
    }

    private void startMqttCountdown() {
        if (mqttBrokerDownTimer != null) {
            mqttBrokerDownTimer.cancel();
            mqttBrokerDownTimer.start();
        }
    }

    private void startDisconnection() {
        if (disconnectedTimer != null) {
            disconnectedTimer.cancel();
            disconnectedTimer.start();
        }
    }

    private void updateFeaturesStatus(FeaturesMessage inboundMessage) {
        ConfigPreferences.setSnapStatus(inboundMessage.snapEnabled);
    }

    private void subscribeToTopic(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 2, null,
                    new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.i(TAG, "subscribed to " + topic);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.e(TAG, "failed to subscribe to " + topic);
                        }
                    });
        } catch (MqttException e) {
            SystemResourceManager.reportException(e);
        }
    }

    private void unsubscribeFromTopic(String topic) {
        if(!getIsInit())
            return;
        try {
            mqttAndroidClient.unsubscribe(topic, null,
                    new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
                            Log.i(TAG, "unsubscribed from " + topic);
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                            Log.e(TAG, "failed to unsubscribe from " + topic);
                        }
                    });
        } catch (MqttException e) {
            SystemResourceManager.reportException(e);
        }
    }

    private void subscribeToMainTopics() {
        Log.i(TAG, ">>> subscribing to MAIN topics");
        subscribeToTopic(serverInfo.getCvsMainTopic() + "/cvs_info/#");
//        subscribeToTopic(serverInfo.getCvsMainTopic() + "/image");
        subscribeToTopic(serverInfo.getCvsMainTopic() + "/alert");
        subscribeToTopic(ROOT_TOPIC + "ownership");
        subscribeToTopic(ROOT_TOPIC + "internal_messages");
        subscribeToTopic(serverInfo.getCvsMainTopic() + "/configuration/+");
        subscribeToTopic(ROOT_TOPIC + "users");
        subscribeToTopic(ROOT_TOPIC + LIFEGUARD_MAIN_TOPIC_ALIAS + "/voicemail");
        subscribeToTopic(ROOT_TOPIC + LIFEGUARD_MAIN_TOPIC_ALIAS + "/connected_watches");
    }

    private void subscribeToCvsStatus(String topicAlias) {
        try {
            Log.i(TAG, ">>> subscribing to status messages of: " + topicAlias);
            subscribeToTopic(ROOT_TOPIC +
                    topicAlias + POOL_SWIMMERS_COUNT_TOPIC_SUFFIX);
        } catch (Exception e) {
            Log.e(TAG, "failed to subscribe to cvs topics: " + e.getMessage());
        }
    }

    private boolean subscribeToCvsAlerts(String topicAlias) {
        if(!getIsInit())
            return false;
        try {
            Log.i(TAG, ">>> subscribing to alert messages of: " + topicAlias);
            subscribeToTopic(ROOT_TOPIC +
                    topicAlias + POOL_ALERT_TOPIC_SUFFIX);
        } catch (Exception e) {
            Log.e(TAG, "failed to subscribe to cvs topics: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void unsubscribeFromCvsAlerts(String topicAlias) {
        try {
            Log.i(TAG, ">>> unsubscribing from alert messages of: " + topicAlias);
            unsubscribeFromTopic(ROOT_TOPIC +
                    topicAlias + POOL_ALERT_TOPIC_SUFFIX);
        } catch (Exception e) {
            Log.e(TAG, "failed to unsubscribe from cvs topics: " + e.getMessage());
        }
    }



    private boolean sendMessage(String topic, Message msg, int qos) {
        if(ConfigPreferences.isDemoMode())
            return true;
        try {
            final String msgStr = msg.toJsonStr();
            MqttMessage message = new MqttMessage();
            message.setPayload(msgStr.getBytes());
            message.setQos(qos);
            mqttAndroidClient.publish(topic, message);
            Log.i(TAG, ">>> message published: " + msg.msgType.name() + "    " + msgStr);
            if (!mqttAndroidClient.isConnected()) {
                Log.e(TAG, "not connected to broker, message not sent");
                return false;
            }
            return true;
        } catch (JSONException e) {
            Log.e(TAG, "outbound message serialization to JSON failed: " + e.toString());
            return false;
        } catch (MqttException e) {
            Log.e(TAG, "message publish failed: " + e.toString());
            return false;
        }
    }

    public void sendWatchStatus(WatchStatusMessage msg) {
        if (connectedPoolData == null) {
            Log.e(TAG, "[publishStatus] not connected to CVS");
            return;
        }
        String topic = ROOT_TOPIC + connectedPoolData.topicAlias +
                LIFEGUARD_MAIN_TOPIC_SUFFIX + "/status";
        sendMessage(topic, msg, QOS_0);
    }

    private void sendAck(String msgId) {
        if (connectedPoolData == null) {
            Log.e(TAG, "[publishAck] not connected to CVS");
            return;
        }
        String topic = ROOT_TOPIC + connectedPoolData.topicAlias +
                LIFEGUARD_MAIN_TOPIC_SUFFIX + "/ack";
        sendMessage(topic, new AckMessage(msgId), QOS_2);
    }

    public void sendLifeguardNoticedAlert(LifeguardNoticedAlert msg) {
        if (connectedPoolData == null) {
            Log.e(TAG, "[publishLifeguardNoticed] not connected to CVS");
            return;
        }

        String topic = ROOT_TOPIC + connectedPoolData.topicAlias +
                LIFEGUARD_MAIN_TOPIC_SUFFIX + "/lg_noticed";
        sendMessage(topic, msg, QOS_2);
    }

    public boolean sendAlertFeedback(AlertFeedbackMessage msg, String alertSrc) {
        String topic = ROOT_TOPIC + alertSrc +
                LIFEGUARD_MAIN_TOPIC_SUFFIX + "/ack";
        return sendMessage(topic, msg, QOS_2);
    }

    public boolean sendSnap(GetPoolSnapMessage msg) {
        if (connectedPoolData == null) {
            Log.e(TAG, "[sendSnap] not connected to CVS");
            return false;
        }

        String topic = ROOT_TOPIC + "snap";
        subscribeToTopic(msg.uuid);
        sendMessage(topic, msg, QOS_2);
        return true;
    }

    public boolean sendVoiceMessage(VoiceMessage msg) {
        String topic = ROOT_TOPIC + LIFEGUARD_MAIN_TOPIC_ALIAS + "/voicemail";
        sendMessage(topic, msg, QOS_2);
        return true;
    }
    public boolean sendWatchConnected(WatchConnectedMessage msg) {
        String topic = ROOT_TOPIC + LIFEGUARD_MAIN_TOPIC_ALIAS + "/connected_watches";
        sendMessage(topic, msg, QOS_2);
        return true;
    }


    public void sendReport(ReportMessage msg) {
        if (connectedPoolData == null) {
            Log.e(TAG, "[sendReport] not connected to CVS");
            return;
        }
        String topic = ROOT_TOPIC + connectedPoolData.topicAlias +
                LIFEGUARD_MAIN_TOPIC_SUFFIX + "/report";
        sendMessage(topic, msg, QOS_2);

    }

    public void sendTeamRequest(TeamRequestMessage msg) {
        String topic = ROOT_TOPIC + "internal_messages";
        sendMessage(topic, msg, QOS_2);
    }

    public void sendTeamResponse(TeamResponseMessage msg) {
        String topic = ROOT_TOPIC + "internal_messages";
        sendMessage(topic, msg, QOS_2);
    }


    public void sendOwnership(OwnershipMessage msg) {
        String topic = ROOT_TOPIC + "ownership";
        sendMessage(topic, msg, QOS_2);
    }

    public void sendException(ErrorMessage msg) {
        if (connectedPoolData == null) {
            Log.e(TAG, "[sendException] not connected to CVS");
            return;
        }
        String topic = ROOT_TOPIC + connectedPoolData.topicAlias +
                LIFEGUARD_MAIN_TOPIC_SUFFIX + "/error";
        sendMessage(topic, msg, QOS_2);
    }

    private void handleReceivedMessage(String msgStr) {
        try {
            Message inboundMessage = getMessageFromJson(msgStr);

            if (inboundMessage.msgID == null || lastAlertMsgIds.contains(inboundMessage.msgID))
            {
                Log.w(TAG, "repeated alert detected: " +
                        inboundMessage.msgID + ". skipping");
                return;
            }

            if(inboundMessage.msgSource.equals(ConfigPreferences.deviceId())){
                return;
            }

            Log.i(TAG, "<<< message received: " + inboundMessage.msgType.name() + "    " +
                    (inboundMessage.msgType.equals(MessageType.CvsAlert) ?
                            inboundMessage.msgID : msgStr));

            switch (inboundMessage.msgType) {
                    case CvsInfo:
                        Log.i(TAG, ">>>>>>>>>>>>>>>>>>> [CvsInfo]");
                        siteData.handleCvsInfo((PoolFullInfoMessage) inboundMessage);
                        publishPoolFullInfo((PoolFullInfoMessage) inboundMessage);

                        if (connectedPoolData == null) {
                            Log.i(TAG, ">>>>>>>>>>>>>>>>>>> [CvsInfo] _connectedCvs is NULL");
                            // all time first watch connection (no last connected cvs)
                            // connecting to first received cvs
                            connectedPoolData =
                                    ((PoolFullInfoMessage) inboundMessage).poolFullInfo.toPoolData();
                            publishConnectedToCvs(connectedPoolData.poolId);
                            subscribeToCvsStatus(connectedPoolData.topicAlias);
                        }

                        subscribeToCvsStatus(
                                ((PoolFullInfoMessage) inboundMessage).poolFullInfo.topicAlias);
                        break;
                        // todo: handle topic change (via the PoolManager)
                    case CvsStatus:
                        siteData.handleSwimmersCountMessage((PoolSwimmersCountMessage) inboundMessage);
                        publishSwimmersCount(((PoolSwimmersCountMessage) inboundMessage));
                        break;

                    case CvsAlert:
                        sendAck(inboundMessage.msgID);
                        lastAlertMsgIds.add(inboundMessage.msgID);
                        siteData.addNotification((PoolAlertMessage) inboundMessage);
                        break;

                    case WatchAck:
                        break;

                    case WatchOwnership:
                        publishWatchOwnership((OwnershipMessage) inboundMessage);
                        break;

                    case WatchTeamRequest:
                        TeamRequestMessage msg = (TeamRequestMessage) inboundMessage;
                        if(msg.msgSource.equals(ConfigPreferences.deviceId())){
                            return;
                        }
                        siteData.addNotification(msg);
                        break;

                    case WatchTeamResponse:
                        TeamResponseMessage response = (TeamResponseMessage) inboundMessage;
                        if(response.msgSource.equals(ConfigPreferences.deviceId())){
                            return;
                        }
                        publishInternalResponse(response);
                        break;

                    case CvsEnableErrorReports:
                        ConfigPreferences.setBugsnagInUse(((EnableErrorReports) inboundMessage).errorReports);
                        break;

                    case CvsSiteName:
                        SitePreferences.saveSiteName(((SiteNameMessage) inboundMessage).siteName);
                        break;
                    case CvsFeatures:
                        updateFeaturesStatus((FeaturesMessage) inboundMessage);
                        break;

                    case WatchSnap:
                        unsubscribeFromTopic(((PoolSnapMessage) inboundMessage).uuid);
                        publishPoolSnap((PoolSnapMessage) inboundMessage);
                        break;

                    case CvsUserList:
                        ConfigPreferences.saveUserList(((UserListMessage) inboundMessage).users);
                        break;

                    case WatchVoicemail:
                        if (inboundMessage.msgSource.equals(ConfigPreferences.deviceId()))
                            break;

                        siteData.addNotification((VoiceMessage) inboundMessage);
                        break;

                    case WatchConnected:
                        if (inboundMessage.msgSource.equals(ConfigPreferences.deviceId()))
                            break;
                        SitePreferences.updateWatch(inboundMessage.msgSource);
                        break;

                    default:
                        Log.e(TAG, ">>>>>>>>>>>>>>>>>>> unidentified message message type: " +
                                inboundMessage.msgType);
                        break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "json parser error: " + e.getMessage());
            SystemResourceManager.reportException(e);
        } catch (Throwable throwable){
            Log.e(TAG, "ooops... probably backwards compat obsolete exception",throwable);
            // TODO: when all BC messages are removed from CVS, log this exception to BugSnug
        }
    }
}