package com.lynxight.common.Communication;

import android.content.Context;
import android.content.Intent;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DemoAndroidClient extends MqttAndroidClient {

    public DemoAndroidClient(Context context, String serverURI, String clientId) {
        super(context, serverURI, clientId);
    }

    public DemoAndroidClient(Context ctx, String serverURI, String clientId, Ack ackType) {
        super(ctx, serverURI, clientId, ackType);
    }

    public DemoAndroidClient(Context ctx, String serverURI, String clientId, MqttClientPersistence persistence) {
        super(ctx, serverURI, clientId, persistence);
    }

    public DemoAndroidClient(Context context, String serverURI, String clientId, MqttClientPersistence persistence, Ack ackType) {
        super(context, serverURI, clientId, persistence, ackType);
    }




    // Broadcast receiver i/f
    @Override
    public void onReceive(Context context, Intent intent) {

    }


    private boolean connectionFlag = true;
    @Override
    public IMqttToken connect() throws MqttException, MqttSecurityException {
        connectionFlag = true;
        return null;
    }

    @Override
    public IMqttToken connect(MqttConnectOptions mqttConnectOptions) throws MqttException, MqttSecurityException {
        connectionFlag = true;
        return null;
    }

    private ScheduledFuture<?> simulationScheduleFeature = null;
    @Override
    public IMqttToken connect(Object o, IMqttActionListener iMqttActionListener) throws MqttException, MqttSecurityException {
        if(simulationScheduleFeature == null){
            simulationScheduleFeature = Executors.newSingleThreadScheduledExecutor().schedule(
                    new Runnable() {
                        @Override
                        public void run() {
                            connectionFlag = true;
                            iMqttActionListener.onSuccess(null);
                        }
                    },
                    1500,
                    TimeUnit.MILLISECONDS);

        }
        return null;
    }

    @Override
    public IMqttToken connect(MqttConnectOptions mqttConnectOptions, Object o, IMqttActionListener iMqttActionListener) throws MqttException, MqttSecurityException {
        return connect(o,iMqttActionListener);
    }

    @Override
    public IMqttToken disconnect() throws MqttException {
        return disconnect(0);
    }

    @Override
    public IMqttToken disconnect(long l) throws MqttException {
        Executors.newSingleThreadScheduledExecutor().schedule(
            ()-> {
                if (simulationScheduleFeature != null){
                    simulationScheduleFeature.cancel(true);
                }
                simulationScheduleFeature = null;
                connectionFlag = false;
            },
            l,
            TimeUnit.MILLISECONDS);
        return null;
    }

    @Override
    public IMqttToken disconnect(Object o, IMqttActionListener iMqttActionListener) throws MqttException {
        return disconnect(0);
    }

    @Override
    public IMqttToken disconnect(long l, Object o, IMqttActionListener iMqttActionListener) throws MqttException {
        return disconnect(0);
    }

    @Override
    public void disconnectForcibly() throws MqttException {
        disconnect(0);
    }

    @Override
    public void disconnectForcibly(long l) throws MqttException {
        disconnect(l);
    }

    @Override
    public void disconnectForcibly(long l, long l1) throws MqttException {
        disconnect(l);
    }

    @Override
    public boolean isConnected() {
        return connectionFlag;
    }

    @Override
    public String getClientId() {
        return "client-id";
    }

    @Override
    public String getServerURI() {
        return  "https://www.facebook.com:443"  ;
    }

    @Override
    public IMqttDeliveryToken publish(String s, byte[] bytes, int i, boolean b) throws MqttException, MqttPersistenceException {
        return null;
    }

    @Override
    public IMqttDeliveryToken publish(String s, byte[] bytes, int i, boolean b, Object o, IMqttActionListener iMqttActionListener) throws MqttException, MqttPersistenceException {
        return null;
    }

    @Override
    public IMqttDeliveryToken publish(String s, MqttMessage mqttMessage) throws MqttException, MqttPersistenceException {
        return null;
    }

    @Override
    public IMqttDeliveryToken publish(String s, MqttMessage mqttMessage, Object o, IMqttActionListener iMqttActionListener) throws MqttException, MqttPersistenceException {
        return null;
    }

    @Override
    public IMqttToken subscribe(String s, int i) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken subscribe(String s, int i, Object o, IMqttActionListener iMqttActionListener) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken subscribe(String[] strings, int[] ints) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken subscribe(String[] strings, int[] ints, Object o, IMqttActionListener iMqttActionListener) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken subscribe(String s, int i, Object o, IMqttActionListener iMqttActionListener, IMqttMessageListener iMqttMessageListener) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken subscribe(String s, int i, IMqttMessageListener iMqttMessageListener) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken subscribe(String[] strings, int[] ints, IMqttMessageListener[] iMqttMessageListeners) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken subscribe(String[] strings, int[] ints, Object o, IMqttActionListener iMqttActionListener, IMqttMessageListener[] iMqttMessageListeners) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken unsubscribe(String s) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken unsubscribe(String[] strings) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken unsubscribe(String s, Object o, IMqttActionListener iMqttActionListener) throws MqttException {
        return null;
    }

    @Override
    public IMqttToken unsubscribe(String[] strings, Object o, IMqttActionListener iMqttActionListener) throws MqttException {
        return null;
    }


    private SimulationTask simulationTask;
    @Override
    public void setCallback(MqttCallback mqttCallback) {
        simulationTask = new SimulationTask(mqttCallback);
        simulationTask.startSimulation();
    }

    @Override
    public IMqttDeliveryToken[] getPendingDeliveryTokens() {
        return new IMqttDeliveryToken[0];
    }

    @Override
    public void setManualAcks(boolean b) {

    }

    @Override
    public void messageArrivedComplete(int i, int i1) throws MqttException {

    }

    @Override
    public void close() {
        if (simulationTask != null)
            simulationTask.stopSimulation();
    }


    public void generateAlert(){
        simulationTask.sendAlert();
    }
}
