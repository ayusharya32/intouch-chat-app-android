package com.easycodingg.intouch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity(tableName = "chats_table")
public class Chat implements Serializable {

    @NonNull
    @PrimaryKey
    public String id;

    public List<String> users;
    public List<Alias> aliases;
    public List<String> typing;
    public boolean isRequestAccepted;
    public boolean isPrivate;
    public Date createdAt;

    @Exclude
    public boolean isSynced;

    @Exclude
    public Date messagesRetrievedTimestamp;

    @Exclude
    @Ignore
    public User otherUser;

    @Exclude
    @Ignore
    public Message lastMessage;

    @Exclude
    @Ignore
    public int unreadMessagesCount = 0;

    @Override
    public String toString() {
        return "Chat{" +
                "id='" + id + '\'' +
                ", users=" + users +
                ", otherUser=" + otherUser +
                ", lastMessage=" + lastMessage +
                '}';
    }
}
