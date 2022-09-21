package com.easycodingg.intouch.ui.mychats;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.easycodingg.intouch.R;
import com.easycodingg.intouch.adapters.ChatAdapter;
import com.easycodingg.intouch.baseactivity.ChatActivity;
import com.easycodingg.intouch.databinding.DialogNewChatBinding;
import com.easycodingg.intouch.databinding.FragmentMyChatsBinding;
import com.easycodingg.intouch.db.IntouchRepository;
import com.easycodingg.intouch.models.Chat;
import com.easycodingg.intouch.models.Message;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.Constants;
import com.easycodingg.intouch.utils.IntouchErrorHandler;
import com.easycodingg.intouch.utils.events.ChatItemClickEvent;
import com.easycodingg.intouch.utils.events.OnQuerySnapshotListenerTriggered;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class MyChatsFragment extends Fragment {
    private static final String TAG = "MyChatsFragmentyyy";

    private FragmentMyChatsBinding binding;

    private FirebaseFirestore db;
    private IntouchRepository repository;

    private ChatAdapter chatAdapter;
    private AlertDialog newChatDialog;

    private List<Chat> currentChatList;
    private List<Message> totalUnreadMessageList;
    private ListenerRegistration userChatsListener, unreadMessagesListener;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Called");

        binding = FragmentMyChatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Called");

        db = FirebaseFirestore.getInstance();
        repository = IntouchRepository.getInstance(getContext());
        currentChatList = new ArrayList<>();
        totalUnreadMessageList = new ArrayList<>();

        setupNewChatDialog();
        setupOnClickListeners();
        setupChatsRecyclerView();

        observeChatsLiveData();
        observeUnreadMessages();
        handleRealtimeEvents();
    }

    private void setupChatsRecyclerView() {
        chatAdapter = new ChatAdapter(currentChatList, new ChatItemClickEvent() {
            @Override
            public void onItemClick(Chat chat) {
                Intent intent = new Intent(getContext(), ChatActivity.class);
                intent.putExtra(Constants.KEY_CHAT, chat);
                startActivity(intent);
            }
        });

        binding.rvChats.setLayoutManager(new LinearLayoutManager(getContext()));

        SimpleItemAnimator animator = (SimpleItemAnimator) binding.rvChats.getItemAnimator();
        if(animator != null) {
            animator.setSupportsChangeAnimations(false);
        }

        binding.rvChats.setAdapter(chatAdapter);
    }

    private void observeChatsLiveData() {
        getUserChatsLiveData().observe(getViewLifecycleOwner(), new Observer<List<Chat>>() {
            @Override
            public void onChanged(List<Chat> chatList) {
                if(chatList.isEmpty()) {
                    binding.llNoChats.setVisibility(View.VISIBLE);

                } else {
                    currentChatList = chatList;
//                putUnreadMessagesWithChats();

                    getChatExtraDetails(chatList);
                    binding.llNoChats.setVisibility(View.GONE);
                }
            }
        });

        Log.d(TAG, "testingLiveData: mychats has observers: " + getUserChatsLiveData().hasObservers());
        Log.d(TAG, "testingLiveData: mychats has active observers: " + getUserChatsLiveData().hasActiveObservers());
    }

    private LiveData<List<Chat>> getUserChatsLiveData() {
        return LiveDataReactiveStreams.fromPublisher(repository.getUserChatsFlowable());
    }

    private void observeUnreadMessages() {
        getUnreadMessagesLiveData().observe(getViewLifecycleOwner(), new Observer<List<Message>>() {
            @Override
            public void onChanged(List<Message> messageList) {
                totalUnreadMessageList = messageList;

                if(!chatAdapter.chatList.isEmpty()) {
                    putUnreadMessagesWithChats();
                }
            }
        });
    }

    private LiveData<List<Message>> getUnreadMessagesLiveData() {
        return LiveDataReactiveStreams.fromPublisher(repository.getAllUnreadMessagesFlowable());
    }

    private void handleRealtimeEvents() {
        userChatsListener = repository.getUserChatsListener(new OnQuerySnapshotListenerTriggered() {
            @Override
            public void onListenerTriggered(QuerySnapshot querySnapshot) {
                repository.handleUserChatsUpdates(querySnapshot)
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                                compositeDisposable.add(d);
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: " + "Chat Added");
                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                Log.d(TAG, "onError: " + e);
                            }
                        });
            }
        });

        unreadMessagesListener = repository.getUnreadMessagesListener(new OnQuerySnapshotListenerTriggered() {
            @Override
            public void onListenerTriggered(QuerySnapshot querySnapshot) {
                repository.handleUnreadMessages(querySnapshot)
                        .subscribe(new CompletableObserver() {
                            @Override
                            public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                                compositeDisposable.add(d);
                            }

                            @Override
                            public void onComplete() {
                                Log.d(TAG, "onComplete: " + "Unread Message Added");
                            }

                            @Override
                            public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                                Log.d(TAG, "onError: " + e);
                            }
                        });
            }
        });
    }

    private void getChatExtraDetails(List<Chat> chatList) {
        Log.d(TAG, "getChatExtraDetails: Called " + chatList);

        repository.getChatExtraDetails(chatList)
                .subscribe(new io.reactivex.rxjava3.core.Observer<Chat>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        Log.d(TAG, "getChatExtraDetails: Subscribed");
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onNext(@io.reactivex.rxjava3.annotations.NonNull Chat chat) {
                        for(Chat currentChat: chatList) {
                            if(currentChat.id.equals(chat.id)) {
                                currentChat = chat;
                                break;
                            }
                        }

                        Log.d(TAG, "getChatExtraDetails -- onNext: " + chatList);

//                        Collections.sort(currentChatList, new Comparator<Chat>() {
//                            @Override
//                            public int compare(Chat item1, Chat item2) {
//                                if(item1.lastMessage == null || item2.lastMessage == null) {
//                                    return 0;
//                                }
//                                return item2.lastMessage.sendingTime.compareTo(item1.lastMessage.sendingTime);
//                            }
//                        });

                        chatAdapter.chatList = chatList;
                        chatAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d(TAG, "getChatExtraDetails -- onError: " + e);
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "getChatExtraDetails -- onComplete: ");
                    }
                });
    }

    private void putUnreadMessagesWithChats() {
        if(currentChatList.isEmpty()) {
            return;
        }

        if(totalUnreadMessageList.isEmpty()) {
            return;
        }

        for(Chat chat: currentChatList) {
            List<Message> unreadChatMessages = new ArrayList<>();

            for(Message message: totalUnreadMessageList) {
                if(message.chatId.equals(chat.id)) {
                    unreadChatMessages.add(message);
                }
            }

            Collections.sort(unreadChatMessages, new Comparator<Message>() {
                @Override
                public int compare(Message item1, Message item2) {
                    return item2.sendingTime.compareTo(item1.sendingTime);
                }
            });

            chat.unreadMessagesCount = unreadChatMessages.size();

            if(!unreadChatMessages.isEmpty()) {
                chat.lastMessage = unreadChatMessages.get(0);
            }
        }

        chatAdapter.notifyDataSetChanged();
    }

    private void setupOnClickListeners() {
        binding.btnNewChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newChatDialog.show();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();

        Log.d(TAG, "onStop: Called");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "onDestroyView: Called");

        if(unreadMessagesListener != null) {
            unreadMessagesListener.remove();
        }

        if(userChatsListener != null) {
            userChatsListener.remove();
        }

        compositeDisposable.clear();
        binding = null;
    }

    /***************************************** DIALOGS ********************************************/
    private void setupNewChatDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());

        DialogNewChatBinding dialogBinding = DialogNewChatBinding.inflate(getLayoutInflater(),
                binding.getRoot(), false);
        dialogBuilder.setView(dialogBinding.getRoot());

        dialogBinding.btnCreateChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchUserByPhone(dialogBinding);
            }
        });

        newChatDialog = dialogBuilder.create();

        newChatDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dialogBinding.etPhone.setText("");
                hideNewChatDialogProgressBar(dialogBinding);
            }
        });

        if (newChatDialog.getWindow() != null) {
            newChatDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getActivity().getWindow().setGravity(Gravity.BOTTOM);
            newChatDialog.getWindow().getAttributes().windowAnimations = R.style.dialog_animation;
        }
    }

    private void searchUserByPhone(DialogNewChatBinding newChatBinding) {
        String phone = newChatBinding.etPhone.getText().toString();

        if(phone.length() == 0) {
            return;
        }

        if(phone.length() != 10) {
            Toast.makeText(getContext(), "Please enter valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if(phone.equals(CommonVariables.loggedInUser.phone)) {
            Toast.makeText(getContext(), "This Phone is currently logged in",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        showNewChatDialogProgressBar(newChatBinding);

        repository.getUserByPhone(phone)
                .subscribe(new MaybeObserver<User>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        Log.d(TAG, "onSubscribe: Called");
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull User user) {
                        Log.d(TAG, "onSuccess: Called" + user);
                        if(user != null) {
                            newChatDialog.dismiss();

                            Intent intent = new Intent(getContext(), ChatActivity.class);
                            intent.putExtra(Constants.KEY_USER, user);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d(TAG, "onError: Called");
                        Log.d(TAG, e.toString());
                        hideNewChatDialogProgressBar(newChatBinding);

                        String errorMessage = e instanceof IntouchErrorHandler ? e.getMessage() : "Some error occurred";
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                        Log.d(TAG, "onComplete: Called");
                        hideNewChatDialogProgressBar(newChatBinding);

                        Toast.makeText(getContext(), "No user found with given phone",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showNewChatDialogProgressBar(DialogNewChatBinding newChatBinding) {
        newChatBinding.progressBar.setVisibility(View.VISIBLE);
        newChatBinding.btnCreateChat.setVisibility(View.GONE);
    }

    private void hideNewChatDialogProgressBar(DialogNewChatBinding newChatBinding) {
        newChatBinding.progressBar.setVisibility(View.GONE);
        newChatBinding.btnCreateChat.setVisibility(View.VISIBLE);
    }
}
