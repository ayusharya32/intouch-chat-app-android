package com.easycodingg.intouch.ui.editprofile;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.easycodingg.intouch.baseactivity.AuthActivity;
import com.easycodingg.intouch.baseactivity.HomeActivity;
import com.easycodingg.intouch.databinding.ActivityEditProfileBinding;
import com.easycodingg.intouch.db.IntouchRepository;
import com.easycodingg.intouch.models.LoggedInDevice;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.utils.CommonMethods;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.Constants;
import com.easycodingg.intouch.utils.IntouchErrorHandler;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Calendar;

import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class EditProfileActivity extends AppCompatActivity {
    private static final String TAG = "EditProfileActivityyy";
    private ActivityEditProfileBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseMessaging messaging;
    private IntouchRepository repository;

    private DatePickerDialog datePickerDialog;
    private Calendar datePickerCalender;

    private Uri profileImageUri;
    private String profileImageDownloadUrl;
    private boolean isProfileImageUpdated = false;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        messaging = FirebaseMessaging.getInstance();
        repository = IntouchRepository.getInstance(this);

        setupDatePickerDialog();
        setOnClickListeners();

    }

    private void setOnClickListeners() {
        binding.layoutEditProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getContentLauncher.launch("image/*");
            }
        });

        binding.etDob.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                datePickerDialog.show();
            }
        });

        binding.btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateInput();
            }
        });
    }

    private void setupDatePickerDialog() {
        datePickerCalender = Calendar.getInstance();

        DatePickerDialog.OnDateSetListener dateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                datePickerCalender.set(Calendar.YEAR, year);
                datePickerCalender.set(Calendar.MONTH, month);
                datePickerCalender.set(Calendar.DAY_OF_MONTH, month);

                binding.etDob.setText(CommonMethods.getFormattedDate(datePickerCalender.getTime()));
            }
        };

        datePickerDialog = new DatePickerDialog(this, dateSetListener, datePickerCalender.get(Calendar.YEAR),
                datePickerCalender.get(Calendar.MONTH), datePickerCalender.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
    }

    private void validateInput() {
        if(binding.etName.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;

        } else if(binding.etDob.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please enter your date of birth", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgressBar();
        if(isProfileImageUpdated) {
            uploadProfileImage();

        } else {
            saveAndUploadNewUserWithFcmToken();
        }
    }

    private void uploadProfileImage() {
        if(profileImageUri == null) {
            return;
        }

        String profileImagePath = auth.getCurrentUser().getUid() + "/IMG" + System.currentTimeMillis() + "." +
                CommonMethods.getFileTypeFromUri(this, profileImageUri);

        StorageReference storageRef = storage.getReference(profileImagePath);
        Log.d(TAG, profileImagePath);

        UploadTask uploadTask = storageRef.putFile(profileImageUri);

        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if(!task.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Some error occurred while uploading image",
                            Toast.LENGTH_SHORT).show();
                }

                return storageRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if(task.isSuccessful()) {
                    profileImageDownloadUrl = task.getResult().toString();

                    saveAndUploadNewUserWithFcmToken();
                } else {
                    Toast.makeText(EditProfileActivity.this,
                            "Some error occurred", Toast.LENGTH_SHORT).show();
                    hideProgressBar();
                }
            }
        });
    }

    private void getAndUpdateFcmToken() {
        messaging.getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        Log.d(TAG, "Fetched FCM from firebase --- ");

                        if(task.isSuccessful() && task.getResult() != null) {
                            String fcmToken = task.getResult();
                            Log.d(TAG, "New FCM Token: " + fcmToken);

                            repository.saveFcmUpdateTimeStamp();
                            uploadUser(fcmToken);
                        }
                    }
                });
    }

    private void uploadUser(String fcmToken) {
        User user = new User();

        user.id = auth.getCurrentUser().getUid();
        user.name = binding.etName.getText().toString().trim();
        user.phone = CommonMethods.getPhoneNumberWithoutCountryCode(auth.getCurrentUser().getPhoneNumber());
        user.description = binding.etDescription.getText().toString().trim();
        user.profileImageUrl = profileImageDownloadUrl;
        user.dateOfBirth = datePickerCalender.getTime();
        user.loggedInDevices = new ArrayList<>();

        LoggedInDevice device = new LoggedInDevice(
                repository.getDeviceId(),
                Build.MANUFACTURER + " " + Build.MODEL,
                fcmToken,
                Calendar.getInstance().getTime()
        );

        user.loggedInDevices.add(device);

        db.collection(Constants.COLLECTION_USERS)
                .document(user.id)
                .set(user)
                .addOnCompleteListener( new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        hideProgressBar();

                        if(task.isSuccessful()) {
                            repository.saveCurrentUserInPrefs(user);
                            repository.saveCurrentUserUpdateTimeStamp();
                            launchHomeActivity();

                        }
                    }
                });
    }

    private void saveAndUploadNewUserWithFcmToken() {
        User user = new User();

        user.id = auth.getCurrentUser().getUid();
        user.name = binding.etName.getText().toString().trim();
        user.phone = CommonMethods.getPhoneNumberWithoutCountryCode(auth.getCurrentUser().getPhoneNumber());
        user.description = binding.etDescription.getText().toString().trim();
        user.profileImageUrl = profileImageDownloadUrl;
        user.dateOfBirth = datePickerCalender.getTime();

        repository.saveAndUploadNewLoggedInUserDetails(user)
                .subscribe(new SingleObserver<User>() {
                    @Override
                    public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull User user) {
                        hideProgressBar();
                        CommonVariables.loggedInUser = user;

                        launchHomeActivity();
                    }

                    @Override
                    public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                        hideProgressBar();
                        String toastMessage = e instanceof IntouchErrorHandler
                            ? e.getMessage() : "Some error occured";

                        Toast.makeText(EditProfileActivity.this, toastMessage,
                                Toast.LENGTH_SHORT).show();

                        Log.d(TAG, "onError: " + e);
                    }
                });
    }

    private void launchHomeActivity() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void launchAuthActivity() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnSubmit.setVisibility(View.GONE);
    }

    private void hideProgressBar() {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnSubmit.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }

    private final ActivityResultLauncher<String> getContentLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            new ActivityResultCallback<Uri>() {
                @Override
                public void onActivityResult(Uri result) {
                    if(result != null) {
                        profileImageUri = result;
                        isProfileImageUpdated = true;

                        binding.imgUser.setImageURI(profileImageUri);
                    }
                }
            });
}