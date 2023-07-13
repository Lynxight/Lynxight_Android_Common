package com.lynxight.common.data_model;

import android.util.Pair;

import com.lynxight.common.Communication.MQTTClientBase;
import com.lynxight.common.managers.SiteData;

import java.util.ArrayList;

public class AlertData {
    private int dummy =1;
    public String alertSrc;
    public String msgId;
    public String alertType;
    public String poolDisplayName;
    public String msgTime;
    public MQTTClientBase.Severity alertSeverity;
    public int coordinatesX;
    public int coordinatesY;
    public ArrayList<Pair<String, String>> alertImages;
    public ArrayList<Float> worldCoordinates;

    public AlertData(String alertSrc, String msgId, String alertType, String poolDisplayName,
                     String msgTime, int coordinatesX, int coordinatesY,
                     ArrayList<Pair<String, String>> alertImages,
                     ArrayList<Float> worldCoordinates, MQTTClientBase.Severity alertSeverity)
    {
        this.alertSrc = alertSrc;
        this.msgId = msgId;
        this.alertType = alertType;
        this.poolDisplayName = poolDisplayName;
        this.msgTime = msgTime;
        this.coordinatesX = coordinatesX;
        this.coordinatesY = coordinatesY;
        this.alertImages = alertImages;
        this.worldCoordinates = worldCoordinates;
        this.alertSeverity = alertSeverity;
    }

    public AlertData(MQTTClientBase.PoolAlertMessage msg, SiteData site){
        this(
                msg.msgSource ,
                msg.msgID ,
                msg.alertType ,
                (site.getPoolData(msg.msgSource) == null ? msg.msgSource : site.getPoolData(msg.msgSource).poolDisplayName),
                msg.msgTime ,
                msg.coordinateX ,
                msg.coordinateY ,
                msg.images ,
                msg.worldCoordinates ,
                msg.severity);
    }
    public boolean withWorldCoordinates() { return worldCoordinates != null; }

}
