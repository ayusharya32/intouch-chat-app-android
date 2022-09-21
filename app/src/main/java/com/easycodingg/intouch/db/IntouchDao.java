package com.easycodingg.intouch.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.easycodingg.intouch.models.Chat;
import com.easycodingg.intouch.models.Message;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.models.UserStatus;
import com.easycodingg.intouch.utils.enums.MessageStatus;
import com.easycodingg.intouch.utils.enums.MessageType;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;

@Dao
public interface IntouchDao {

    // Users
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(User user);

    @Query("SELECT * FROM users_table WHERE id = :userId")
    User getUserById(String userId);

    @Query("SELECT * FROM users_table WHERE phone = :phone")
    User getUserByPhone(String phone);


    // Chats
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChat(Chat chat);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertChatList(List<Chat> chatList);

    @Query("SELECT * FROM chats_table")
    Flowable<List<Chat>> getUserChats();

    @Query("SELECT * FROM chats_table WHERE id = :chatId")
    Chat getChatById(String chatId);

    @Query("SELECT * FROM chats_table WHERE isSynced = 0")
    List<Chat> getAllUnsyncedChats();

    @Query("SELECT * FROM chats_table WHERE id = :chatId")
    Flowable<Chat> getChatFlowable(String chatId);


    // Messages
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessage(Message message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessageList(List<Message> messageList);

    @Query("SELECT * FROM messages_table WHERE id = :messageId")
    Message getMessageById(String messageId);

    @Query("SELECT * FROM messages_table WHERE chatId = :chatId ORDER BY sendingTime")
    Flowable<List<Message>> getChatMessages(String chatId);

    @Query("SELECT * FROM messages_table WHERE chatId = :chatId ORDER BY sendingTime DESC LIMIT 1")
    Message getChatLastMessage(String chatId);

    @Query("SELECT * FROM messages_table WHERE chatId = :chatId AND status <> :status AND sentByUserId <> :sentByUserId ORDER BY sendingTime DESC")
    List<Message> getUnreadChatMessages(String chatId, MessageStatus status, String sentByUserId);

    @Query("SELECT * FROM messages_table WHERE status <> :status AND sentByUserId <> :sentByUserId")
    Flowable<List<Message>> getAllUnreadMessages(MessageStatus status, String sentByUserId);

    @Query("SELECT * FROM messages_table WHERE status = :status AND sentByUserId <> :sentByUserId")
    Flowable<List<Message>> getReceivedMessagesByStatus(MessageStatus status, String sentByUserId);

    @Query("SELECT * FROM messages_table WHERE isSynced = 0 AND type = :type")
    List<Message> getAllUnsyncedMessages(MessageType type);

    // User Statuses
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUserStatus(UserStatus status);

    @Query("SELECT * FROM user_statuses_table WHERE userId = :userId")
    UserStatus getUserStatus(String userId);

    @Query("SELECT * FROM user_statuses_table WHERE userId = :userId")
    Flowable<UserStatus> getUserStatusFlowable(String userId);
}
