package com.easycodingg.intouch.services;

import static com.easycodingg.intouch.utils.Constants.NOTIFICATION_CHANNEL_LOW;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.easycodingg.intouch.R;
import com.easycodingg.intouch.db.IntouchRepository;
import com.easycodingg.intouch.models.IntouchDocument;
import com.easycodingg.intouch.models.IntouchImage;
import com.easycodingg.intouch.models.Message;
import com.easycodingg.intouch.models.UserStatus;
import com.easycodingg.intouch.utils.CommonMethods;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.NetworkUtils;
import com.easycodingg.intouch.utils.enums.FileDownloadStatus;
import com.easycodingg.intouch.utils.enums.FileUploadStatus;
import com.easycodingg.intouch.utils.enums.MessageStatus;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StreamDownloadTask;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class IntouchService extends LifecycleService {
    private static final String TAG = "IntouchServiceyyy";
    private static final int FOREGROUND_NOTIFICATION_ID = 1;

    // Keys
    public static final String KEY_FILE_TYPE = "KEY_FILE_TYPE";
    public static final String KEY_UPLOADING_FILES_LIST = "KEY_UPLOADING_FILES_LIST";
    public static final String KEY_DOWNLOADING_FILES_LIST = "KEY_UPLOADING_FILES_LIST";
    public static final String KEY_UPLOADING_FILE = "KEY_UPLOADING_FILE";
    public static final String KEY_DOWNLOADING_FILE = "KEY_DOWNLOADING_FILE";

    // Actions
    public static final String ACTION_START_SERVICE = "ACTION_START_SERVICE";
    public static final String ACTION_STOP_SERVICE = "ACTION_STOP_SERVICE";
    public static final String ACTION_FOR_APP_COMES_FOREGROUND = "ACTION_FOR_APP_COMES_FOREGROUND";
    public static final String ACTION_FOR_APP_GOES_BACKGROUND = "ACTION_FOR_APP_GOES_BACKGROUND";
    public static final String ACTION_UPLOAD_FILES = "ACTION_UPLOAD_FILES";
    public static final String ACTION_DOWNLOAD_FILES = "ACTION_DOWNLOAD_FILES";
    public static final String ACTION_STOP_FILE_UPLOAD = "ACTION_STOP_FILE_UPLOAD";
    public static final String ACTION_STOP_FILE_DOWNLOAD = "ACTION_STOP_FILE_DOWNLOAD";
    public static final String ACTION_STOP_ALL_UPLOAD_AND_DOWNLOAD = "ACTION_STOP_ALL_UPLOAD_AND_DOWNLOAD";

    // Static Variables
    public static boolean isRunning;
    public static boolean isForeground;

    // Class Variables
    private boolean waitingForNetwork;
    private UserStatus currentUserStatus;

    private List<ServiceProcess> serviceProcesses;
    private List<FileUploadingProcess> fileUploadingProcesses;
    private List<FileDownloadingProcess> fileDownloadingProcesses;

    private MutableLiveData<Boolean> appClosedLiveData;
    private MutableLiveData<List<ServiceProcess>> serviceProcessesLiveData;
    private MutableLiveData<List<FileUploadingProcess>> fileUploadingProcessesLiveData;
    private MutableLiveData<List<FileDownloadingProcess>> fileDownloadingProcessesLiveData;

    private MediatorLiveData<ServiceStatus> serviceStatusLiveData;

    private NetworkUtils networkUtils;
    private IntouchRepository repository;
    private FirebaseStorage storage;

    private CompositeDisposable compositeDisposable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Called");

        isRunning = true;
        serviceProcesses = new ArrayList<>();
        fileUploadingProcesses = new ArrayList<>();
        fileDownloadingProcesses = new ArrayList<>();

        appClosedLiveData = new MutableLiveData<>(false);
        serviceProcessesLiveData = new MutableLiveData<>(serviceProcesses);
        fileUploadingProcessesLiveData = new MutableLiveData<>(fileUploadingProcesses);
        fileDownloadingProcessesLiveData = new MutableLiveData<>(fileDownloadingProcesses);

        repository = IntouchRepository.getInstance(this);
        networkUtils = NetworkUtils.getInstance(this);
        storage = FirebaseStorage.getInstance();

        compositeDisposable = new CompositeDisposable();

        addNetworkChangeListener();
        setupLiveDataObservers();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent == null || intent.getAction() == null) {
            return super.onStartCommand(intent, flags, startId);
        }

        String action = intent.getAction();

        switch(action) {
            case ACTION_START_SERVICE:
                beginService();
                break;

            case ACTION_FOR_APP_COMES_FOREGROUND:
                onAppComesForeground();
                break;

            case ACTION_FOR_APP_GOES_BACKGROUND:
                onAppGoesBackground();
                break;

            case ACTION_STOP_SERVICE:
                terminateService();
                break;

            case ACTION_UPLOAD_FILES:
                uploadFiles(intent);
                break;

            case ACTION_STOP_FILE_UPLOAD:
                stopFileUpload(intent);
                break;

            case ACTION_DOWNLOAD_FILES:
                downloadFiles(intent);
                break;

            case ACTION_STOP_FILE_DOWNLOAD:
                stopFileDownload(intent);
                break;

            case ACTION_STOP_ALL_UPLOAD_AND_DOWNLOAD:
                cancelAllUploadAndDownload();
                break;
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /************************************* START COMMAND FUNCTIONS *************************************/

    private void beginService() {
        updateUserStatus(true);
        handleUnsyncedChats();
        handleUnsyncedTextMessages();
        handleUnreadMessages();
    }

    private void onAppComesForeground() {
        updateUserStatus(true);
    }

    private void onAppGoesBackground() {
        updateUserStatus(false);
    }

    private void terminateService() {
        stopSelf();
    }

    private void uploadFiles(Intent intent) {
        Log.d(TAG, "uploadFiles: " + intent.getSerializableExtra(KEY_FILE_TYPE));
        makeServiceForeground(getUploadDownloadNotification("Uploading files.."));

        FileType fileType = (FileType) intent.getSerializableExtra(KEY_FILE_TYPE);

        if(fileType == FileType.TYPE_MESSAGE) {
            List<Message> messageList = (List<Message>) intent.getSerializableExtra(KEY_UPLOADING_FILES_LIST);
            Log.d(TAG, "uploadFiles: " + messageList);
            handleFileMessagesUploading(messageList);
        }
    }

    private void stopFileUpload(Intent intent) {
        Log.d(TAG, "stopFileUpload: Called");
        FileType fileType = (FileType) intent.getSerializableExtra(KEY_FILE_TYPE);
        FileUploadingProcess process = null;
        String fileId = "";

        if(fileType == FileType.TYPE_MESSAGE) {
            Message message = (Message) intent.getSerializableExtra(KEY_UPLOADING_FILE);
            Log.d(TAG, "stopFileUpload: " + message);
            fileId = message.id;
        }

        process = getUploadingProcess(fileType, fileId);
        if(process != null && !fileId.isEmpty()) {
            FileUploadingProcess finalProcess = process;
            Completable.fromAction(new Action() {
                @Override
                public void run() throws Throwable {
                    cancelUploadingProcess(finalProcess);
                }
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "stopFileUpload: onComplete: Successful");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.d(TAG, "stopFileUpload: onError: " + e);
                        }
                    });
        }
    }

    private void downloadFiles(Intent intent) {
        Log.d(TAG, "downloadFiles: Called");
        makeServiceForeground(getUploadDownloadNotification("Downloading files.."));

        FileType fileType = (FileType) intent.getSerializableExtra(KEY_FILE_TYPE);

        if(fileType == FileType.TYPE_MESSAGE) {
            List<Message> messageList = (List<Message>) intent.getSerializableExtra(KEY_DOWNLOADING_FILES_LIST);
            Log.d(TAG, "downloadFiles: " + messageList);

            handleFileMessagesDownloading(messageList);
        }
    }

    private void stopFileDownload(Intent intent) {
        Log.d(TAG, "stopFileDownload: Called");
        FileType fileType = (FileType) intent.getSerializableExtra(KEY_FILE_TYPE);
        FileDownloadingProcess process = null;
        String fileId = "";

        if(fileType == FileType.TYPE_MESSAGE) {
            Message message = (Message) intent.getSerializableExtra(KEY_DOWNLOADING_FILE);
            Log.d(TAG, "stopFileDownload: " + message);
            fileId = message.id;
        }

        process = getDownloadingProcess(fileType, fileId);
        if(process != null && !fileId.isEmpty()) {

            FileDownloadingProcess finalProcess = process;
            Completable.fromAction(new Action() {
                        @Override
                        public void run() throws Throwable {
                            cancelDownloadingProcess(finalProcess);
                        }
                    }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new CompletableObserver() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onComplete() {
                            Log.d(TAG, "stopFileDownload: onComplete: Successful");
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            Log.d(TAG, "stopFileDownload: onError: " + e);
                        }
                    });
        }
    }

    private void cancelAllUploadAndDownload() {
        Log.d(TAG, "cancelAllUploadAndDownload: Called");

        Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                for(FileUploadingProcess process: new ArrayList<>(fileUploadingProcesses)) {
                    cancelUploadingProcess(process);
                }

                for(FileDownloadingProcess process: new ArrayList<>(fileDownloadingProcesses)) {
                    cancelDownloadingProcess(process);
                }
            }
        }).subscribeOn(Schedulers.io())
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "cancelAllUploadAndDownload: onComplete: Successful");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "cancelAllUploadAndDownload: onError: " + e);
                    }
                });
    }

    /******************************************* CANCELLATION *********************************************/

    private void cancelUploadingProcess(FileUploadingProcess process) {
        process.task.removeOnProgressListener(process.progressListener);
        process.task.cancel();

        switch(process.fileType) {
            case TYPE_MESSAGE:
                Message message = process.message;

                message.uploadingPercentage = 0;
                message.fileUploadStatus = FileUploadStatus.NOT_UPLOADING;

                Log.d(TAG, "stopFileUpload: Before Updating Message: " + message);
                repository.insertFileMessageIntoLocalDB(message).blockingAwait();
                Log.d(TAG, "stopFileUpload: Updated Message: " + message);
                break;

            case TYPE_SCANNED_DOCUMENT:
                // TODO: Handle cancel modifications of scanned document
                break;

            case TYPE_PAINT_IMAGE:
                // TODO: Handle cancel modifications of paint image
                break;

        }

        removeFileUploadingProcess(process);
    }

    private void cancelDownloadingProcess(FileDownloadingProcess process) {
        process.isCancelled = true;
        process.task.cancel();

        switch(process.fileType) {
            case TYPE_MESSAGE:
                Message message = process.message;

                message.downloadingPercentage = 0;
                message.fileDownloadStatus = FileDownloadStatus.NOT_DOWNLOADING;

                repository.insertFileMessageIntoLocalDB(message).blockingAwait();
                break;

            case TYPE_SCANNED_DOCUMENT:
                // TODO: Handle cancel modifications of scanned document
                break;

            case TYPE_PAINT_IMAGE:
                // TODO: Handle cancel modifications of paint image
                break;

        }

        removeFileDownloadingProcess(process);
    }

    /**************************************** UPLOADING *************************************************/

    private void handleFileMessagesUploading(List<Message> messageList) {
        for(Message message: messageList) {
            uploadMessageFile(message);
        }
    }

    private void uploadMessageFile(Message message) {
        Uri uploadFileUri = Uri.parse(message.localFileUriString);
        File fileToUpload = new File(uploadFileUri.getPath());

        String storagePath = "chats/" + message.chatId + "/" + fileToUpload.getName();
        StorageReference storageRef = storage.getReference(storagePath);

        UploadTask uploadTask = storageRef.putFile(uploadFileUri);

        FileUploadingProcess uploadingProcess = new FileUploadingProcess();
        uploadingProcess.id = UUID.randomUUID().toString();
        uploadingProcess.fileType = FileType.TYPE_MESSAGE;
        uploadingProcess.message = message;
        uploadingProcess.task = uploadTask;
        uploadingProcess.progressListener = new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@androidx.annotation.NonNull UploadTask.TaskSnapshot snapshot) {
                double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                Log.d(TAG, "onProgress: " + progress + "% done");

                message.uploadingPercentage = (int) progress;
                message.fileUploadStatus = FileUploadStatus.UPLOADING;
                handleFileMessageStatus(message);
            }
        };

        addFileUploadingProcess(uploadingProcess);

        uploadTask.addOnProgressListener(uploadingProcess.progressListener);

        uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@androidx.annotation.NonNull Task<UploadTask.TaskSnapshot> task) {
                if(task.isSuccessful()) {
                    Log.d(TAG, "onComplete: Message Upload Successful");

                    storageRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@androidx.annotation.NonNull Task<Uri> task) {
                            if(task.isSuccessful()) {
                                message.fileDownloadUrl = task.getResult().toString();
                                message.fileUploadStatus = FileUploadStatus.UPLOADED;

                                repository.uploadFileMessage(message)
                                        .subscribe(new CompletableObserver() {
                                            @Override
                                            public void onSubscribe(@NonNull Disposable d) {
                                                compositeDisposable.add(d);
                                            }

                                            @Override
                                            public void onComplete() {
                                                Log.d(TAG, "onComplete: Message uploaded " + message);
                                                removeFileUploadingProcess(uploadingProcess);
                                            }

                                            @Override
                                            public void onError(@NonNull Throwable e) {
                                                Log.d(TAG, "onError: " + e);
                                                removeFileUploadingProcess(uploadingProcess);
                                            }
                                        });

                            } else {
                                Log.d(TAG, "onComplete: " + task.getException());
                                removeFileUploadingProcess(uploadingProcess);
                            }
                        }
                    });
                } else {
                    Log.d(TAG, "onComplete: " + task.getException());
                    removeFileUploadingProcess(uploadingProcess);
                }
            }
        });

        Log.d(TAG, "uploadFileMessage: IntouchService: Finished");
    }

    /**************************************** DOWNLOADING *************************************************/

    private void handleFileMessagesDownloading(List<Message> messageList) {
        for(Message message: messageList) {
            downloadMessageFile(message);
        }
    }

    private void downloadMessageFile(Message message) {
        StorageReference downloadFileRef = storage.getReferenceFromUrl(message.fileDownloadUrl);

        Log.d(TAG, "downloadMessageFile: " + downloadFileRef.getPath());
        Log.d(TAG, "downloadMessageFile: " + downloadFileRef.getName());
        Log.d(TAG, "downloadMessageFile: " + CommonMethods.getExtensionFromFileName(downloadFileRef.getName()));
        Log.d(TAG, "downloadMessageFile: " + CommonMethods.getMimeTypeFromFileName(downloadFileRef.getName()));

        String mimeType = CommonMethods.getMimeTypeFromFileName(downloadFileRef.getName());
        boolean imageFile = mimeType.contains("image");

        Log.d(TAG, "downloadMessageFile: Image File " + imageFile);

        Uri savedFileUri = null;

        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/iN Touch/"
                    + downloadFileRef.getName();

            savedFileUri = Uri.fromFile(new File(filePath));

        } else {
            ContentResolver contentResolver = getContentResolver();

            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, downloadFileRef.getName());
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,
                    imageFile ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_DOWNLOADS);

            if(imageFile) {
                savedFileUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            } else {
                savedFileUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
            }
        }

        Log.d(TAG, "downloadMessageFile: Uri: " + savedFileUri.toString());
        Log.d(TAG, "downloadMessageFile: Uri Path: " + savedFileUri.getPath());

        StreamDownloadTask downloadTask = downloadFileRef.getStream();

        FileDownloadingProcess downloadingProcess = new FileDownloadingProcess();
        downloadingProcess.id = UUID.randomUUID().toString();
        downloadingProcess.fileType = FileType.TYPE_MESSAGE;
        downloadingProcess.message = message;
        downloadingProcess.task = downloadTask;
        downloadingProcess.isCancelled = false;

        addFileDownloadingProcess(downloadingProcess);

        Uri finalSavedFileUri = savedFileUri;
        downloadTask.addOnCompleteListener(new OnCompleteListener<StreamDownloadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@androidx.annotation.NonNull Task<StreamDownloadTask.TaskSnapshot> task) {
                if(task.isSuccessful()) {
                    saveFileFromStream(task.getResult(), message, finalSavedFileUri, downloadingProcess)
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                                compositeDisposable.add(d);
                            }

                            @Override
                            public void onComplete() {
                                if(downloadingProcess.isCancelled) {
                                    return;
                                }

                                message.fileDownloadStatus = FileDownloadStatus.DOWNLOADED;
                                message.localFileUriString = finalSavedFileUri.toString();

                                repository.downloadFileMessage(message)
                                        .subscribe(new CompletableObserver() {
                                            @Override
                                            public void onSubscribe(@NonNull Disposable d) {
                                                compositeDisposable.add(d);
                                            }

                                            @Override
                                            public void onComplete() {
                                                Log.d(TAG, "onComplete: File Downloaded " + message);
                                                removeFileDownloadingProcess(downloadingProcess);
                                            }

                                            @Override
                                            public void onError(@NonNull Throwable e) {
                                                Log.d(TAG, "onError: " + e);
                                                removeFileDownloadingProcess(downloadingProcess);
                                            }
                                        });
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG, "downloadMessageFile: onError: " + e);
                                removeFileDownloadingProcess(downloadingProcess);
                            }
                        });
                }
            }
        });
    }

    private Completable saveFileFromStream(StreamDownloadTask.TaskSnapshot snapshot,
                                           Message message,
                                           Uri finalSavedFileUri,
                                           FileDownloadingProcess downloadingProcess) {
        Completable completable = Completable.fromAction(new Action() {
            @Override
            public void run() throws Throwable {
                try {
                    long totalBytes = snapshot.getTotalByteCount();
                    long bytesDownloaded = 0;
                    long previousTimeInMillis = System.currentTimeMillis();

                    OutputStream outputStream = getContentResolver().openOutputStream(finalSavedFileUri);

                    byte[] buffer = new byte[4 * 1024];
                    int read;

                    while((read = snapshot.getStream().read(buffer)) != -1) {
                        if(downloadingProcess.isCancelled) {
                            outputStream.flush();
                            outputStream.close();
                            throw new Exception("Downloading Process Cancelled");
                        }

                        outputStream.write(buffer, 0, read);
                        bytesDownloaded += read;

                        long currentTimeInMillis = System.currentTimeMillis();
//                        Log.d(TAG, "downloadMessageFile: " + (currentTimeInMillis - previousTimeInMillis));
                        if(currentTimeInMillis - previousTimeInMillis > 100) {
                            message.downloadingPercentage = (int) (bytesDownloaded / totalBytes);
                            message.fileDownloadStatus = FileDownloadStatus.DOWNLOADING;
                            handleFileMessageStatus(message);

                            previousTimeInMillis = currentTimeInMillis;
                        }
                    }

                    outputStream.flush();
                    outputStream.close();
                    Log.d(TAG, "downloadMessageFile: File Streamed");

                }  catch(Exception e) {
                    Log.d(TAG, "downloadMessageFile: " + e);
                }
            }
        });

        return completable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**************************************** FILE MESSAGES ***********************************************/

    private void handleFileMessageStatus(Message message) {
        repository.insertFileMessageIntoLocalDB(message)
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "downloadMessageFile: onComplete: Message Status Updated");
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                    }
                });
    }

    private void updateUserStatus(boolean isOnline) {
        Log.d(TAG, "updateUserStatus: Called");

        ServiceProcess process = new ServiceProcess();
        process.id = UUID.randomUUID().toString();
        process.type = ServiceProcessType.USER_STATUS_UPDATE;

        repository.updateCurrentUserStatus(isOnline)
                .subscribe(new SingleObserver<UserStatus>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                        addServiceProcess(process);
                    }

                    @Override
                    public void onSuccess(@NonNull UserStatus status) {
                        Log.d(TAG, "onSuccess: New User Status -- " + status);
                        currentUserStatus = status;
                        removeServiceProcess(process);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: ", e);
                        removeServiceProcess(process);
                    }
                });
    }

    /************************************** UNSYNCED AND UNREAD ********************************************/

    private void handleUnsyncedChats() {
        ServiceProcess process = new ServiceProcess();
        process.id = UUID.randomUUID().toString();
        process.type = ServiceProcessType.CHAT_SYNC;

        repository.uploadAllUnsyncedChats()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                        addServiceProcess(process);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: All Chats Synced");
                        removeServiceProcess(process);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                        removeServiceProcess(process);
                    }
                });
    }

    private void handleUnsyncedTextMessages() {
        ServiceProcess process = new ServiceProcess();
        process.id = UUID.randomUUID().toString();
        process.type = ServiceProcessType.MESSAGES_SYNC;

        repository.uploadAllUnsyncedTextMessages()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                        addServiceProcess(process);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: All Messages Synced");
                        removeServiceProcess(process);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                        removeServiceProcess(process);
                    }
                });
    }

    private void handleUnreadMessages() {
        // Listening Task, Adding to process list not needed
        Log.d(TAG, "handleUnreadMessages: Called");

        LiveData<List<Message>> unreadMessagesLiveData = LiveDataReactiveStreams.fromPublisher(
                repository.getAllUnreadMessagesFlowable());

        unreadMessagesLiveData.observe(this, new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messageList) {
                Log.d(TAG, "handleUnreadMessages: onChanged: " + messageList);
                List<Message> messagesWithStatusSent = new ArrayList<>();

                for(Message message: messageList) {
                    if(message.status.equals(MessageStatus.SENT)
                            && !message.sentByUserId.equals(CommonVariables.loggedInUser.id)) {
                        messagesWithStatusSent.add(message);
                    }
                }

                if(messagesWithStatusSent.isEmpty()) {
                    return;
                }

                repository.markMessagesAsDelivered(messageList)
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@NonNull Disposable d) {
                                compositeDisposable.add(d);
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "handleUnreadMessages: onComplete: Messages Marked as Delivered");
                            }

                            @Override
                            public void onError(@NonNull Throwable e) {
                                Log.d(TAG, "handleUnreadMessages: onError: " + e);
                            }
                        });
            }
        });
    }

    /************************************** NETWORK AND INTERNET ***********************************************/

    private void addNetworkChangeListener() {
        waitingForNetwork = false;
        networkUtils.registerNetworkChangeListener();

        networkUtils.getNetworkStatusLiveData().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer networkStatus) {
                if(networkStatus == 0) {
                    Log.d(TAG, "onChanged: Network Gone");
                    waitingForNetwork = true;
                    return;
                }

                Log.d(TAG, "onChanged: Network Available");
                if(waitingForNetwork) {
                    onNetworkAvailable();
                }
            }
        });

        Log.d(TAG, "testingLiveData networkLiveData has observers: " + networkUtils.getNetworkStatusLiveData().hasObservers());
        Log.d(TAG, "testingLiveData networkLiveData has active observers: " + networkUtils.getNetworkStatusLiveData().hasActiveObservers());

    }

    private void onNetworkAvailable() {
        Log.d(TAG, "onNetworkAvailable: Called");
        if(!appClosedLiveData.getValue() && !currentUserStatus.isOnline) {
            updateUserStatus(true);
        }

        handleUnsyncedChats();
        handleUnsyncedTextMessages();
    }

    private void setupLiveDataObservers() {
        serviceStatusLiveData = new MediatorLiveData<>();

        serviceStatusLiveData.addSource(appClosedLiveData, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean aBoolean) {
                Log.d(TAG, "onChanged: App Closed");
                serviceStatusLiveData.postValue(getServiceStatus());
            }
        });

        serviceStatusLiveData.addSource(serviceProcessesLiveData, new Observer<List<ServiceProcess>>() {
            @Override
            public void onChanged(List<ServiceProcess> serviceProcesses) {
                Log.d(TAG, "onChanged: Observer Service Processes " + serviceProcesses);
                serviceStatusLiveData.postValue(getServiceStatus());
            }
        });

        serviceStatusLiveData.addSource(fileUploadingProcessesLiveData, new Observer<List<FileUploadingProcess>>() {
            @Override
            public void onChanged(List<FileUploadingProcess> fileUploadingProcesses) {
                Log.d(TAG, "onChanged: Observer File Upload");
                serviceStatusLiveData.postValue(getServiceStatus());
            }
        });

        serviceStatusLiveData.addSource(fileDownloadingProcessesLiveData, new Observer<List<FileDownloadingProcess>>() {
            @Override
            public void onChanged(List<FileDownloadingProcess> fileDownloadingProcesses) {
                Log.d(TAG, "onChanged: Observer File Upload");
                serviceStatusLiveData.postValue(getServiceStatus());
            }
        });

        serviceStatusLiveData.observe(this, new Observer<ServiceStatus>() {
            @Override
            public void onChanged(ServiceStatus serviceStatus) {
                Log.d(TAG, "MUP: MediatorLiveData Updated");
                Log.d(TAG, "MUP: App Closed: " + serviceStatus.appClosed);
                Log.d(TAG, "MUP: Service Processes: " + serviceStatus.serviceProcesses);
                Log.d(TAG, "MUP: File Upload Processes: " + serviceStatus.fileUploadingProcesses);
                Log.d(TAG, "MUP: File Download Processes: " + serviceStatus.fileDownloadingProcesses);

                if(!serviceStatus.appClosed && serviceStatus.fileUploadingProcesses.isEmpty()
                        && serviceStatus.fileDownloadingProcesses.isEmpty()) {
                    endForeground();
                }

                if(serviceStatus.appClosed && serviceStatus.serviceProcesses.isEmpty()
                        && serviceStatus.fileUploadingProcesses.isEmpty()
                        && serviceStatus.fileDownloadingProcesses.isEmpty()) {
                    Log.d(TAG, "Service Status Live Data -- onChanged: Closing Service");
                    terminateService();
                }
            }
        });
    }

    /***************************************** SERVICE UTILITY *************************************************/

    private ServiceStatus getServiceStatus() {
        Log.d(TAG, "getServiceStatus: Called");
        ServiceStatus serviceStatus = new ServiceStatus();

        serviceStatus.appClosed = appClosedLiveData.getValue();
        serviceStatus.serviceProcesses = serviceProcessesLiveData.getValue();
        serviceStatus.fileUploadingProcesses = fileUploadingProcessesLiveData.getValue();
        serviceStatus.fileDownloadingProcesses = fileDownloadingProcessesLiveData.getValue();

        return serviceStatus;
    }

    private void addServiceProcess(ServiceProcess process) {
        serviceProcesses.add(process);
        serviceProcessesLiveData.postValue(serviceProcesses);
    }

    private void removeServiceProcess(ServiceProcess process) {
        serviceProcesses.remove(process);
        serviceProcessesLiveData.postValue(serviceProcesses);
    }

    private void addFileUploadingProcess(FileUploadingProcess process) {
        fileUploadingProcesses.add(process);
        fileUploadingProcessesLiveData.postValue(fileUploadingProcesses);
    }

    private void removeFileUploadingProcess(FileUploadingProcess process) {
        fileUploadingProcesses.remove(process);
        fileUploadingProcessesLiveData.postValue(fileUploadingProcesses);
    }

    private void addFileDownloadingProcess(FileDownloadingProcess process) {
        fileDownloadingProcesses.add(process);
        fileDownloadingProcessesLiveData.postValue(fileDownloadingProcesses);
    }

    private void removeFileDownloadingProcess(FileDownloadingProcess process) {
        fileDownloadingProcesses.remove(process);
        fileDownloadingProcessesLiveData.postValue(fileDownloadingProcesses);
    }

    public FileUploadingProcess getUploadingProcess(FileType fileType, String fileId) {
        for(FileUploadingProcess process: fileUploadingProcesses) {
            if(process.fileType == fileType) {
                switch(process.fileType) {
                    case TYPE_MESSAGE:
                        if(process.message.id.equals(fileId)) {
                            return process;
                        }
                        break;

                    case TYPE_SCANNED_DOCUMENT:
                        break;

                    case TYPE_PAINT_IMAGE:
                        break;
                }
            }
        }

        return null;
    }

    public FileDownloadingProcess getDownloadingProcess(FileType fileType, String fileId) {
        for(FileDownloadingProcess process: fileDownloadingProcesses) {
            if(process.fileType == fileType) {
                switch(process.fileType) {
                    case TYPE_MESSAGE:
                        if(process.message.id.equals(fileId)) {
                            return process;
                        }
                        break;

                    case TYPE_SCANNED_DOCUMENT:
                        break;

                    case TYPE_PAINT_IMAGE:
                        break;
                }
            }
        }

        return null;
    }

    /******************************************* FOREGROUND SERVICE ******************************************/

    private void makeServiceForeground(Notification notification) {
        if(isForeground) {
            Log.d(TAG, "makeServiceForeground: Already in foreground");
            return;
        }

        isForeground = true;
        startForeground(FOREGROUND_NOTIFICATION_ID, notification);
    }

    private void endForeground() {
        if(!isForeground) {
            Log.d(TAG, "endForeground: Already in background");
            return;
        }

        isForeground = false;
        stopForeground(true);
    }

    /******************************************* SERVICE NOTIFICATIONS ******************************************/

    private Notification getAppClosingNotification() {
        return new NotificationCompat.Builder(
                this, NOTIFICATION_CHANNEL_LOW)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_app_vector_logo)
                .setContentText("Closing app..")
                .build();
    }

    private Notification getUploadDownloadNotification(String title) {
        Intent cancelIntent = new Intent(this, IntouchService.class);
        cancelIntent.setAction(ACTION_STOP_ALL_UPLOAD_AND_DOWNLOAD);

        PendingIntent cancelPendingIntent = PendingIntent.getService(this, 0 ,
                cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(
                this, NOTIFICATION_CHANNEL_LOW)
                .setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_app_vector_logo)
                .setContentTitle(title)
                .setProgress(0, 0, true)
                .addAction(R.drawable.ic_app_vector_logo, "Cancel", cancelPendingIntent)
                .build();
    }

    /*************************************** ON TASK REMOVED AND ON DESTROY **************************************/

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved: Called");

        appClosedLiveData.postValue(true);
        makeServiceForeground(getAppClosingNotification());
        updateUserStatus(false);

        // TODO: Make user offline and execute any other code which
        //  should be done when app is removed from recent apps
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Called");

        networkUtils.removeNetworkChangeListener();
        compositeDisposable.clear();
        isRunning = false;
    }

    /*************************************** SERVICE CLASSES AND ENUMS **************************************/

    public static class ServiceStatus {
        public boolean appClosed;
        public List<ServiceProcess> serviceProcesses;
        public List<FileUploadingProcess> fileUploadingProcesses;
        public List<FileDownloadingProcess> fileDownloadingProcesses;
    }

    public static class ServiceProcess {
        public String id;
        public ServiceProcessType type;

        @Override
        public String toString() {
            return "ServiceProcess{" +
                    "id='" + id + '\'' +
                    ", type=" + type +
                    '}';
        }
    }

    public static class FileUploadingProcess {
        public String id;
        public FileType fileType;
        public Message message;
        public IntouchDocument document;
        public IntouchImage image;
        public UploadTask task;
        public OnProgressListener<UploadTask.TaskSnapshot> progressListener;
    }

    public static class FileDownloadingProcess {
        public String id;
        public FileType fileType;
        public Message message;
        public IntouchDocument document;
        public IntouchImage image;
        public StreamDownloadTask task;
        public boolean isCancelled;
    }

    public enum ServiceProcessType {
        CHAT_SYNC,
        MESSAGES_SYNC,
        USER_STATUS_UPDATE
    }

    public enum FileType {
        TYPE_MESSAGE,
        TYPE_SCANNED_DOCUMENT,
        TYPE_PAINT_IMAGE
    }
}
