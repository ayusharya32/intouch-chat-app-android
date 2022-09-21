package com.easycodingg.intouch.ui.chat;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.easycodingg.intouch.R;
import com.easycodingg.intouch.adapters.MessageAdapter;
import com.easycodingg.intouch.databinding.DialogSendFilesBinding;
import com.easycodingg.intouch.databinding.DialogStoragePermissionBinding;
import com.easycodingg.intouch.databinding.FragmentChatBinding;
import com.easycodingg.intouch.db.IntouchRepository;
import com.easycodingg.intouch.models.Chat;
import com.easycodingg.intouch.models.Message;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.models.UserStatus;
import com.easycodingg.intouch.services.IntouchService;
import com.easycodingg.intouch.ui.imageviewer.ImageViewerFragment;
import com.easycodingg.intouch.utils.CommonMethods;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.IntouchErrorHandler;
import com.easycodingg.intouch.utils.LoadingDialogFragment;
import com.easycodingg.intouch.utils.PermissionUtils;
import com.easycodingg.intouch.utils.enums.FileDownloadStatus;
import com.easycodingg.intouch.utils.enums.FileUploadStatus;
import com.easycodingg.intouch.utils.enums.MessageStatus;
import com.easycodingg.intouch.utils.enums.MessageType;
import com.easycodingg.intouch.utils.events.LeftMessageItemClickEvent;
import com.easycodingg.intouch.utils.events.OnDocSnapshotListenerTriggered;
import com.easycodingg.intouch.utils.events.OnQuerySnapshotListenerTriggered;
import com.easycodingg.intouch.utils.events.RightMessageItemClickEvent;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragmentyy";

    private FragmentChatBinding binding;

    private FirebaseFirestore db;
    private IntouchRepository repository;

    private MessageAdapter messageAdapter;
    private Chat chat;
    private User otherUser;
    private boolean wasTyping, messageSent;
    private Handler typingHandler;

    private MessageType currentFileMessageType;

    private PermissionUtils permissionUtils;

    private ListenerRegistration chatMessagesListener, userStatusListener, chatListener;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = IntouchRepository.getInstance(getContext());
        db = FirebaseFirestore.getInstance();
        permissionUtils = PermissionUtils.getInstance(getContext());

        chat = com.easycodingg.intouch.ui.chat.ChatFragmentArgs.fromBundle(getActivity().getIntent().getExtras()).getChat();
        otherUser = com.easycodingg.intouch.ui.chat.ChatFragmentArgs.fromBundle(getActivity().getIntent().getExtras()).getUser();

        messageSent = false;

        Log.d(TAG, "onViewCreated: Chat -- " + chat);
        Log.d(TAG, "onViewCreated: User -- " + otherUser);

        setupMessagesRecyclerView();

        if(otherUser != null) {
            setupUserDetails(otherUser);
            checkIfChatIsAlreadyPresent();
        }

        if(chat != null) {
            otherUser = chat.otherUser;
            setupUserDetails(otherUser);
            performActionsOnChat();
        }

        observeUserStatus();
        getRealtimeUserStatusUpdates();

//        addMenu();
        setupMessageEditTextListeners();
        setupOnClickListeners();
        setupLastSeenTextView();
    }

    private void performActionsOnChat() {
        observeChatMessages();
        getRealtimeChatMessages();
        observeCurrentChat();
        getRealtimeChatUpdates();
    }

    private void setupLastSeenTextView() {
        binding.txtUserOnlineStatus.setSelected(true);
    }

    private void checkIfChatIsAlreadyPresent() {
        repository.getChatByUsers(otherUser)
                .subscribe(new MaybeObserver<Chat>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull Chat chat) {
                        ChatFragment.this.chat = chat;
                        performActionsOnChat();
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d(TAG, "onError " + e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Chat not found");
                    }
                });
    }

    private void setupMessageEditTextListeners() {
        typingHandler = new Handler();

        binding.etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                Log.d(TAG, "beforeTextChanged: ");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String message = binding.etMessage.getText().toString().trim();
                binding.btnSendMessage.setEnabled(!message.isEmpty());
                Log.d(TAG, "onTextChanged: ");
            }

            @Override
            public void afterTextChanged(Editable s) {
                Log.d(TAG, "afterTextChanged: " + s.toString());

                if(s.toString().isEmpty()) {
                    return;
                }

                if(!wasTyping) {
                    Log.d(TAG, "afterTextChanged: Setting Typing true");
                    wasTyping = true;
                    toggleUserTypingStatus();
                }

                typingHandler.removeCallbacksAndMessages(null);

                typingHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "afterTextChanged: typingHandler: Calling Typing Post Delayed");

                        if(wasTyping) {
                            wasTyping = false;
                            toggleUserTypingStatus();
                        }
                    }
                }, 1000);
            }
        });
    }

    private void toggleUserTypingStatus() {
        repository.toggleUserTypingStatus(chat)
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Typing");
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d(TAG, "OnError: " + e);
                    }
                });
    }

    private void setupOnClickListeners() {
        binding.btnSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = binding.etMessage.getText().toString().trim();

                if(message.isEmpty()) {
                    return;
                }

                if(otherUser == null) {
                    return;
                }

                if(chat == null) {
                    createChat(message);
                    return;
                }

                sendMessage(message);
            }
        });

        binding.btnSendFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSendFilesDialog();
            }
        });
    }

    private void setupMessagesRecyclerView() {
        messageAdapter = new MessageAdapter(new LeftMessageItemClickEvent() {
            @Override
            public void onItemClick(Message message) {
                if(message.type == MessageType.TYPE_TEXT) {
                    return;
                }

                if(message.fileDownloadStatus == FileDownloadStatus.DOWNLOADED
                        && message.localFileUriString != null) {
                    openMessageFile(message);

                } else {
                    checkPermissionsAndDownloadMessageFile(message);
                }
            }

            @Override
            public void onDownloadButtonClick(Message message) {
                checkPermissionsAndDownloadMessageFile(message);
            }

            @Override
            public void onDownloadCancelButtonClick(Message message) {
                cancelMessageFileDownload(message);
            }

        }, new RightMessageItemClickEvent() {
            @Override
            public void onItemClick(Message message) {
                if(message.type == MessageType.TYPE_TEXT) {
                    return;
                }

                if(message.localFileUriString != null) {
                    openMessageFile(message);
                }

            }

            @Override
            public void onUploadButtonClick(Message message) {
                if(message.localFileUriString != null) {
                    sendMessageFilesForUploading(Collections.singletonList(message));

                } else {
                    Toast.makeText(getContext(), "Unable to send file", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onUploadCancelButtonClick(Message message) {
                cancelMessageFileUpload(message);
            }
        });

        binding.rvMessages.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvMessages.setAdapter(messageAdapter);

        binding.rvMessages.setItemAnimator(null);

        binding.rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                Log.d(TAG, "onScrollStateChanged: messageSent: " + messageSent);
                messageSent = false;
            }
        });
    }

    private void openMessageFile(Message message) {
        Log.d(TAG, "openFile: Called" + message);
        if(message.localFileUriString == null) {
            return;
        }

        if(message.type == MessageType.TYPE_IMAGE) {
            Fragment imageViewerFragment = new ImageViewerFragment();

            Bundle bundle = new Bundle();
            bundle.putSerializable("message", message);
            imageViewerFragment.setArguments(bundle);

            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.hide(this);
            transaction.add(R.id.nav_host_fragment, imageViewerFragment);
            transaction.addToBackStack(null);
            transaction.commit();

        } else if(message.type == MessageType.TYPE_DOCUMENT) {
            Uri contentUri = !message.localFileUriString.contains("content://") ?
                    FileProvider.getUriForFile(getContext(),
                    getContext().getPackageName() + ".provider",
                    new File(Uri.parse(message.localFileUriString).getPath())) :
                    Uri.parse(message.localFileUriString);

            Log.d(TAG, "openMessageFile: " + contentUri);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(contentUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, ""));
        }
    }

    private void checkPermissionsAndDownloadMessageFile(Message message) {
        if(!permissionUtils.readExternalStoragePermissionGranted() ||
                !permissionUtils.writeExternalStoragePermissionGranted()) {
            showStoragePermissionDialog();
            return;
        }

        sendMessageFileForDownloading(message);
    }

    private void sendMessageFileForDownloading(Message message) {
        Intent intent = new Intent(getContext(), IntouchService.class);
        intent.setAction(IntouchService.ACTION_DOWNLOAD_FILES);
        intent.putExtra(IntouchService.KEY_FILE_TYPE, IntouchService.FileType.TYPE_MESSAGE);
        intent.putExtra(IntouchService.KEY_DOWNLOADING_FILES_LIST, (Serializable) Collections.singletonList(message));
        getContext().startService(intent);
    }

    private void cancelMessageFileDownload(Message message) {
        Intent intent = new Intent(getContext(), IntouchService.class);
        intent.setAction(IntouchService.ACTION_STOP_FILE_DOWNLOAD);
        intent.putExtra(IntouchService.KEY_FILE_TYPE, IntouchService.FileType.TYPE_MESSAGE);
        intent.putExtra(IntouchService.KEY_DOWNLOADING_FILE, message);
        getContext().startService(intent);
    }

    private void createChat(String messageContent) {
        chat = getNewChatInstance();

        repository.createChat(chat)
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Chat Created Successfully");
                        performActionsOnChat();
                        sendMessage(messageContent);
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        String errorMessage = e instanceof IntouchErrorHandler ? e.getMessage() : "Some error occurred";
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendMessage(String messageContent) {
        Message message = new Message();

        message.id = UUID.randomUUID().toString();
        message.chatId = chat.id;
        message.users = chat.users;
        message.content = messageContent;
        message.sentByUserId = CommonVariables.loggedInUser.id;
        message.type = MessageType.TYPE_TEXT;
        message.status = MessageStatus.WAITING_TO_SEND;
        message.sendingTime = Calendar.getInstance().getTime();

        message.isSynced = false;
        message.orderTimestamp = message.sendingTime;

        repository.createMessage(message)
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Message Sent Successfully");
                        binding.etMessage.setText("");
                        messageSent = true;
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        String errorMessage = e instanceof IntouchErrorHandler ? e.getMessage() : "Some error occurred";
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void observeChatMessages() {
        getChatMessagesLiveData().observe(getViewLifecycleOwner(), new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messageList) {
                Log.d(TAG, "onChanged: " + messageList.toString());
                binding.txtNewChatGreeting.setVisibility(messageList.size() > 0 ? View.GONE : View.VISIBLE);

                Log.d(TAG, "ChatFragmentMessages: " + messageList);

                boolean itemsAddedToList = messageAdapter.getCurrentList().isEmpty() ||
                        (messageAdapter.getCurrentList().size() != messageList.size());

                messageAdapter.submitList(messageList, new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "onScrollStateChanged: itemsAdded " + itemsAddedToList);
                        Log.d(TAG, "onScrollStateChanged: listSize " + messageList.size());

                        if(itemsAddedToList || messageSent) {
                            binding.rvMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });

                List<Message> receivedMessagesWithDeliveredStatus = new ArrayList<>();

                for(Message message: messageList) {
                    if(message.status.equals(MessageStatus.DELIVERED)
                            && !message.sentByUserId.equals(CommonVariables.loggedInUser.id)) {
                        receivedMessagesWithDeliveredStatus.add(message);
                    }
                }

                if(!receivedMessagesWithDeliveredStatus.isEmpty()) {
                    handleReadingMessages(receivedMessagesWithDeliveredStatus);
                }
            }
        });
    }

    private LiveData<List<Message>> getChatMessagesLiveData() {
        return LiveDataReactiveStreams.fromPublisher(repository.getChatMessagesFlowable(chat.id));
    }

    private void getRealtimeChatMessages() {
        chatMessagesListener = repository.getChatMessagesListener(chat.id, new OnQuerySnapshotListenerTriggered() {
            @Override
            public void onListenerTriggered(QuerySnapshot querySnapshot) {
                repository.handleChatMessages(chat, querySnapshot)
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                                compositeDisposable.add(d);
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: " + "Messages Updated");
                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                Log.d(TAG, "onError: " + e);
                            }
                        });
            }
        });
    }

    private void observeUserStatus() {
        getUserStatusLiveData().observe(getViewLifecycleOwner(), new Observer<UserStatus>() {
            @Override
            public void onChanged(UserStatus status) {
                showUserStatus(status);
            }
        });
    }

    private LiveData<UserStatus> getUserStatusLiveData() {
        return LiveDataReactiveStreams.fromPublisher(repository.getUserStatusFlowable(otherUser.id));
    }

    private void getRealtimeUserStatusUpdates() {
        userStatusListener = repository.getUserStatusListener(otherUser.id, new OnDocSnapshotListenerTriggered() {
            @Override
            public void onListenerTriggered(DocumentSnapshot documentSnapshot) {
                repository.handleUserStatus(documentSnapshot)
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                                compositeDisposable.add(d);
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: User Status");
                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                Log.d(TAG, "onError: " + e);
                            }
                        });
            }
        });
    }

    private void observeCurrentChat() {
        getChatLiveData().observe(getViewLifecycleOwner(), new Observer<Chat>() {
            @Override
            public void onChanged(Chat chat) {
                if(chat.typing.contains(otherUser.id)) {
                    showTyping();
                } else {
                    hideTyping();
                }
            }
        });
    }

    private LiveData<Chat> getChatLiveData() {
        return LiveDataReactiveStreams.fromPublisher(repository.getChatFlowable(chat.id));
    }

    private void getRealtimeChatUpdates() {
        chatListener = repository.getChatListener(chat.id, new OnDocSnapshotListenerTriggered() {
            @Override
            public void onListenerTriggered(DocumentSnapshot documentSnapshot) {
                repository.handleChatUpdates(documentSnapshot, chat)
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                                compositeDisposable.add(d);
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: Chat Listener");
                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                Log.d(TAG, "onError: " + e);
                            }
                        });
            }
        });
    }

    private void handleReadingMessages(List<Message> receivedMessagesWithDeliveredStatus) {
        Log.d(TAG, "handleReadingMessages: Called " + receivedMessagesWithDeliveredStatus);

        repository.markMessagesAsRead(receivedMessagesWithDeliveredStatus)
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "handleReadingMessages: onComplete");
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d(TAG, "handleReadingMessages: onError " + e);
                    }
                });
    }

    private Chat getNewChatInstance() {
        Chat chat = new Chat();

        chat.id = UUID.randomUUID().toString();
        chat.users = Arrays.asList(CommonVariables.loggedInUser.id, otherUser.id);
        chat.typing = Collections.emptyList();
        chat.isRequestAccepted = false;
        chat.isPrivate = false;
        chat.createdAt = Calendar.getInstance().getTime();
        chat.isSynced = false;

        return chat;
    }

    private void setupUserDetails(User user) {
        if(user.profileImageUrl != null) {
            Glide.with(getContext())
                    .load(user.profileImageUrl)
                    .into(binding.imgUser);
        } else {
            binding.imgUser.setImageResource(R.drawable.ic_user);
        }

        binding.txtUserName.setText(user.name);

        showNewChatGreeting();
    }

    private void showUserStatus(UserStatus status) {
        String userStatusString = "";

        if(status.isOnline) {
            userStatusString = "Online";
        } else {
            if(status.lastSeenTime != null) {
                String lastSeenTimeString = CommonMethods.getLastSeenTime(status.lastSeenTime);

                if(!lastSeenTimeString.isEmpty()) {
                    userStatusString = "Last seen " + lastSeenTimeString;

                } else {
                    binding.txtUserOnlineStatus.setVisibility(View.GONE);
                }
            }
        }

        binding.txtUserOnlineStatus.setText(userStatusString);
    }

    private void showTyping() {
        binding.txtUserTyping.setText(otherUser.name + " is typing...");

        binding.txtUserTyping.setVisibility(View.VISIBLE);
        binding.txtUserTyping.setAlpha(0);
        binding.txtUserTyping.setTranslationY(-binding.txtUserTyping.getHeight() * 2);

        binding.txtUserTyping.animate()
                .translationY(0)
                .alpha(1);
    }

    private void hideTyping() {
        Log.d(TAG, "hideTyping: Called");
        binding.txtUserTyping.setVisibility(View.GONE);
    }

    private void showNewChatGreeting() {
        String greeting = "Welcome <b>" + CommonVariables.loggedInUser.name + "</b>, " +
                "Say Hii to <b>" + otherUser.name + "</b>";
        binding.txtNewChatGreeting.setText(Html.fromHtml(greeting));

        binding.txtNewChatGreeting.setVisibility(View.VISIBLE);
    }

    private void hideNewChatGreeting() {
        binding.txtNewChatGreeting.setVisibility(View.GONE);
    }

    private void addMenu() {
        binding.toolbar.inflateMenu(R.menu.home_menu);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if(userStatusListener != null) {
            userStatusListener.remove();
        }

        if(chatMessagesListener != null) {
            chatMessagesListener.remove();
        }

        if(chatListener != null) {
            chatListener.remove();
        }

        compositeDisposable.clear();
        binding = null;
    }

    ActivityResultLauncher<Intent> pickFilesToSendLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if(result.getData() == null) {
                        Log.d(TAG, "onActivityResult: No result");
                        return;
                    }

                    Intent resultIntent = result.getData();
                    List<Uri> selectedFiles = new ArrayList<>();

                    if(resultIntent.getData() != null) {
                        // Single File Picked
                        selectedFiles.add(resultIntent.getData());

                    } else if(resultIntent.getClipData().getItemCount() != 0) {
                        // Multiple Files Picked

                        for(int i = 0; i < resultIntent.getClipData().getItemCount(); i++) {
                            Uri fileUri = resultIntent.getClipData().getItemAt(i).getUri();
                            selectedFiles.add(fileUri);
                        }
                    }

                    Log.d(TAG, "onActivityResult: " + selectedFiles);
                    sendFiles(selectedFiles);
                }
            }
    );

    private void sendFiles(List<Uri> selectedFiles) {
        if(selectedFiles.isEmpty()) {
            Log.d(TAG, "sendFiles: No files to send");
            return;
        }

        if(selectedFiles.size() > 50) {
            Toast.makeText(getContext(), "Limit exceeded, you can only send 20 files at a time",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        prepareMessagesAndSendToUpload(selectedFiles);
    }

    private void prepareMessagesAndSendToUpload(List<Uri> selectedFiles) {
        LoadingDialogFragment loadingDialog = LoadingDialogFragment
                .getLoadingDialogFragmentInstance("Preparing to send...");

        Single.fromCallable(new Callable<List<Message>>() {
                    @Override
                    public List<Message> call() throws Exception {
                        List<Message> fileMessagesToSend = getFileMessagesFromSelectedUriList(selectedFiles);
                        Log.d(TAG, "sendFiles: " + fileMessagesToSend);
                        Log.d(TAG, "sendFiles: " + Thread.currentThread().getName());

                        repository.insertFileMessagesIntoLocalDB(fileMessagesToSend).blockingAwait();

                        return fileMessagesToSend;
                    }
                }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<Message>>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                        loadingDialog.show(getChildFragmentManager(), "Loading");
                    }

                    @Override
                    public void onSuccess(List<Message> messageList) {
                        Log.d(TAG, "sendFiles: " + "fileCompressionCompleted" + Thread.currentThread().getName());
                        loadingDialog.dismiss();
                        sendMessageFilesForUploading(messageList);
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                        Toast.makeText(getContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                        loadingDialog.dismiss();
                    }
                });
    }

    private List<Message> getFileMessagesFromSelectedUriList(List<Uri> selectedFiles) {
        List<Message> fileMessagesToSend = new ArrayList<>();

        for(Uri uri: selectedFiles) {
            Message message = new Message();

            message.id = UUID.randomUUID().toString();
            message.chatId = chat.id;
            message.users = chat.users;

            message.content = currentFileMessageType == MessageType.TYPE_DOCUMENT
                    ? CommonMethods.getFileNameFromUri(getContext(), uri) : "";

            message.sentByUserId = CommonVariables.loggedInUser.id;
            message.type = currentFileMessageType;
            message.status = MessageStatus.WAITING_TO_SEND;
            message.sendingTime = Calendar.getInstance().getTime();

            // File
            message.fileUploadStatus = currentFileMessageType == MessageType.TYPE_IMAGE ?
                    FileUploadStatus.PREPARING : FileUploadStatus.NOT_UPLOADING;
            message.uploadingPercentage = 0;

            if(currentFileMessageType == MessageType.TYPE_IMAGE) {
                Uri imageUri = getCompressedImageUri(uri);
                message.localFileUriString = imageUri == null ? uri.toString() : imageUri.toString();

            } else if(currentFileMessageType == MessageType.TYPE_DOCUMENT) {
                Uri documentUri = getFileUriFromContentUriAndroid(uri);
                message.localFileUriString =  documentUri == null ? uri.toString() : documentUri.toString();
            }

            message.isSynced = false;
            message.orderTimestamp = message.sendingTime;

            fileMessagesToSend.add(message);
        }

        return fileMessagesToSend;
    }

    private Uri getFileUriFromContentUriAndroid(Uri uri) {
        try {
            String fileType = CommonMethods.getFileExtensionFromContentUri(getContext(), uri);
            String fileName = "DOC" + CommonMethods.getFormattedDateTime(Calendar.getInstance().getTime(),
                    "yyyyMMddHHmmssSSS");

            String filePath = getContext().getExternalFilesDir("sent/documents") + "/" +
                    fileName + "." + fileType;
            Log.d(TAG, "getCompressedImageUri: " + filePath);

            File file = new File(filePath);

            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4 * 1024];
            int read;

            while((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            inputStream.close();

            return Uri.fromFile(file);

        } catch(Exception e) {
            Log.d(TAG, "getFileUriFromContentUriAndroid: " + e);
        }

        return null;
    }

    private Uri getCompressedImageUri(Uri imageUri) {
        try {
            Bitmap bitmap = CommonMethods.getImageBitmapFromUri(getContext(), imageUri);

            String fileName = "IMG" + CommonMethods.getFormattedDateTime(Calendar.getInstance().getTime(),
                    "yyyyMMddHHmmssSSS");
            String filePath = getContext().getExternalFilesDir("sent/images") + "/" + fileName + ".jpg";
            Log.d(TAG, "getCompressedImageUri: " + filePath);

            File file = new File(filePath);

            FileOutputStream outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            outputStream.flush();
            outputStream.close();

            return Uri.fromFile(file);

        } catch(Exception e) {
            Log.d(TAG, "getCompressedImageUri: " + e);
        }

        return null;
    }

    private void sendMessageFilesForUploading(List<Message> fileMessagesToSend) {
        messageSent = true;

        Intent intent = new Intent(getContext(), IntouchService.class);
        intent.setAction(IntouchService.ACTION_UPLOAD_FILES);
        intent.putExtra(IntouchService.KEY_FILE_TYPE, IntouchService.FileType.TYPE_MESSAGE);
        intent.putExtra(IntouchService.KEY_UPLOADING_FILES_LIST, (Serializable) fileMessagesToSend);
        getContext().startService(intent);
    }

    private void cancelMessageFileUpload(Message message) {
        Intent intent = new Intent(getContext(), IntouchService.class);
        intent.setAction(IntouchService.ACTION_STOP_FILE_UPLOAD);
        intent.putExtra(IntouchService.KEY_FILE_TYPE, IntouchService.FileType.TYPE_MESSAGE);
        intent.putExtra(IntouchService.KEY_UPLOADING_FILE, message);
        getContext().startService(intent);
    }

    /***************************************** DIALOGS ********************************************/
    private void showSendFilesDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());

        DialogSendFilesBinding dialogBinding = DialogSendFilesBinding.inflate(getLayoutInflater(),
                binding.getRoot(), false);
        dialogBuilder.setView(dialogBinding.getRoot());

        AlertDialog sendFilesDialog = dialogBuilder.create();
        sendFilesDialog.setCancelable(true);

        dialogBinding.btnSendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFilesDialog.dismiss();
                currentFileMessageType = MessageType.TYPE_IMAGE;
                pickFilesFromStorage("image/*");
            }
        });

        dialogBinding.btnSendDocument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendFilesDialog.dismiss();
                currentFileMessageType = MessageType.TYPE_DOCUMENT;
                pickFilesFromStorage("*/*");
            }
        });

        if (sendFilesDialog.getWindow() != null) {
            sendFilesDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getActivity().getWindow().setGravity(Gravity.BOTTOM);
            sendFilesDialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
        }

        sendFilesDialog.show();
    }

    private void pickFilesFromStorage(String type) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(type);

        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        pickFilesToSendLauncher.launch(intent);
    }

    private void showStoragePermissionDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());

        DialogStoragePermissionBinding dialogBinding = DialogStoragePermissionBinding.inflate(getLayoutInflater(),
                binding.getRoot(), false);
        dialogBuilder.setView(dialogBinding.getRoot());

        List<String> deniedPermissions = permissionUtils.getDeniedPermissionsList();
        boolean permissionPermanentlyDenied = deniedPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                deniedPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        Log.d(TAG, "showStoragePermissionDialog: deniedPermissions: " + deniedPermissions);
        Log.d(TAG, "showStoragePermissionDialog: permissionPermanentlyDenied: " + permissionPermanentlyDenied);

        if(permissionPermanentlyDenied) {
            Log.d(TAG, "showStoragePermissionDialog: Permanent Denial");
            dialogBinding.btnContinue.setText("Settings");
            dialogBinding.txtInfo.setText(R.string.storage_permission_needed_after_permanent_denial);
        }

        AlertDialog storagePermissionDialog = dialogBuilder.create();
        storagePermissionDialog.setCancelable(false);

        dialogBinding.btnNotNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storagePermissionDialog.dismiss();
            }
        });

        dialogBinding.btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                storagePermissionDialog.dismiss();

                if(permissionPermanentlyDenied) {
                    openAppPermissionSettings();
                    return;
                }

                checkRequiredStoragePermissionsAndRequest();
            }
        });

        storagePermissionDialog.show();
    }

    private void checkRequiredStoragePermissionsAndRequest() {
        List<String> permissionsToRequest = new ArrayList<>();

        if(!permissionUtils.readExternalStoragePermissionGranted()) {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if(!permissionUtils.writeExternalStoragePermissionGranted()) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        requestStoragePermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
    }

    private void openAppPermissionSettings() {
        appPermissionSettingsLauncher.launch(permissionUtils.getAppLocationSettingsIntent());
    }

    private final ActivityResultLauncher<String[]> requestStoragePermissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {
                @Override
                public void onActivityResult(Map<String, Boolean> result) {
                    if(permissionUtils.readExternalStoragePermissionGranted()
                            && permissionUtils.writeExternalStoragePermissionGranted()) {

                        // TODO: Execute storage feature
                        return;
                    }

                    if(!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                            !permissionUtils.readExternalStoragePermissionGranted()) {
                        permissionUtils.setPermissionPermanentlyDenied(Manifest.permission.READ_EXTERNAL_STORAGE);
                    }

                    if(!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                            !permissionUtils.writeExternalStoragePermissionGranted()) {
                        permissionUtils.setPermissionPermanentlyDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> appPermissionSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    permissionUtils.updateDeniedPermissionsList();

                    Log.d(TAG, "onActivityResult: " + permissionUtils.getDeniedPermissionsList());

                    if(permissionUtils.readExternalStoragePermissionGranted()
                            && permissionUtils.writeExternalStoragePermissionGranted()) {

                        // TODO: Execute storage feature
                    }
                }
            }
    );

}
