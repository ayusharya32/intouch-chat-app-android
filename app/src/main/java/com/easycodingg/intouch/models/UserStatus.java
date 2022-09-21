package com.easycodingg.intouch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "user_statuses_table")
public class UserStatus {
    @NonNull
    @PrimaryKey
    public String userId;

    public boolean isOnline;
    public Date lastSeenTime;

    @Override
    public String toString() {
        return "UserStatus{" +
                "userId='" + userId + '\'' +
                ", isOnline=" + isOnline +
                ", lastSeenTime=" + lastSeenTime +
                '}';
    }
}
