package com.easycodingg.intouch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity(tableName = "users_table")
public class User implements Serializable {

    @NonNull
    @PrimaryKey
    public String id;

    public String name;
    public String phone;
    public String description;
    public String profileImageUrl;
    public Date dateOfBirth;

    @Ignore
    public List<LoggedInDevice> loggedInDevices;

    @Exclude
    public Date retrievedTimestamp;

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", description='" + description + '\'' +
                ", profileImageUrl='" + profileImageUrl + '\'' +
                ", dateOfBirth=" + dateOfBirth +
                ", loggedInDevices=" + loggedInDevices +
                ", retrievedTimestamp=" + retrievedTimestamp +
                '}';
    }
}
