package com.easycodingg.intouch.baseactivity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

import com.easycodingg.intouch.db.IntouchRepository;
import com.easycodingg.intouch.models.LoggedInDevice;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.ui.editprofile.EditProfileActivity;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.Constants;
import com.easycodingg.intouch.utils.LoadingDialogFragment;
import com.easycodingg.intouch.utils.NetworkUtils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "Splashyyy";

    private Handler handler;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseMessaging messaging;
    private IntouchRepository repository;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);

        splashScreen.setKeepOnScreenCondition(() -> true);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        messaging = FirebaseMessaging.getInstance();
        repository = IntouchRepository.getInstance(this);

        Log.d(TAG, "onCreate: " + repository.getCurrentUserFromPrefs());

        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setupApp();
            }
        }, 0);
    }

    private void setupApp() {
        initializeAndGetDeviceId();

        if(auth.getCurrentUser() != null) {
            getCurrentUserDetails();

        } else {
            Log.d(TAG, "Launching Auth Activity --- ");
            launchAuthActivity();
        }
    }

    private void initializeAndGetDeviceId() {
        String deviceId = repository.getDeviceId();

        if(deviceId == null || deviceId.isEmpty()) {
            deviceId = UUID.randomUUID().toString();
            repository.saveDeviceId(deviceId);
        }

        CommonVariables.deviceId = deviceId;
    }

    private void getCurrentUserDetails() {
        Log.d(TAG, "getCurrentUserDetails: Called");

        repository.getLoggedInUserDetails(auth.getCurrentUser().getUid(), false)
                .subscribe(new MaybeObserver<User>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull User user) {
                        CommonVariables.loggedInUser = user;

                        boolean deviceStillLoggedIn = false;
                        for(LoggedInDevice device: user.loggedInDevices) {
                            if(device.deviceId.equals(CommonVariables.deviceId)) {
                                deviceStillLoggedIn = true;
                                break;
                            }
                        }

                        if(deviceStillLoggedIn) {
                            Log.d(TAG, "onSuccess: Device Logged in going to get fcm token");
                            getAndUpdateFcmToken();

                        } else {
                            Log.d(TAG, "onSuccess: Device not logged in, clearing data");
                            clearAllCachedData();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d(TAG, e + "");
                        clearAllCachedData();
                    }

                    @Override
                    public void onComplete() {
                        launchEditProfileActivity();
                    }
                });
    }

    private void getAndUpdateFcmToken() {
        repository.getAndUpdateFcmToken()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        launchHomeActivity();
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                    }
                });
    }

    private void clearAllCachedData() {
        repository.clearAllCachedData()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onComplete() {
                        auth.signOut();
                        launchAuthActivity();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                    }
                });
    }

    private void launchHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    private void launchAuthActivity() {
        Intent intent = new Intent(this, AuthActivity.class);
        startActivity(intent);
        finish();
    }

    private void launchEditProfileActivity() {
        Intent intent = new Intent(this, EditProfileActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}