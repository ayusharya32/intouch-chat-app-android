package com.easycodingg.intouch.baseactivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.easycodingg.intouch.R;
import com.easycodingg.intouch.adapters.LoggedInDeviceAdapter;
import com.easycodingg.intouch.databinding.ActivitySettingsBinding;
import com.easycodingg.intouch.databinding.DialogLogoutBinding;
import com.easycodingg.intouch.db.IntouchRepository;
import com.easycodingg.intouch.models.LoggedInDevice;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.LoadingDialogFragment;
import com.easycodingg.intouch.utils.events.LoggedInDeviceItemClickEvent;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Collections;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivityyy";
    private ActivitySettingsBinding binding;

    private FirebaseAuth auth;
    private IntouchRepository repository;

    private LoggedInDeviceAdapter loggedInDeviceAdapter;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        repository = IntouchRepository.getInstance(this);

        setupLoggedInDevicesRecyclerView();

        getUpdatedLoggedInDevices();

        setOnClickListeners();
    }

    private void setupLoggedInDevicesRecyclerView() {
        loggedInDeviceAdapter = new LoggedInDeviceAdapter(Collections.emptyList(), new LoggedInDeviceItemClickEvent() {
            @Override
            public void onLogoutButtonCLick(LoggedInDevice device) {
                showLogoutDialog(
                        "Are you sure you want to logout from below device?",
                        device
                );
            }
        });

        binding.rvLoggedInDevices.setLayoutManager(new LinearLayoutManager(this));
        binding.rvLoggedInDevices.setAdapter(loggedInDeviceAdapter);
    }

    private void setOnClickListeners() {
        binding.txtLogoutThisDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(LoggedInDevice loggedInDevice: CommonVariables.loggedInUser.loggedInDevices) {
                    if(loggedInDevice.deviceId.equals(CommonVariables.deviceId)) {
                        showLogoutDialog(
                        "Are you sure you want to logout from current device?",
                            loggedInDevice
                        );

                        break;
                    }
                }
            }
        });

        binding.txtLogoutAllDevices.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog(
                        "Are you sure you want to logout from all devices?",
                        null
                );

            }
        });
    }

    private void getUpdatedLoggedInDevices() {
        repository.getLoggedInUserDetails(CommonVariables.loggedInUser.id, true)
                .subscribe(new MaybeObserver<User>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(@NonNull User user) {
                        CommonVariables.loggedInUser = user;

                        loggedInDeviceAdapter.deviceList = user.loggedInDevices;
                        loggedInDeviceAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                    }

                    @Override
                    public void onComplete() {
                        finish();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    private void showLogoutDialog(String title, LoggedInDevice device) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        DialogLogoutBinding dialogBinding = DialogLogoutBinding.inflate(getLayoutInflater(),
                binding.getRoot(), false);
        dialogBuilder.setView(dialogBinding.getRoot());

        dialogBinding.txtInfo.setText(title);

        if(device != null) {
            dialogBinding.txtDeviceName.setText(device.deviceName);
            dialogBinding.txtDeviceName.setVisibility(View.VISIBLE);

        } else {
            dialogBinding.txtDeviceName.setVisibility(View.GONE);
        }

        AlertDialog logoutDialog = dialogBuilder.create();
        logoutDialog.setCancelable(true);

        dialogBinding.btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutDialog.dismiss();

                if(device != null) {
                    logoutFromDevice(device);

                } else {
                    logoutFromAllDevices();
                }
            }
        });

        dialogBinding.btnNotNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logoutDialog.dismiss();
            }
        });

        logoutDialog.show();
    }

    private void logoutFromDevice(LoggedInDevice device) {
        LoadingDialogFragment loadingDialog = LoadingDialogFragment
                .getLoadingDialogFragmentInstance("Logging out..");

        repository.logoutFromDevice(device)
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                        loadingDialog.show(getSupportFragmentManager(), "loading");
                    }

                    @Override
                    public void onComplete() {
                        auth.signOut();
                        loadingDialog.dismiss();

                        if(device.deviceId.equals(CommonVariables.deviceId)) {
                            CommonVariables.loggedInUser = null;
                            launchAuthActivity();

                        } else {
                            loggedInDeviceAdapter.deviceList = CommonVariables.loggedInUser.loggedInDevices;
                            loggedInDeviceAdapter.notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                        loadingDialog.dismiss();
                    }
                });
    }

    private void logoutFromAllDevices() {
        LoadingDialogFragment loadingDialog = LoadingDialogFragment
                .getLoadingDialogFragmentInstance("Logging out..");

        repository.logoutFromAllDevices()
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        compositeDisposable.add(d);
                        loadingDialog.show(getSupportFragmentManager(), "loading");
                    }

                    @Override
                    public void onComplete() {
                        auth.signOut();
                        loadingDialog.dismiss();

                        CommonVariables.loggedInUser = null;
                        launchAuthActivity();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Log.d(TAG, "onError: " + e);
                        loadingDialog.dismiss();
                    }
                });
    }

    private void launchAuthActivity() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);

    }
}
