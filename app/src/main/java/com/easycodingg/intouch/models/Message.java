package com.easycodingg.intouch.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.easycodingg.intouch.utils.enums.FileDownloadStatus;
import com.easycodingg.intouch.utils.enums.FileUploadStatus;
import com.easycodingg.intouch.utils.enums.MessageStatus;
import com.easycodingg.intouch.utils.enums.MessageType;
import com.google.firebase.firestore.Exclude;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Entity(tableName = "messages_table")
public class Message implements Serializable {

    @NonNull
    @PrimaryKey
    public String id;

    public String chatId;
    public List<String> users;
    public String sentByUserId;
    public String content;
    public String fileDownloadUrl;
    public MessageType type;
    public MessageStatus status;
    public Date sendingTime;
    public Date receivingTime;

    @Exclude
    public int uploadingPercentage = 0;

    @Exclude
    public FileUploadStatus fileUploadStatus = FileUploadStatus.PREPARING;

    @Exclude
    public int downloadingPercentage = 0;

    @Exclude
    public FileDownloadStatus fileDownloadStatus = FileDownloadStatus.NOT_DOWNLOADING;

    @Exclude
    public String localFileUriString;

    @Exclude
    public boolean isSynced;

    @Exclude
    public Date orderTimestamp;

    @Override
    public String toString() {
        return "Message{" +
                "id='" + id + '\'' +
                ", chatId='" + chatId + '\'' +
                ", users=" + users +
                ", sentByUserId='" + sentByUserId + '\'' +
                ", content='" + content + '\'' +
                ", fileDownloadUrl='" + fileDownloadUrl + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", sendingTime=" + sendingTime +
                ", receivingTime=" + receivingTime +
                ", uploadingPercentage=" + uploadingPercentage +
                ", fileUploadStatus=" + fileUploadStatus +
                ", downloadingPercentage=" + downloadingPercentage +
                ", fileDownloadStatus=" + fileDownloadStatus +
                ", localFileUriString='" + localFileUriString + '\'' +
                ", isSynced=" + isSynced +
                ", orderTimestamp=" + orderTimestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return uploadingPercentage == message.uploadingPercentage && downloadingPercentage == message.downloadingPercentage && isSynced == message.isSynced && id.equals(message.id) && Objects.equals(chatId, message.chatId) && Objects.equals(users, message.users) && Objects.equals(sentByUserId, message.sentByUserId) && Objects.equals(content, message.content) && Objects.equals(fileDownloadUrl, message.fileDownloadUrl) && type == message.type && status == message.status && Objects.equals(sendingTime, message.sendingTime) && Objects.equals(receivingTime, message.receivingTime) && fileUploadStatus == message.fileUploadStatus && fileDownloadStatus == message.fileDownloadStatus && Objects.equals(localFileUriString, message.localFileUriString) && Objects.equals(orderTimestamp, message.orderTimestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, chatId, users, sentByUserId, content, fileDownloadUrl, type, status, sendingTime, receivingTime, uploadingPercentage, fileUploadStatus, downloadingPercentage, fileDownloadStatus, localFileUriString, isSynced, orderTimestamp);
    }
}
