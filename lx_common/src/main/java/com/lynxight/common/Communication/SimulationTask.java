package com.lynxight.common.Communication;

import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import com.lynxight.common.R;
import com.lynxight.common.data_model.PoolData;
import com.lynxight.common.managers.SiteData;
import com.lynxight.common.utils.SitePreferences;
import com.lynxight.common.utils.Utils;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class SimulationTask extends MQTTClientBase implements Runnable {

    private final int MINIMUM_SWIMMERS = 20;
    private final int MAX_SWIMMERS = 90;
    private final int MAXIMUM_SWIMMER_DIFFERENCE = 3;
    private boolean firstChange = true;
    private ArrayList<Integer> swimmersCounter;

    private MqttCallback myCallback;
    private List<PoolFullInfoMessage> pools = new ArrayList<>();
    private PoolSwimmersCountMessage poolSwimmersCountMessage;
    private PoolAlertMessage poolAlertMessage;
    private int flowStage = 0;
    private Handler handler;

    public SimulationTask(MqttCallback mqttCallback) {
        myCallback = mqttCallback;
        try {
            pools.add((PoolFullInfoMessage) getMessageFromJson("{\"message_id\": \"82722fc7-5c74-4c76-9bae-9093b2780e18\", \"time\": \"2022-08-15 10:23:08.513857\", \"source\": \"eb7102c6-69f1-4559-bc3d-9fed9af06853\", \"type\": 4, \"pool_info\": {\"pool_id\": \"eb7102c6-69f1-4559-bc3d-9fed9af06853\", \"pool_display_name\": \"Sport\", \"topic_alias\": \"Penguins__127_0_0_1\", \"pool_image\": \"iVBORw0KGgoAAAANSUhEUgAAAUoAAAC0CAYAAADyxmXYAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA0ZJREFUeNrs3U9r02AAwOF32q6sjNZZlSLWg5QdVmHFgeDwMNzBr6VfYB/EqxcF/x1kXndQQelh2Ivo9FBHZRPBJNJS7dhAu6VLngdCk7YE+rb80rcLWQgAAAAAAAAAAAAAAAAAAACctJnRjafPXpyLbtqGBSBsr99Z245XCn89EEfyufEBCPej5V68csZYABxOKAGEEuD/FI56wvqTHaMEZN7GSjUsLxR9owQw9QYQSgChBBBKAKEEEEoAoQRAKAGEEkAoAYQSQCgBhBJAKAGEEgChBBBKAKEEEEoAoQQQSgChBBBKAKEEQCgBhBJAKAGEEkAoAYQSQCgBhBIAoQQQSgChBBBKAKEEEEoAoQQQSgCEEkAoAYQSQCgBhBJAKAGEEkAoAYQSAKEEEEoAoQQQSgChBBBKAKEEEEoAhBJAKAGEEkAoAYQSQCgBMqJgCIA01fZ7E93fl9nK6Qhl+edeaPQ/TWx//bOl0C1f8omCDFrdeT3R/T28vHo6QjkXhXLxW3dyR4hSVSiB1PiNEkAoAYQSQCgB0pTa6UH93V74vvv7tIBa/Yp3AjhQt/MmvN96law3mq2w2L6Vj1D2vn4Om48ehB/7e8MX37591ycCGGvF1svHw+13UTDn5itJMzI/9Y5f7CCSgyNGPCAAoz5+6IzdN5iJZj6Uo5E87D4g3yrnL45Pg2dL+Qhlo7n0x3Y5+ip90IAA+Va/2gzXlm4Mt2v1xolPu5M4pxPKVihGR4Vu521yG/84W0zhKAFMv9bNtWRJU2p/9Y6PFPECMO2cRwkglABCCSCUAGlyhXMgVZsXruczlPGl2I/jKsNA9hzHv24w9QYQSgChBBBKAKEEEEoAhBJAKAGEEkAoAYQSQCgBhBJAKAGEEgChBBBKAKEEEEoAoQQQSgChBBBKAIQSQCgBhBJAKAGEEkAoAYQSQCgBEEoAoQQQSgChBBBKAKEEEEoAoQQQSkMAIJQAQgkglABCCSCUAEIJIJQAQgmAUAIIJYBQAgglgFACCCWAUAIIJQBCCSCUAEIJIJQAQgkglABCCZADhaOesLxQNEpA5s0XZv49lBsrVSMImHoDIJQAQgkAAAAAAAAAAAAAAAAATJNfAgwAFhZeAm3YeSIAAAAASUVORK5CYII=\", \"transform_matrix\": [-71.3, 0.0, 322.0, 0.0, 39.5, 17.0, 0.0, 0.0, 1.0]}, \"expiration_time\": \"9999-12-31 23:59:59.999999\"}"));
            pools.add((PoolFullInfoMessage) getMessageFromJson("{\"message_id\": \"82722fc7-5c74-4c76-9bae-9093b2780e18\", \"time\": \"2022-08-15 10:23:08.513857\", \"source\": \"eb710ssd-69f1-4559-bc3d-9fed9af06853\", \"type\": 4, \"pool_info\": {\"pool_id\": \"eb710ssd-69f1-4559-bc3d-9fed9af06853\", \"pool_display_name\": \"Infants\", \"topic_alias\": \"Penguins__127_0_0_1\", \"pool_image\": \"iVBORw0KGgoAAAANSUhEUgAAAUoAAAC0CAYAAADyxmXYAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA0ZJREFUeNrs3U9r02AAwOF32q6sjNZZlSLWg5QdVmHFgeDwMNzBr6VfYB/EqxcF/x1kXndQQelh2Ivo9FBHZRPBJNJS7dhAu6VLngdCk7YE+rb80rcLWQgAAAAAAAAAAAAAAAAAAACctJnRjafPXpyLbtqGBSBsr99Z245XCn89EEfyufEBCPej5V68csZYABxOKAGEEuD/FI56wvqTHaMEZN7GSjUsLxR9owQw9QYQSgChBBBKAKEEEEoAoQRAKAGEEkAoAYQSQCgBhBJAKAGEEgChBBBKAKEEEEoAoQQQSgChBBBKAKEEQCgBhBJAKAGEEkAoAYQSQCgBhBIAoQQQSgChBBBKAKEEEEoAoQQQSgCEEkAoAYQSQCgBhBJAKAGEEkAoAYQSAKEEEEoAoQQQSgChBBBKAKEEEEoAhBJAKAGEEkAoAYQSQCgBMqJgCIA01fZ7E93fl9nK6Qhl+edeaPQ/TWx//bOl0C1f8omCDFrdeT3R/T28vHo6QjkXhXLxW3dyR4hSVSiB1PiNEkAoAYQSQCgB0pTa6UH93V74vvv7tIBa/Yp3AjhQt/MmvN96law3mq2w2L6Vj1D2vn4Om48ehB/7e8MX37591ycCGGvF1svHw+13UTDn5itJMzI/9Y5f7CCSgyNGPCAAoz5+6IzdN5iJZj6Uo5E87D4g3yrnL45Pg2dL+Qhlo7n0x3Y5+ip90IAA+Va/2gzXlm4Mt2v1xolPu5M4pxPKVihGR4Vu521yG/84W0zhKAFMv9bNtWRJU2p/9Y6PFPECMO2cRwkglABCCSCUAGlyhXMgVZsXruczlPGl2I/jKsNA9hzHv24w9QYQSgChBBBKAKEEEEoAhBJAKAGEEkAoAYQSQCgBhBJAKAGEEgChBBBKAKEEEEoAoQQQSgChBBBKAIQSQCgBhBJAKAGEEkAoAYQSQCgBEEoAoQQQSgChBBBKAKEEEEoAoQQQSkMAIJQAQgkglABCCSCUAEIJIJQAQgmAUAIIJYBQAgglgFACCCWAUAIIJQBCCSCUAEIJIJQAQgkglABCCZADhaOesLxQNEpA5s0XZv49lBsrVSMImHoDIJQAQgkAAAAAAAAAAAAAAAAATJNfAgwAFhZeAm3YeSIAAAAASUVORK5CYII=\", \"transform_matrix\": [-71.3, 0.0, 322.0, 0.0, 39.5, 17.0, 0.0, 0.0, 1.0]}, \"expiration_time\": \"9999-12-31 23:59:59.999999\"}"));
            pools.add((PoolFullInfoMessage) getMessageFromJson("{\"message_id\": \"82722fc7-5c74-4c76-9bae-9093b2780e18\", \"time\": \"2022-08-15 10:23:08.513857\", \"source\": \"tlv_olympic_74dfe679-b63b-565f-bd04-93caff13eab3\", \"type\": 4, \"pool_info\": {\"pool_id\": \"tlv_olympic_74dfe679-b63b-565f-bd04-93caff13eab3\", \"pool_display_name\": \"Outdoor\", \"topic_alias\": \"Penguins__127_0_0_1\", \"pool_image\": \"iVBORw0KGgoAAAANSUhEUgAAAUoAAAC0CAYAAADyxmXYAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA0ZJREFUeNrs3U9r02AAwOF32q6sjNZZlSLWg5QdVmHFgeDwMNzBr6VfYB/EqxcF/x1kXndQQelh2Ivo9FBHZRPBJNJS7dhAu6VLngdCk7YE+rb80rcLWQgAAAAAAAAAAAAAAAAAAACctJnRjafPXpyLbtqGBSBsr99Z245XCn89EEfyufEBCPej5V68csZYABxOKAGEEuD/FI56wvqTHaMEZN7GSjUsLxR9owQw9QYQSgChBBBKAKEEEEoAoQRAKAGEEkAoAYQSQCgBhBJAKAGEEgChBBBKAKEEEEoAoQQQSgChBBBKAKEEQCgBhBJAKAGEEkAoAYQSQCgBhBIAoQQQSgChBBBKAKEEEEoAoQQQSgCEEkAoAYQSQCgBhBJAKAGEEkAoAYQSAKEEEEoAoQQQSgChBBBKAKEEEEoAhBJAKAGEEkAoAYQSQCgBMqJgCIA01fZ7E93fl9nK6Qhl+edeaPQ/TWx//bOl0C1f8omCDFrdeT3R/T28vHo6QjkXhXLxW3dyR4hSVSiB1PiNEkAoAYQSQCgB0pTa6UH93V74vvv7tIBa/Yp3AjhQt/MmvN96law3mq2w2L6Vj1D2vn4Om48ehB/7e8MX37591ycCGGvF1svHw+13UTDn5itJMzI/9Y5f7CCSgyNGPCAAoz5+6IzdN5iJZj6Uo5E87D4g3yrnL45Pg2dL+Qhlo7n0x3Y5+ip90IAA+Va/2gzXlm4Mt2v1xolPu5M4pxPKVihGR4Vu521yG/84W0zhKAFMv9bNtWRJU2p/9Y6PFPECMO2cRwkglABCCSCUAGlyhXMgVZsXruczlPGl2I/jKsNA9hzHv24w9QYQSgChBBBKAKEEEEoAhBJAKAGEEkAoAYQSQCgBhBJAKAGEEgChBBBKAKEEEEoAoQQQSgChBBBKAIQSQCgBhBJAKAGEEkAoAYQSQCgBEEoAoQQQSgChBBBKAKEEEEoAoQQQSkMAIJQAQgkglABCCSCUAEIJIJQAQgmAUAIIJYBQAgglgFACCCWAUAIIJQBCCSCUAEIJIJQAQgkglABCCZADhaOesLxQNEpA5s0XZv49lBsrVSMImHoDIJQAQgkAAAAAAAAAAAAAAAAATJNfAgwAFhZeAm3YeSIAAAAASUVORK5CYII=\", \"transform_matrix\": [-71.3, 0.0, 322.0, 0.0, 39.5, 17.0, 0.0, 0.0, 1.0]}, \"expiration_time\": \"9999-12-31 23:59:59.999999\"}"));
            this.swimmersCounter = new ArrayList<Integer>();
            poolSwimmersCountMessage = new PoolSwimmersCountMessage("{\"swimmer_count\": 12, \"message_id\": \"d82aacf6-7007-44bd-b9ad-f23407e03e27\", \"time\": \"2022-08-17 15:25:58.006038\", \"source\": \"eb7102c6-69f1-4559-bc3d-9fed9af06853\", \"type\": 1, \"expiration_time\": \"9999-12-31 23:59:59.999999\"}");
            constructPoolAlert();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void randomizePoolCoordinates(){
        poolAlertMessage.worldCoordinates = new ArrayList<Float>();
        poolAlertMessage.worldCoordinates.add((float)getRandomNumber(0, 5));
        poolAlertMessage.worldCoordinates.add((float)getRandomNumber(0, 5));
    }

    private void constructPoolAlert() throws JSONException {
        poolAlertMessage = new PoolAlertMessage("{\"message_id\": \"56815773f\", \"time\": \"2022-08-17 15:38:04.429160\", \"source\": \"eb710\", \"type\": 2, \"expiration_time\": \"9999-12-31 23:59:59.999999\", \"alert_type\": \"idr\", \"display_color\": \"RED\", \"location\": [35.0, 147.5], \"world_coordinates\": [2.0, 2.0], \"image_list\": [{\"timestamp\": \"14:07:40\", \"image\": \"jjjj\"}, {\"timestamp\": \"14:07:45\", \"image\": \"\"}, {\"timestamp\": \"14:07:50\", \"image\": \"\"}] }");
        poolAlertMessage.coordinateX = 108;
        poolAlertMessage.coordinateY = 135;
        poolAlertMessage.worldCoordinates = new ArrayList<Float>();
        poolAlertMessage.worldCoordinates.add(3.0f);
        poolAlertMessage.worldCoordinates.add(3.0f);
        poolAlertMessage.images = new ArrayList<>();
        prepareWarningMessage();
    }

    private void prepareWarningMessage(){
        poolAlertMessage.alertType = "warning";
        poolAlertMessage.severity = Severity.RED;
        poolAlertMessage.images.clear();
        poolAlertMessage.images.add(new Pair<>("15:37:54",Utils.resourceImageToBase64(context,R.drawable.demo_warning_0)));
        poolAlertMessage.images.add(new Pair<>("15:37:59",Utils.resourceImageToBase64(context,R.drawable.demo_warning_1)));
        poolAlertMessage.images.add(new Pair<>("15:38:04",Utils.resourceImageToBase64(context,R.drawable.demo_warning_2)));
        randomizePoolCoordinates();
    }

    private void prepareOvercrowdingMessage(){
        poolAlertMessage.alertType = "overcrowding";
        poolAlertMessage.severity = Severity.BLUE;
        poolAlertMessage.images.clear();
        poolAlertMessage.images.add(new Pair<>("15:37:54",Utils.resourceImageToBase64(context,R.drawable.overcrowd_0)));
        randomizePoolCoordinates();
    }

    private void prepareIdrMessage(){
        poolAlertMessage.alertType = "idr";
        poolAlertMessage.severity = Severity.YELLOW;
        poolAlertMessage.images.clear();
        poolAlertMessage.images.add(new Pair<>("15:37:54",Utils.resourceImageToBase64(context,R.drawable.idr_image0)));
        poolAlertMessage.images.add(new Pair<>("15:37:59",Utils.resourceImageToBase64(context,R.drawable.idr_image1)));
        poolAlertMessage.images.add(new Pair<>("15:38:04",Utils.resourceImageToBase64(context,R.drawable.idr_image2)));
        randomizePoolCoordinates();
    }


    public void startSimulation() {
        handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(this, 500);
    }

    public void stopSimulation() {
        if (handler == null)
            return;
        handler.removeCallbacks(this);
    }


    @Override
    public void run() {
//        poolManager.publishPoolDataChanged(pools.get(0).poolFullInfo.poolId);
        // Run every 1.5 second. Generate a message and call myCallback
        if (flowStage < pools.size()) {
            sendPoolFullInfo();
        } else {
            sendSwimmerCount();
        }
        ++flowStage;
        handler.postDelayed(this, 1500);
    }

    private void sendSwimmerCount() {
        mockSwimmersCount();
        sendInboundMessage(poolSwimmersCountMessage);
    }

    private void mockSwimmersCount() {
        PoolFullInfoMessage pool = pools.get(flowStage % pools.size());
        mockIdAndTime(poolSwimmersCountMessage);
        poolSwimmersCountMessage.msgSource = pool.msgSource;

        verifyCachedSwimmerCount();

        poolSwimmersCountMessage.swimmerCount = swimmersCounter.get(flowStage % pools.size());

        swimmersCounter.set(flowStage % pools.size(), getRandomNumber(Math.max(poolSwimmersCountMessage.swimmerCount - MAXIMUM_SWIMMER_DIFFERENCE, 0),
                Math.min(poolSwimmersCountMessage.swimmerCount + MAXIMUM_SWIMMER_DIFFERENCE, MAX_SWIMMERS)));

        poolSwimmersCountMessage.swimmerCount = swimmersCounter.get(flowStage % pools.size());
    }

    private void verifyCachedSwimmerCount() {
        if (firstChange)
        {
            firstChange = false;
            Map<String, String> swimmerMap = SitePreferences.getCachedSwimmersCount();
            int randomNumber;

            for (int i = 0; i < pools.size(); i++) {
                randomNumber = getRandomNumber(MINIMUM_SWIMMERS, MAX_SWIMMERS);
                swimmersCounter.add(randomNumber);
            }

            SitePreferences.saveSwimmerCountMap(swimmerMap);
        }
    }

    private void sendPoolFullInfo() {
        PoolFullInfoMessage message = pools.get(flowStage % pools.size());

        mockIdAndTime(message);
        sendInboundMessage(message);
    }

    private void mockIdAndTime(Message message) {
        message.msgID = "82722fc7-5c74-" + flowStage;
        android.text.format.DateFormat df = new android.text.format.DateFormat();
        message.msgTime = (String) df.format("yyyy-MM-dd hh:mm:ss a", new java.util.Date());  //"2022-08-17 15:25:58.006038\"
    }

    public int getRandomNumber(int min, int max) {
        return (int) ((Math.random() * (max - min)) + min);
    }

    private void sendInboundMessage(Message message) {
        MqttMessage mqttMessage = new MqttMessage();
        try {
            mqttMessage.setPayload(message.toJson().toString().getBytes());
            myCallback.messageArrived("topic", mqttMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int alertCycleIndex = 0;
    public void sendAlert() {
        switch(alertCycleIndex++ % 3){
            case 0:
                prepareWarningMessage();
                break;
            case 1:
                prepareIdrMessage();
                break;
            case 2:
            default:
                prepareOvercrowdingMessage();
        }
        
        mockIdAndTime(poolAlertMessage);
        PoolData poolData = SiteData.getInstance().getSelectedPoolData();
        poolAlertMessage.msgSource = poolData != null ? poolData.poolId : pools.get(flowStage % pools.size()).msgSource;
        sendInboundMessage(poolAlertMessage);
    }
}



