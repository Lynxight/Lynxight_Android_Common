package com.lynxight.common.managers;

import static com.lynxight.common.utils.Constants.TIMER_TICK_INTERVAL;

import android.os.CountDownTimer;
import android.util.Log;

import com.lynxight.common.Communication.MQTTClientBase;
import com.lynxight.common.data_model.Event;
import com.lynxight.common.data_model.PoolData;
import com.lynxight.common.data_model.PoolImageData;
import com.lynxight.common.utils.Constants;
import com.lynxight.common.utils.SitePreferences;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.PublishSubject;

@Singleton
public class SiteData {

    public static final int NOTIFICATION_BACK_TO_MAIN_TIMER_INTERVAL = 10 * 60 * TIMER_TICK_INTERVAL;

    private final static String TAG = SiteData.class.getSimpleName();

    private final PublishSubject<String> poolTimeoutSubject = PublishSubject.create();
    public Observable<String> onPoolTimeout() { return poolTimeoutSubject; }
    private void publishPoolTimeout(String poolId) {
        poolTimeoutSubject.onNext(poolId);
    }

    private final PublishSubject<Event<Object>> notificationSubject = PublishSubject.create();
    public Observable<Event<Object>> onNotification() { return notificationSubject; }
    private void publishNotification() {
        notificationSubject.onNext(new Event<>(new Object()));
    }

    private final ConcurrentHashMap<String, PoolData> poolDataMap =
            new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, PoolData> getPoolDataMap() {
        if(poolDataMap.isEmpty()){
            Map<String, PoolData> map = SitePreferences.fetchPoolDataMapFromStorage();
            poolDataMap.putAll(map);
        }
        return poolDataMap;
    }

    private final ConcurrentHashMap<String, PoolImageData> poolImageMap = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> swimmerCountMap = new ConcurrentHashMap<>();

    private final Map<String, PoolTimeout> poolTimeoutMap = new HashMap<>();

    private final List<String> poolsInTimeout = new ArrayList<>();

    private PriorityQueue<MQTTClientBase.Notification> notificationPriorityQueue ;

    private void resetPriorityQueue(){
        notificationPriorityQueue= new PriorityQueue<MQTTClientBase.Notification>(new Comparator<MQTTClientBase.Notification>() {
            @Override
            public int compare(MQTTClientBase.Notification o1, MQTTClientBase.Notification o2) {
                return o1.prioritize(o2);
            }
        });

    }



    private PoolData selectedPoolData;


    
    public class PoolTimeout {
        public String poolId;
        public CountDownTimer timeoutTimer;

        public PoolTimeout(String poolId) {
            this.poolId = poolId;
            timeoutTimer =
                    new CountDownTimer(Constants.POOL_DISPLAY_TIMER_INTERVAL,
                            Constants.TIMER_TICK_INTERVAL)
                    {
                        public void onTick(long millisUntilFinished) {}
                        public void onFinish() { handlePoolTimeout(poolId); }
                    };
            timeoutTimer.start();
        }

        public void restart() {
            if (timeoutTimer != null) {
                timeoutTimer.cancel();
                timeoutTimer.start();
            }
        }

        public void stop() {
            if (timeoutTimer != null) {
                timeoutTimer.cancel();
            }
        }
    }

    @Inject
    public SiteData() {
        resetPriorityQueue();
        Map<String,String> cachedMap = SitePreferences.getCachedSwimmersCount();
        if(cachedMap != null){
            swimmerCountMap.putAll(cachedMap);
        }
        instance = this;
    }
    private static SiteData instance ;
    public static SiteData getInstance(){
        if(instance == null){
            instance = new SiteData();
        }
        return instance;
    }

    public void init() {
        // todo: handle demo mode
    }

    public void stop() {
        for (PoolTimeout poolTimeout : poolTimeoutMap.values()) {
            poolTimeout.stop();
        }
    }

    public void clearPools() {
        stop();
        poolDataMap.clear();
        poolImageMap.clear();
        poolTimeoutMap.clear();
        swimmerCountMap.clear();
        poolsInTimeout.clear();
        selectedPoolData = null;
    }

    public void handleSwimmersCountMessage(MQTTClientBase.PoolSwimmersCountMessage poolSwimmersCountMessage) {
        String poolId = poolSwimmersCountMessage.msgSource;
        swimmerCountMap.put(poolId, poolSwimmersCountMessage.swimmerCountText);
        SitePreferences.saveSwimmerCountMap(swimmerCountMap);
        SitePreferences.poolUpdated(poolId);
        poolsInTimeout.remove(poolId);

        PoolTimeout poolTimeout = poolTimeoutMap.get(poolId);
        if (poolTimeout != null) {
            poolTimeout.restart();
        }
    }

    public void handleCvsInfo(MQTTClientBase.PoolFullInfoMessage poolFullInfoMessage) {
        Log.i(TAG, "handling cvs info message for cvs: " + poolFullInfoMessage.msgSource);
        String poolId = poolFullInfoMessage.poolFullInfo.poolId ;
        getPoolDataMap().put(poolId, poolFullInfoMessage.poolFullInfo.toPoolData());
        SitePreferences.saveLatestPoolMap(getPoolDataMap());
        SitePreferences.poolUpdated(poolId);

        poolImageMap.put(poolId, poolFullInfoMessage.poolFullInfo.toPoolImageData());
        if (poolTimeoutMap.containsKey(poolId)) {
            Objects.requireNonNull(poolTimeoutMap.get(poolId)).restart();
        } else {
            poolTimeoutMap.put(poolId,new PoolTimeout(poolId));
        }

        PoolData lastConnectedCvs = SitePreferences.loadLastConnectedCvs();
        if(lastConnectedCvs == null || lastConnectedCvs.poolId.equals(poolId)){
            selectedPoolData = poolFullInfoMessage.poolFullInfo.toPoolData();
        }

    }

    void handlePoolTimeout(String poolId) {
        Log.i(TAG, "pool timeout: " + poolId);
        poolsInTimeout.add(poolId);
        publishPoolTimeout(poolId);
    }

    public Map<String, PoolImageData> getPoolImageMap() {
        return poolImageMap;
    }

    public PoolData getPoolData(String poolId) {
        return getPoolDataMap().getOrDefault(poolId, null);
    }

    public List<PoolData> getAllPoolData() {
        ArrayList<PoolData> tempList = new ArrayList<>(getPoolDataMap().values());
        tempList.sort(Comparator.comparing(o -> o.poolDisplayName));
        return tempList;
    }

    public List<String> getPoolsInTimeout() {
        return new ArrayList<>(poolsInTimeout);
    }

    public PoolImageData getPoolImageData(String poolId) {
        return poolImageMap.getOrDefault(poolId, null);
    }

    public String getSwimmerCount(String poolId) {
        String countStr = swimmerCountMap.getOrDefault(poolId, null);
        return countStr;
    }




    public synchronized void setSelectedPoolData(PoolData poolData) {
        selectedPoolData = poolData;
    }

    public synchronized PoolData getSelectedPoolData() {
        return selectedPoolData;

    }


    public final MQTTClientBase.Notification peekNotification(){
        return notificationPriorityQueue == null ? null : notificationPriorityQueue.peek();
    }

    public synchronized MQTTClientBase.Notification findNotification(String msgID){
        Object notes[] = notificationPriorityQueue.toArray();
        for(Object n : notes){
            if(((MQTTClientBase.Notification)n).msgID.equals(msgID)){
                return (MQTTClientBase.Notification)n;
            }
        }
        return null;
    }

    public synchronized void clearAllNotifications(){
        resetPriorityQueue();
    }
    private void inspectNotificationQueue(){
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date now = new Date();

        Iterator<MQTTClientBase.Notification> itr = notificationPriorityQueue.iterator();
        while(itr.hasNext()){
            Date alertTime = formatter.parse(itr.next().msgTime,new ParsePosition(0));
            if( now.getTime() - alertTime.getTime() > NOTIFICATION_BACK_TO_MAIN_TIMER_INTERVAL ) {
                itr.remove(); ;
            }
        }
    }

    public synchronized void removeNotification(String msgID){
        inspectNotificationQueue();
        if(notificationPriorityQueue.isEmpty())
            return;
        MQTTClientBase.Notification top = peekNotification();
        for(MQTTClientBase.Notification n : notificationPriorityQueue){
            if(n.msgID.equals(msgID)){
                notificationPriorityQueue.remove(n);
                break;
            }
        }
        if(notificationPriorityQueue.isEmpty() || !top.equals(peekNotification())){
            publishNotification();
        }
    }
    public void addNotification(MQTTClientBase.Notification notification){
        hackNotificationTime(notification);
        inspectNotificationQueue();

        if(notificationPriorityQueue.isEmpty()){
            notificationPriorityQueue.add(notification);
            publishNotification();
            return;
        }
        if(notification.severity.compare(MQTTClientBase.Severity.YELLOW) > 0 ){
            return; // if another notification is handled, we do not add notification of low priority
        }
        MQTTClientBase.Notification top = peekNotification();
        notificationPriorityQueue.add(notification);
        if(!top.equals(peekNotification())){
            publishNotification();
        }
    }

    private void hackNotificationTime(MQTTClientBase.Notification notification) {
        //////////////////////////////////////////////////////////////////////////////////
        // This is a hack to avoid issues with watch and server times are not in sync -
        // when we receive an alert we set the watch time on the message
        //////////////////////////////////////////////////////////////////////////////////
        SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date d = new Date();
        notification.msgTime = formatter.format(d);
    }

}