package com.easycodingg.intouch.models;

import java.io.Serializable;
import java.util.Date;

public class LoggedInDevice implements Serializable {
    public String deviceId;
    public String deviceName;
    public String fcmToken;
    public Date loggedInTime;
    public Date lastActive;

    public LoggedInDevice() {}

    public LoggedInDevice(String deviceId, String deviceName, String fcmToken, Date loggedInTime) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.fcmToken = fcmToken;
        this.loggedInTime = loggedInTime;
    }

    @Override
    public String toString() {
        return "LoggedInDevice{" +
                "deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", fcmToken='" + fcmToken + '\'' +
                ", loggedInTime=" + loggedInTime +
                ", lastActive=" + lastActive +
                '}';
    }
}
