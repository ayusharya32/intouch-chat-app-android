package com.easycodingg.intouch.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;

import com.easycodingg.intouch.models.Chat;
import com.easycodingg.intouch.models.LoggedInDevice;
import com.easycodingg.intouch.models.Message;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.models.UserStatus;
import com.easycodingg.intouch.utils.CommonMethods;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.Constants;
import com.easycodingg.intouch.utils.IntouchErrorHandler;
import com.easycodingg.intouch.utils.NetworkUtils;
import com.easycodingg.intouch.utils.enums.FileDownloadStatus;
import com.easycodingg.intouch.utils.enums.MessageStatus;
import com.easycodingg.intouch.utils.enums.MessageType;
import com.easycodingg.intouch.utils.events.OnDocSnapshotListenerTriggered;
import com.easycodingg.intouch.utils.events.OnQuerySnapshotListenerTriggered;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.gson.Gson;

import org.reactivestreams.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class IntouchRepository {
    private static final String TAG = "IntouchRepositoryyyy";

    public static final String INTOUCH_PREFS = "INTOUCH_PREFS";

    // Shared Preferences Keys
    private static final String PREFS_KEY_LOGGED_IN_USER = "KEY_USER";
    private static final String PREFS_KEY_DEVICE_ID = "KEY_DEVICE_ID";
    private static final String PREFS_KEY_FCM_UPDATE_TIMESTAMP = "KEY_FCM_UPDATE_TIMESTAMP";
    private static final String PREFS_KEY_USER_UPDATE_TIMESTAMP = "KEY_USER_UPDATE_TIMESTAMP";
    private static final String PREFS_KEY_CHATS_UPDATE_TIMESTAMP = "KEY_CHATS_UPDATE_TIMESTAMP";

    private static final int HOURS_TO_UPDATE_CHATS = 24;
    private static final int HOURS_TO_UPDATE_FCM_TOKEN = 24;
    private static final int MINUTES_TO_UPDATE_USER = 15;
    private static final int HOURS_TO_UPDATE_CHAT_MESSAGES = 6;

    private static IntouchRepository INSTANCE;

    private SharedPreferences sharedPreferences;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseMessaging messaging;
    private IntouchDatabase intouchDatabase;
    private NetworkUtils networkUtils;

    public IntouchRepository(Context context) {
        this.intouchDatabase = IntouchDatabase.getInstance(context.getApplicationContext());
        this.sharedPreferences = context.getSharedPreferences(INTOUCH_PREFS, Context.MODE_PRIVATE);
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
        this.messaging = FirebaseMessaging.getInstance();
        this.networkUtils = NetworkUtils.getInstance(context);
    }

    public static IntouchRepository getInstance(Context context) {
        if(INSTANCE == null) {
            return new IntouchRepository(context);
        }

        return INSTANCE;
    }

    /***************************** SHARED PREFERENCES FUNCTIONS ***********************************/

    public void saveCurrentUserInPrefs(User user) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(PREFS_KEY_LOGGED_IN_USER, new Gson().toJson(user));

        prefsEditor.apply();
    }

    public User getCurrentUserFromPrefs() {
        String userJson = sharedPreferences.getString(PREFS_KEY_LOGGED_IN_USER, "");

        if(userJson != null && !userJson.isEmpty()) {
            return new Gson().fromJson(userJson, User.class);
        } else {
            return null;
        }
    }

    public void saveFcmUpdateTimeStamp() {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putLong(PREFS_KEY_FCM_UPDATE_TIMESTAMP, Calendar.getInstance().getTimeInMillis());

        prefsEditor.apply();
    }

    public Date getFcmUpdateTimeStamp() {
        Long fcmUpdateTimeInMillis = sharedPreferences.getLong(PREFS_KEY_FCM_UPDATE_TIMESTAMP, 0);

        if(fcmUpdateTimeInMillis == 0) {
            return null;
        }

        return new Date(fcmUpdateTimeInMillis);
    }

    public void saveCurrentUserUpdateTimeStamp() {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putLong(PREFS_KEY_USER_UPDATE_TIMESTAMP, Calendar.getInstance().getTimeInMillis());

        prefsEditor.apply();
    }

    public Date getCurrentUserUpdateTimeStamp() {
        long userUpdateTimeInMillis = sharedPreferences.getLong(PREFS_KEY_USER_UPDATE_TIMESTAMP, 0);

        if(userUpdateTimeInMillis == 0) {
            return null;
        }

        return new Date(userUpdateTimeInMillis);
    }

    public void saveChatsUpdateTimeStamp() {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putLong(PREFS_KEY_CHATS_UPDATE_TIMESTAMP, Calendar.getInstance().getTimeInMillis());

        prefsEditor.apply();
    }

    public Date getChatsUpdateTimeStamp() {
        long chatsUpdateTimeInMillis = sharedPreferences.getLong(PREFS_KEY_CHATS_UPDATE_TIMESTAMP, 0);

        if(chatsUpdateTimeInMillis == 0) {
            return null;
        }

        return new Date(chatsUpdateTimeInMillis);
    }

    public void saveDeviceId(String deviceId) {
        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(PREFS_KEY_DEVICE_ID, deviceId);

        prefsEditor.apply();
    }

    public String getDeviceId() {
        return sharedPreferences.getString(PREFS_KEY_DEVICE_ID, null);
    }

    private void clearSharedPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }

    /************************************* FOR LOGGED IN USER **************************************/

    public Single<User> saveAndUploadNewLoggedInUserDetails(User user) {
        Single<User> single = Single.fromCallable(new Callable<User>() {
            @Override
            public User call() throws Exception {
                if(networkUtils.getNetworkStatus() == 0) {
                    throw IntouchErrorHandler.getNoInternetException();
                }

                String fcmToken = Tasks.await(messaging.getToken());
                LoggedInDevice currentLoggedInDevice = CommonMethods.getCurrentLoggedInDevice(fcmToken);

                user.loggedInDevices = new ArrayList<>();
                user.loggedInDevices.add(currentLoggedInDevice);

                Tasks.await(insertUserIntoFirebase(user));

                saveCurrentUserInPrefs(user);
                saveCurrentUserUpdateTimeStamp();
                saveFcmUpdateTimeStamp();

                return user;
            }
        });

        return single.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Maybe<User> getLoggedInUserDetails(String userId, boolean forceUpdate) {
        Maybe<User> maybe = Maybe.fromCallable(new Callable<User>() {
            @Override
            public User call() throws Exception {
                User user = getCurrentUserFromPrefs();

                if(user != null && !currentUserUpdateNeeded() && !forceUpdate) {
                    Log.d(TAG, "getLoggedInUserDetails: Sending Local User");
                    return user;
                }

                if(networkUtils.getNetworkStatus() == 0) {
                    return user;
                }

                DocumentSnapshot docSnapshot = Tasks.await(getUserFromFirebaseById(userId));

                if(docSnapshot != null && docSnapshot.toObject(User.class) != null) {
                    user = docSnapshot.toObject(User.class);

                    saveCurrentUserInPrefs(user);
                    saveCurrentUserUpdateTimeStamp();

                    Log.d(TAG, "getLoggedInUserDetails: Sending Firebase User");
                    return user;
                }

                return null;
            }
        });

        return maybe.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable getAndUpdateFcmToken() {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                if(networkUtils.getNetworkStatus() == 0) {
                    return;
                }

                if(CommonVariables.loggedInUser == null) {
                    return;
                }

                if(!fcmUpdateNeeded()) {
                    Log.d(TAG, "getLoggedInUserDetails: Not Updating FCM...");

                    return;
                }

                Log.d(TAG, "getLoggedInUserDetails: Updating FCM...");

                String fcmToken = Tasks.await(messaging.getToken());
                CommonMethods.updateUserLoggedInDevices(CommonVariables.loggedInUser, fcmToken);

                Tasks.await(insertUserIntoFirebase(CommonVariables.loggedInUser));

                saveCurrentUserInPrefs(CommonVariables.loggedInUser);
                saveCurrentUserUpdateTimeStamp();
                saveFcmUpdateTimeStamp();
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Maybe<User> getUserFromFirebase(String userId) {
        Maybe<User> maybe = Maybe.fromCallable(new Callable<User>() {
            @Override
            public User call() throws Exception {
                Task<DocumentSnapshot> task = db.collection(Constants.COLLECTION_USERS)
                        .document(userId)
                        .get();

                DocumentSnapshot documentSnapshot = Tasks.await(task);

                if(documentSnapshot != null) {
                    return documentSnapshot.toObject(User.class);
                }

                return null;
            }
        });

        return maybe.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable logoutFromDevice(LoggedInDevice device) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                User user = CommonVariables.loggedInUser;
                user.loggedInDevices.remove(device);

                Tasks.await(insertUserIntoFirebase(user));

                if(device.deviceId.equals(CommonVariables.deviceId)) {
                    clearSharedPreferences();
                    intouchDatabase.clearAllTables();
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable logoutFromAllDevices() {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                User user = CommonVariables.loggedInUser;
                user.loggedInDevices = new ArrayList<>();

                Tasks.await(insertUserIntoFirebase(user));

                clearSharedPreferences();
                intouchDatabase.clearAllTables();
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable clearAllCachedData() {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                clearSharedPreferences();
                intouchDatabase.clearAllTables();
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private Task<Void> insertUserIntoFirebase(User user) {
        return db.collection(Constants.COLLECTION_USERS)
                .document(user.id)
                .set(user);
    }

    /***************************************** DATA ***********************************************/

    public Completable createChat(Chat chat) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                insertChatIntoLocalDB(chat);

                if(networkUtils.getNetworkStatus() != 0 && !chat.isSynced) {
                    Tasks.await(insertChatToFirebase(chat));

                    chat.isSynced = true;
                    insertChatIntoLocalDB(chat);
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable retrieveAndSaveUserChats() {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                if(networkUtils.getNetworkStatus() == 0) {
                    return;
                }

                if(!isChatsUpdateNeeded()) {
                    return;
                }

                QuerySnapshot querySnapshot = Tasks.await(getChatsFromFirebase());

                if(querySnapshot != null && querySnapshot.size() > 0) {
                    List<Chat> chatList = querySnapshot.toObjects(Chat.class);

                    for(Chat chat: chatList) {
                        chat.isSynced = true;
                    }

                    insertChatListToLocalDB(chatList);

                    saveChatsUpdateTimeStamp();
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Flowable<List<Chat>> getUserChatsFlowable() {
        return intouchDatabase.intouchDao().getUserChats()
                .subscribeOn(Schedulers.io());
    }

    public Flowable<Chat> getChatFlowable(String chatId) {
        return intouchDatabase.intouchDao().getChatFlowable(chatId);
    }

    public Observable<Chat> getChatExtraDetails(List<Chat> chatList) {
        Log.d(TAG, "getChatExtraDetails: Called");
        return Flowable.fromIterable(chatList)
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<Chat, Publisher<Chat>>() {
                    @Override
                    public Publisher<Chat> apply(Chat chat) throws Throwable {
                        return Flowable.fromCallable(new Callable<Chat>() {
                            @Override
                            public Chat call() throws Exception {
                                String otherUserId = CommonMethods.getOtherUserIdFromChat(chat);
                                User otherUser = getUserById(otherUserId).blockingGet();

                                Message lastChatMessage;

                                List<Message> chatUnreadMessages = getUnreadChatMessagesFromLocalDB(chat.id);

                                if(!chatUnreadMessages.isEmpty()) {
                                    lastChatMessage = chatUnreadMessages.get(0);
                                    chat.unreadMessagesCount = chatUnreadMessages.size();

                                } else {
                                    lastChatMessage = getChatLastMessage(chat).blockingGet();
                                }

                                chat.otherUser = otherUser;
                                chat.lastMessage = lastChatMessage;

                                Log.d(TAG, "Extra data: " + chat);

                                return chat;
                            }
                        });
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .toObservable();
    }

    public Maybe<Chat> getChatByUsers(User otherUser) {
        Maybe<Chat> maybe = Maybe.fromCallable(new Callable<Chat>() {
            @Override
            public Chat call() throws Exception {
                List<Chat> chats = getUserChatsFlowable().firstElement().blockingGet();

                for(Chat chat: chats) {
                    if(chat.users.contains(otherUser.id)) {
                        return chat;
                    }
                }

                QuerySnapshot querySnapshot = Tasks.await(getChatByUsersFromFirebase(otherUser.id));

                if(querySnapshot != null && querySnapshot.size() > 0) {
                    Chat retrievedChat = querySnapshot.toObjects(Chat.class).get(0);

                    retrievedChat.isSynced = true;
                    insertChatIntoLocalDB(retrievedChat);

                    return retrievedChat;
                }

                return null;
            }
        });

        return maybe.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // # MUF - Mass Updating Function
    public Completable uploadAllUnsyncedChats() {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                List<Chat> chats = getAllUnsyncedChats();
                WriteBatch batch = db.batch();

                for(Chat chat: chats) {
                    DocumentReference docRef = db.collection(Constants.COLLECTION_CHATS)
                            .document(chat.id);

                    batch.set(docRef, chat);
                    chat.isSynced = true;
                }

                if(networkUtils.getNetworkStatus() != 0) {
                    Tasks.await(batch.commit());
                } else {
                    for(Chat chat: chats) {
                        chat.isSynced = false;
                    }
                }

                insertChatListToLocalDB(chats);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable toggleUserTypingStatus(Chat chat) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                if(chat.typing.contains(CommonVariables.loggedInUser.id)) {
                    chat.typing.remove(CommonVariables.loggedInUser.id);

                } else {
                    chat.typing.add(CommonVariables.loggedInUser.id);
                }

                Tasks.await(insertChatToFirebase(chat));
                insertChatIntoLocalDB(chat);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable createMessage(Message message) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                insertMessageIntoLocalDB(message);

                if(networkUtils.getNetworkStatus() != 0 && !message.isSynced) {
                    message.status = MessageStatus.SENT;
                    Tasks.await(insertMessageToFirebase(message));

                    message.isSynced = true;
                    insertMessageIntoLocalDB(message);
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable uploadFileMessage(Message message) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                if(networkUtils.getNetworkStatus() != 0 && !message.isSynced) {
                    Log.d(TAG, "uploadFileMessage: " + message);

                    message.status = MessageStatus.SENT;
                    Tasks.await(insertMessageToFirebase(message));

                    message.isSynced = true;
                    insertMessageIntoLocalDB(message);
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable downloadFileMessage(Message message) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                if(networkUtils.getNetworkStatus() != 0) {
                    Log.d(TAG, "downloadFileMessage: " + message);

                    message.isSynced = true;
                    insertMessageIntoLocalDB(message);
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable insertFileMessageIntoLocalDB(Message message) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                Log.d(TAG, "insertFileMessageIntoLocalDB: " + message);
                insertMessageIntoLocalDB(message);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Completable insertFileMessagesIntoLocalDB(List<Message> messageList) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                insertMessageListIntoLocalDB(messageList);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }


    public Completable retrieveAndSaveUserMessages(Chat chat) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                if(networkUtils.getNetworkStatus() == 0) {
                    return;
                }

                if(!isMessagesUpdateNeeded(chat)) {
                    return;
                }

                QuerySnapshot querySnapshot = Tasks.await(getChatsFromFirebase());

                if(querySnapshot != null && querySnapshot.size() > 0) {
                    List<Message> messageList = querySnapshot.toObjects(Message.class);

                    for(Message message: messageList) {
                        handleMessageModifications(message, true);
                    }

                    insertMessageListIntoLocalDB(messageList);

                    chat.messagesRetrievedTimestamp = Calendar.getInstance().getTime();
                    insertChatIntoLocalDB(chat);
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Maybe<Message> getChatLastMessage(Chat chat) {
        Maybe<Message> maybe = Maybe.fromCallable(new Callable<Message>() {
            @Override
            public Message call() throws Exception {
                Message locallySavedLastMessage = getChatLastMessageFromLocalDB(chat.id);

                if(locallySavedLastMessage != null) {
                    return locallySavedLastMessage;
                }

                QuerySnapshot querySnapshot = Tasks.await(getChatLastMessageFromFirebase(chat.id));

                if(querySnapshot != null && querySnapshot.size() > 0) {
                    Message message = querySnapshot.toObjects(Message.class).get(0);
                    handleMessageModifications(message, true);

                    insertMessageIntoLocalDB(message);
                    return message;
                }

                return null;
            }
        });

        return maybe.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // # MUF - Mass Updating Function
    public Completable uploadAllUnsyncedTextMessages() {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                List<Message> messages = getAllUnsyncedMessages(MessageType.TYPE_TEXT);
                WriteBatch batch = db.batch();

                Log.d(TAG, "uploadAllUnsyncedMessages: " + messages);

                for(Message message: messages) {
                    DocumentReference docRef = db.collection(Constants.COLLECTION_MESSAGES)
                            .document(message.id);

                    if(message.status.equals(MessageStatus.WAITING_TO_SEND)
                            && message.sentByUserId.equals(CommonVariables.loggedInUser.id)) {
                        message.status = MessageStatus.SENT;
                    }
                    message.isSynced = true;

                    batch.set(docRef, message);
                }

                if(networkUtils.getNetworkStatus() != 0) {
                    Tasks.await(batch.commit());
                } else {
                    for(Message message: messages) {
                        message.isSynced = false;
                    }
                }

                insertMessageListIntoLocalDB(messages);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // # MUF - Mass Updating Function
    public Completable markMessagesAsDelivered(List<Message> messages) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                Log.d(TAG, "markMessagesAsDelivered: " + messages);
                WriteBatch batch = db.batch();

                for(Message message: messages) {
                    DocumentReference docRef = db.collection(Constants.COLLECTION_MESSAGES)
                            .document(message.id);

                    if(message.status.equals(MessageStatus.SENT)
                            && !message.sentByUserId.equals(CommonVariables.loggedInUser.id)) {
                        message.status = MessageStatus.DELIVERED;
                        message.receivingTime = Calendar.getInstance().getTime();
                    }

                    message.isSynced = true;
                    batch.set(docRef, message);
                }

                if(networkUtils.getNetworkStatus() != 0) {
                    Tasks.await(batch.commit());
                } else {
                    for(Message message: messages) {
                        message.isSynced = false;
                    }
                }

                insertMessageListIntoLocalDB(messages);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // # MUF - Mass Updating Function
    public Completable markMessagesAsRead(List<Message> messages) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                Log.d(TAG, "markMessagesAsRead: " + messages);
                WriteBatch batch = db.batch();

                for(Message message: messages) {
                    DocumentReference docRef = db.collection(Constants.COLLECTION_MESSAGES)
                            .document(message.id);

                    if(message.status.equals(MessageStatus.DELIVERED)
                            && !message.sentByUserId.equals(CommonVariables.loggedInUser.id)) {
                        message.status = MessageStatus.READ;
                    }

                    message.isSynced = true;
                    batch.set(docRef, message);
                }

                if(networkUtils.getNetworkStatus() != 0) {
                    Tasks.await(batch.commit());
                } else {
                    for(Message message: messages) {
                        message.isSynced = false;
                    }
                }

                insertMessageListIntoLocalDB(messages);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Flowable<List<Message>> getChatMessagesFlowable(String chatId) {
        return intouchDatabase.intouchDao().getChatMessages(chatId)
                .subscribeOn(Schedulers.io());
    }

    public Flowable<List<Message>> getAllUnreadMessagesFlowable() {
        return intouchDatabase.intouchDao().getAllUnreadMessages(MessageStatus.READ,
                        CommonVariables.loggedInUser.id)
                .subscribeOn(Schedulers.io());
    }

    public Flowable<List<Message>> getReceivedMessagesByStatusFlowable(MessageStatus status) {
        return intouchDatabase.intouchDao().getReceivedMessagesByStatus(status,
                        CommonVariables.loggedInUser.id)
                .subscribeOn(Schedulers.io());
    }

    public Maybe<User> getUserByPhone(String phone) {
        Maybe<User> single = Maybe.fromCallable(new Callable<User>() {
            @Override
            public User call() throws Exception {
                User locallySavedUser = getUserFromLocalDB(Constants.FIELD_PHONE, phone);

                if(locallySavedUser != null && !isUserUpdateNeeded(locallySavedUser)) {
                    return locallySavedUser;
                }

                if(networkUtils.getNetworkStatus() == 0) {
                    throw IntouchErrorHandler.getNoInternetException();
                }

                Task<QuerySnapshot> task = db.collection(Constants.COLLECTION_USERS)
                        .whereEqualTo(Constants.FIELD_PHONE, phone)
                        .get();

                QuerySnapshot querySnapshot = Tasks.await(task);

                if(querySnapshot != null && querySnapshot.size() != 0) {
                    User user =  querySnapshot.toObjects(User.class).get(0);
                    saveUserToLocalDB(user);

                    return user;
                }

                return null;
            }
        });

        return single.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Maybe<User> getUserById(String userId) {
        Maybe<User> single = Maybe.fromCallable(new Callable<User>() {
            @Override
            public User call() throws Exception {
                User locallySavedUser = getUserFromLocalDB(Constants.FIELD_ID, userId);

                if(locallySavedUser != null && !isUserUpdateNeeded(locallySavedUser)) {
                    return locallySavedUser;
                }

                if(networkUtils.getNetworkStatus() == 0) {
                    throw IntouchErrorHandler.getNoInternetException();
                }

                Task<DocumentSnapshot> task = db.collection(Constants.COLLECTION_USERS)
                        .document(userId)
                        .get();

                DocumentSnapshot documentSnapshot = Tasks.await(task);

                if(documentSnapshot != null && documentSnapshot.toObject(User.class) != null) {
                    User user = documentSnapshot.toObject(User.class);
                    saveUserToLocalDB(user);

                    return user;
                }

                return null;
            }
        });

        return single.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Single<UserStatus> updateCurrentUserStatus(boolean isOnline) {
        Single<UserStatus> single = Single.fromCallable(new Callable<UserStatus>() {
            @Override
            public UserStatus call() throws Exception {
                UserStatus status = getUserStatusFromLocalDB(CommonVariables.loggedInUser.id);

                if(status == null) {
                    status = new UserStatus();

                    status.userId = CommonVariables.loggedInUser.id;
                    status.isOnline = isOnline;
                    status.lastSeenTime = Calendar.getInstance().getTime();
                }

                if(networkUtils.getNetworkStatus() != 0) {
                    Tasks.await(insertUserStatusIntoFirebase(status));

                } else {
                    throw IntouchErrorHandler.getNoInternetException();
                }

                return status;
            }
        });

        return single.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Flowable<UserStatus> getUserStatusFlowable(String userId) {
        return intouchDatabase.intouchDao().getUserStatusFlowable(userId);
    }

    /**************************************** LISTENERS *******************************************/

    public ListenerRegistration getUserChatsListener(OnQuerySnapshotListenerTriggered event) {
        return db.collection(Constants.COLLECTION_CHATS)
                .whereArrayContains(Constants.FIELD_USERS, CommonVariables.loggedInUser.id)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot,
                                        @Nullable FirebaseFirestoreException error) {

                        if(error != null) {
                            Log.d(TAG, "User Chats Listener: " + error);
                            return;
                        }

                        if(querySnapshot != null) {
                            event.onListenerTriggered(querySnapshot);
                        }
                    }
                });
    }

    public Completable handleUserChatsUpdates(QuerySnapshot querySnapshot) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                for(DocumentChange documentChange: querySnapshot.getDocumentChanges()) {
                    Chat chat = documentChange.getDocument().toObject(Chat.class);
                    Chat locallySavedChat = getChatById(chat.id);

                    if(locallySavedChat != null && locallySavedChat.messagesRetrievedTimestamp != null) {
                        chat.messagesRetrievedTimestamp = locallySavedChat.messagesRetrievedTimestamp;
                    }

                    chat.isSynced = true;

                    switch(documentChange.getType()) {
                        case ADDED:
                        case MODIFIED:
                            insertChatIntoLocalDB(chat);
                    }

                    Log.d(TAG, "handleUserChatsUpdates: " + chat);
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public ListenerRegistration getChatListener(String chatId, OnDocSnapshotListenerTriggered event) {
        return db.collection(Constants.COLLECTION_CHATS)
                .document(chatId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException error) {

                        if(error != null) {
                            Log.d(TAG, "Chat Listener: " + error);
                            return;
                        }

                        if(documentSnapshot != null) {
                            event.onListenerTriggered(documentSnapshot);
                        }

                    }
                });
    }

    public Completable handleChatUpdates(DocumentSnapshot documentSnapshot, Chat chat) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                if(documentSnapshot == null){
                    return;
                }

                Chat updatedChat = documentSnapshot.toObject(Chat.class);

                updatedChat.isSynced = true;
                updatedChat.messagesRetrievedTimestamp = chat.messagesRetrievedTimestamp;

                insertChatIntoLocalDB(updatedChat);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public ListenerRegistration getUnreadMessagesListener(OnQuerySnapshotListenerTriggered event) {
        return db.collection(Constants.COLLECTION_MESSAGES)
                .whereArrayContains(Constants.FIELD_USERS, CommonVariables.loggedInUser.id)
                .whereNotEqualTo(Constants.FIELD_STATUS, MessageStatus.READ)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot,
                                        @Nullable FirebaseFirestoreException error) {

                        if(error != null) {
                            Log.d(TAG, "Unread Messages Listener: " + error);
                            return;
                        }

                        if(querySnapshot != null) {
                            event.onListenerTriggered(querySnapshot);
                        }
                    }
                });
    }

    public Completable handleUnreadMessages(QuerySnapshot querySnapshot) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                for(DocumentChange documentChange: querySnapshot.getDocumentChanges()) {
                    Message message = documentChange.getDocument().toObject(Message.class);

                    if(!message.sentByUserId.equals(CommonVariables.loggedInUser.id)) {
                        checkSavedMessageAndUpdate(message);

                        message.fileDownloadStatus = FileDownloadStatus.NOT_DOWNLOADING;
                        message.downloadingPercentage = 0;

                        message.isSynced = true;
                        message.orderTimestamp = message.receivingTime;

                        switch(documentChange.getType()) {
                            case ADDED:
                            case MODIFIED:
                                insertMessageIntoLocalDB(message);
                        }
                    }
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public ListenerRegistration getChatMessagesListener(String chatId,
                                                        OnQuerySnapshotListenerTriggered event) {

        return db.collection(Constants.COLLECTION_MESSAGES)
                .whereEqualTo(Constants.FIELD_CHAT_ID, chatId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot querySnapshot,
                                        @Nullable FirebaseFirestoreException error) {

                        if(error != null) {
                            Log.d(TAG, "Chat Messages Listener: " + error);
                            return;
                        }

                        if(querySnapshot != null) {
                            event.onListenerTriggered(querySnapshot);
                        }
                    }
                });
    }

    public Completable handleChatMessages(Chat chat, QuerySnapshot querySnapshot) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                for(DocumentChange documentChange: querySnapshot.getDocumentChanges()) {
                    Message message = documentChange.getDocument().toObject(Message.class);

                    checkSavedMessageAndUpdate(message);

                    Log.d(TAG, "chatmessss: " + message);

                    message.isSynced = true;
                    message.orderTimestamp = message.receivingTime;

                    switch(documentChange.getType()) {
                        case ADDED:
                        case MODIFIED:
                            insertMessageIntoLocalDB(message);
                    }
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    private void checkSavedMessageAndUpdate(Message message) {
        Message savedMessage = getMessageByIdFromLocalDB(message.id);

        if(savedMessage != null) {
            message.uploadingPercentage = savedMessage.uploadingPercentage;
            message.fileUploadStatus = savedMessage.fileUploadStatus;
            message.downloadingPercentage = savedMessage.downloadingPercentage;
            message.fileDownloadStatus = savedMessage.fileDownloadStatus;
            message.localFileUriString = savedMessage.localFileUriString;
        }
    }

    public ListenerRegistration getUserStatusListener(String userId,
                                                        OnDocSnapshotListenerTriggered event) {

        return db.collection(Constants.COLLECTION_USER_STATUSES)
                .document(userId)
                .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                    @Override
                    public void onEvent(@Nullable DocumentSnapshot documentSnapshot,
                                        @Nullable FirebaseFirestoreException error) {

                        if(error != null) {
                            Log.d(TAG, "User Status Listener: " + error);
                            return;
                        }

                        if(documentSnapshot != null) {
                            event.onListenerTriggered(documentSnapshot);
                        }

                    }
                });
    }

    public Completable handleUserStatus(DocumentSnapshot documentSnapshot) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                if(documentSnapshot == null){
                    return;
                }

                UserStatus userStatus = documentSnapshot.toObject(UserStatus.class);
                insertUserStatusIntoLocalDB(userStatus);
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /***************************************** PRIVATE ********************************************/

    private void saveUserToLocalDB(User user) {
        user.retrievedTimestamp = Calendar.getInstance().getTime();
        intouchDatabase.intouchDao().insertUser(user);
    }

    private User getUserFromLocalDB(String field, String value) {
        if(field.equals(Constants.FIELD_PHONE)) {
            return intouchDatabase.intouchDao().getUserByPhone(value);
        } else {
            return intouchDatabase.intouchDao().getUserById(value);
        }
    }

    private Task<DocumentSnapshot> getUserFromFirebaseById(String userId) {
        return db.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get();
    }

    private void insertChatIntoLocalDB(Chat chat) {
        intouchDatabase.intouchDao().insertChat(chat);
    }

    private Task<Void> insertChatToFirebase(Chat chat) {
        return db.collection(Constants.COLLECTION_CHATS)
                .document(chat.id)
                .set(chat);
    }

    private void insertChatListToLocalDB(List<Chat> chatList) {
        intouchDatabase.intouchDao().insertChatList(chatList);
    }

    private Chat getChatById(String chatId) {
        return intouchDatabase.intouchDao().getChatById(chatId);
    }

    private Task<QuerySnapshot> getChatsFromFirebase() {
        return db.collection(Constants.COLLECTION_CHATS)
                .whereArrayContains(Constants.FIELD_USERS, CommonVariables.loggedInUser.id)
                .get();
    }

    private Task<QuerySnapshot> getChatByUsersFromFirebase(String otherUserId) {
        return db.collection(Constants.COLLECTION_CHATS)
                .whereIn(Constants.FIELD_USERS,
                        Arrays.asList(Arrays.asList(otherUserId, CommonVariables.loggedInUser.id),
                                Arrays.asList(CommonVariables.loggedInUser.id, otherUserId)))
                .get();
    }

    private List<Chat> getAllUnsyncedChats() {
        return intouchDatabase.intouchDao().getAllUnsyncedChats();
    }

    private Task<QuerySnapshot> getChatMessagesFromFirebase(String chatId) {
        return db.collection(Constants.COLLECTION_MESSAGES)
                .whereEqualTo(Constants.FIELD_CHAT_ID, chatId)
                .get();
    }

    private void insertMessageListIntoLocalDB(List<Message> messageList) {
        intouchDatabase.intouchDao().insertMessageList(messageList);
    }

    private void insertMessageIntoLocalDB(Message message) {
        intouchDatabase.intouchDao().insertMessage(message);
    }

    private Task<Void> insertMessageToFirebase(Message message) {
        return db.collection(Constants.COLLECTION_MESSAGES)
                .document(message.id)
                .set(message);
    }

    private Message getMessageByIdFromLocalDB(String messageId) {
        return intouchDatabase.intouchDao().getMessageById(messageId);
    }

    private Message getChatLastMessageFromLocalDB(String chatId) {
        return intouchDatabase.intouchDao().getChatLastMessage(chatId);
    }

    private List<Message> getUnreadChatMessagesFromLocalDB(String chatId) {
        return intouchDatabase.intouchDao().getUnreadChatMessages(chatId, MessageStatus.READ,
                CommonVariables.loggedInUser.id);
    }

    private Task<QuerySnapshot> getChatLastMessageFromFirebase(String chatId) {
        return db.collection(Constants.COLLECTION_MESSAGES)
                .whereEqualTo(Constants.FIELD_CHAT_ID, chatId)
                .orderBy(Constants.FIELD_SENDING_TIME, Query.Direction.DESCENDING)
                .limit(1)
                .get();
    }

    private List<Message> getAllUnsyncedMessages(MessageType type) {
        return intouchDatabase.intouchDao().getAllUnsyncedMessages(type);
    }

    private void insertUserStatusIntoLocalDB(UserStatus status) {
        intouchDatabase.intouchDao().insertUserStatus(status);
    }

    private Task<Void> insertUserStatusIntoFirebase(UserStatus status) {
        return db.collection(Constants.COLLECTION_USER_STATUSES)
                .document(status.userId)
                .set(status);
    }

    private UserStatus getUserStatusFromLocalDB(String userId) {
        return intouchDatabase.intouchDao().getUserStatus(userId);
    }

    /*************************************** UTILITIES ********************************************/

    private boolean isChatsUpdateNeeded() {
        Date chatsUpdateTimestamp = getChatsUpdateTimeStamp();

        return (System.currentTimeMillis() - chatsUpdateTimestamp.getTime())
                > TimeUnit.HOURS.toMillis(HOURS_TO_UPDATE_CHATS);
    }

    private boolean isUserUpdateNeeded(User user) {
        Date userRetrievedTimestamp = user.retrievedTimestamp;

        return (System.currentTimeMillis() - userRetrievedTimestamp.getTime())
                > TimeUnit.MINUTES.toMillis(MINUTES_TO_UPDATE_USER);
    }

    private boolean isMessagesUpdateNeeded(Chat chat) {
        Date messagesRetrievedTimestamp = chat.messagesRetrievedTimestamp;

        return (System.currentTimeMillis() - messagesRetrievedTimestamp.getTime())
                > TimeUnit.HOURS.toMillis(HOURS_TO_UPDATE_CHAT_MESSAGES);
    }

    private Message handleMessageModifications(Message message, boolean isSynced) {
        checkSavedMessageAndUpdate(message);

        message.isSynced = isSynced;
        message.orderTimestamp = message.sentByUserId.equals(CommonVariables.loggedInUser.id)
                ? message.sendingTime : message.receivingTime;

        return message;
    }

    private boolean currentUserUpdateNeeded() {
        Date userLastUpdatedTimestamp = getCurrentUserUpdateTimeStamp();

        if(userLastUpdatedTimestamp == null) {
            return true;
        }

        return (System.currentTimeMillis() - userLastUpdatedTimestamp.getTime())
                > TimeUnit.MINUTES.toMillis(MINUTES_TO_UPDATE_USER);
    }

    private boolean fcmUpdateNeeded() {
        Date fcmLastUpdatedTimestamp = getFcmUpdateTimeStamp();

        if(fcmLastUpdatedTimestamp == null) {
            return true;
        }

        return System.currentTimeMillis() - fcmLastUpdatedTimestamp.getTime()
                > TimeUnit.HOURS.toMillis(HOURS_TO_UPDATE_FCM_TOKEN);
    }
}
