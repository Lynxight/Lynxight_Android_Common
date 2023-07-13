package com.lynxight.common.utils;

import static com.lynxight.common.utils.Utils.getIPString;

import android.content.Context;
import android.os.CountDownTimer;

import com.lynxight.common.Communication.BroadcastDatagramClient;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ServerAddressReceiver extends BroadcastDatagramClient {

    protected static final int ADDRESS_GETTER_BUFFER_SIZE = 50;
    protected static final int ADDRESS_GETTER_PORT = 8890;
    protected static final int ASK_INTERVAL = 2000;
    protected static final int SOCKET_TIMEOUT = ASK_INTERVAL * 10;
    protected static final char ADDRESS_GETTER_REQUEST = '?';
    protected static final char ADDRESS_GETTER_DEFAULT_ANSWER = '!';

    private final CountDownTimer askForAddressLoop = new CountDownTimer(ASK_INTERVAL, ASK_INTERVAL / 2) {
        public void onTick(long l) {}
        public void onFinish() {
            BroadcastDatagramClient.sendBroadcastPacket(context, String.valueOf(ADDRESS_GETTER_REQUEST), ADDRESS_GETTER_PORT);
            askForAddressLoop.start();
        }
    };

    public ServerAddressReceiver(Context context) {
        super(context, ADDRESS_GETTER_PORT);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("AddressProviderHandler");
        waitConnection();
        askForAddressLoop.start();
        byte[] buf = new byte[ADDRESS_GETTER_BUFFER_SIZE];

        try {
            datagramSocket.setSoTimeout(SOCKET_TIMEOUT);
        } catch (SocketException ignored) {}
        while (!Thread.interrupted()) {
            try {
                Arrays.fill(buf, (byte) 0);
                DatagramPacket packet = new DatagramPacket(buf, ADDRESS_GETTER_BUFFER_SIZE);
                datagramSocket.receive(packet);
                String remoteIP = packet.getAddress().toString().substring(1);
                if (!remoteIP.equals(getIPString(context))) {
                    String data = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                    if (data.charAt(0) != ADDRESS_GETTER_REQUEST) {
                        if (data.charAt(0) == ADDRESS_GETTER_DEFAULT_ANSWER)
                            SitePreferences.saveServerIp(remoteIP);
                        else
                            SitePreferences.saveServerIp(data);
                        ConfigPreferences.setConfigured(true);
                        break;      // done after one receive. still in a loop to prevent crashing
                    }
                }
            }
            catch (SocketTimeoutException ignored) {
                break;
            }
            catch (IOException ignored) {}
        }
        if (askForAddressLoop != null){
            askForAddressLoop.cancel();
        }
        super.run();
    }
}
