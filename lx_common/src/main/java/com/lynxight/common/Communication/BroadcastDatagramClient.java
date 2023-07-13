package com.lynxight.common.Communication;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.LoudnessEnhancer;
import android.media.audiofx.NoiseSuppressor;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

public class BroadcastDatagramClient implements Runnable {

    protected static final int RATE = 8000;
    protected static final int INTERVAL = 20;
    protected static final int BUFFER_SIZE = INTERVAL * INTERVAL * 4;
    protected static final int UPDATE_INTERVAL = 10 * 1000;
    protected static final int END_TRANSMISSION_TIMEOUT = 1000;

    protected final Context context;
    protected DatagramSocket datagramSocket;
    protected Runnable connectRunnable;
    protected static final ConcurrentHashMap<String, WatchSpeaker> tracks = new ConcurrentHashMap<>();
    protected static final ConcurrentHashMap<String, WatchMember> watchMembers = new ConcurrentHashMap<>();
    protected static final ArrayList<WatchMember> activePoolsTalking = new ArrayList<>();

    protected static class WatchMember {
        public String poolName;
        public long timeout;
        public boolean isMuted;
        public int avgVolume;
        public WatchMember(String pool, long timeout, boolean isMuted) {
            this.poolName = pool;
            this.timeout = timeout;
            this.isMuted = isMuted;
            this.avgVolume = 0;
        }
    }

    protected static class WatchSpeaker {
        public AudioTrack track;
        public LoudnessEnhancer enhancer;
        public WatchSpeaker() {
            track = new AudioTrack(new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setLegacyStreamType(AudioManager.STREAM_VOICE_CALL)
                    .build(),
                    new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setSampleRate(RATE)
                            .build(),
                    BUFFER_SIZE, AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            enhancer = new LoudnessEnhancer(track.getAudioSessionId());
            NoiseSuppressor.create(track.getAudioSessionId());
            AcousticEchoCanceler.create(track.getAudioSessionId());
            enhancer.setTargetGain(1500);
//            enhancer.setEnabled(true);

            track.play();
        }
    }

    protected static final PublishSubject<String> watchTalkingSubject =
            PublishSubject.create();
    public static Observable<String> onWatchTalking() { return watchTalkingSubject; }
    protected void publishWatchTalking(final String poolName) {
        watchTalkingSubject.onNext(poolName);
    }

    public BroadcastDatagramClient(Context context) {
        this.context = context;
        this.connectRunnable = this::initSocket;
    }

    public BroadcastDatagramClient(Context context, int port) {
        this.context = context;
        this.connectRunnable = () -> initSocket(port);
    }

    protected boolean initSocket() {
        try {
            datagramSocket = new DatagramSocket();
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    protected void initSocket(int port) {
        try {
            datagramSocket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            datagramSocket.setBroadcast(true);
        } catch (IOException ignored) {}
    }

    protected void waitConnection() {
        connectRunnable.run();
        while (datagramSocket == null) {
            try {
                Thread.sleep(UPDATE_INTERVAL);
                connectRunnable.run();
            } catch (InterruptedException ignored) {}
        }
    }

    @Override
    public void run() {
        datagramSocket.disconnect();
        datagramSocket.close();
    }

    protected static InetAddress getBroadcastAddress(Context context) throws IOException {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        DhcpInfo dhcp = wifi.getDhcpInfo();

        int broadcast = 0xFFFFFFFF;
        byte[] quads = new byte[4];
        for (int k = 0; k < 4; k++)
            quads[k] = (byte) (broadcast >> (k * 8));
        return InetAddress.getByAddress(quads);
    }

    public static void sendBroadcastPacket(Context context, String data, int port) {
        Thread tempThread = new Thread(() -> {
            try {
                DatagramSocket tempOutSocket = new DatagramSocket();
                DatagramPacket packet = new DatagramPacket(data.getBytes(), data.length(), getBroadcastAddress(context), port);
                tempOutSocket.send(packet);
                tempOutSocket.disconnect();
                tempOutSocket.close();
            } catch (IOException ignored) {}
        });
        tempThread.start();
        try {
            tempThread.join();
        } catch (InterruptedException ignored) {}
    }
}
