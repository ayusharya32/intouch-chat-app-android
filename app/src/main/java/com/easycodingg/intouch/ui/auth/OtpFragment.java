package com.easycodingg.intouch.ui.auth;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.easycodingg.intouch.baseactivity.AuthActivity;
import com.easycodingg.intouch.baseactivity.HomeActivity;
import com.easycodingg.intouch.databinding.FragmentOtpBinding;
import com.easycodingg.intouch.models.LoggedInDevice;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.db.IntouchRepository;
import com.easycodingg.intouch.ui.editprofile.EditProfileActivity;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.Constants;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.MaybeObserver;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;

public class OtpFragment extends Fragment {
    private static final String TAG = "OtpFragmentyy";
    private static final int OTP_LENGTH = 6;
    private FragmentOtpBinding binding;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseMessaging messaging;
    private IntouchRepository repository;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks phoneAuthCallbacks;

    private String phone;
    private String verificationId;
    private String enteredCode;
    private CountDownTimer timer;

    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentOtpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        messaging = FirebaseMessaging.getInstance();
        repository = IntouchRepository.getInstance(getContext());

        phone = OtpFragmentArgs.fromBundle(getArguments()).getPhone();

        if(phone.length() == 10) {
            setupInfo();
            setupOtpInputs();
            setupPhoneAuthCallbacks();
            sendOTP();
            setupOnClickListeners();

        } else {
            Toast.makeText(getContext(), "Invalid Phone Number", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupInfo() {
        binding.txtCodeSentTo.setText("Code is sent to +91 " + phone);
    }

    private void setupOnClickListeners() {
        binding.btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(enteredCode.length() != OTP_LENGTH) {
                    return;
                }

                signInWithPhoneAuthCredential(enteredCode);
            }
        });

        binding.txtRequestAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendOTP();
            }
        });
    }

    private void setupOtpInputs() {
        View.OnKeyListener keyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                EditText currentEditText = (EditText) view;

                if(currentEditText.getText().length() != 0) {
                    return false;
                }

                if(event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                    if(view.getId() == binding.etOtp2.getId()) {
                        binding.etOtp1.requestFocus();
                        binding.etOtp1.setText("");

                    } else if(view.getId() == binding.etOtp3.getId()) {
                        binding.etOtp2.requestFocus();
                        binding.etOtp2.setText("");

                    } else if(view.getId() == binding.etOtp4.getId()) {
                        binding.etOtp3.requestFocus();
                        binding.etOtp3.setText("");

                    } else if(view.getId() == binding.etOtp5.getId()) {
                        binding.etOtp4.requestFocus();
                        binding.etOtp4.setText("");

                    } else if(view.getId() == binding.etOtp6.getId()){
                        binding.etOtp5.requestFocus();
                        binding.etOtp5.setText("");
                    }
                }

                return false;
            }
        };

        binding.etOtp1.setOnKeyListener(keyListener);
        binding.etOtp2.setOnKeyListener(keyListener);
        binding.etOtp3.setOnKeyListener(keyListener);
        binding.etOtp4.setOnKeyListener(keyListener);
        binding.etOtp5.setOnKeyListener(keyListener);
        binding.etOtp6.setOnKeyListener(keyListener);

        binding.etOtp1.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.length() == 1) {
                    binding.etOtp2.requestFocus();
                }
            }
        });

        binding.etOtp2.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.length() == 1) {
                    binding.etOtp3.requestFocus();
                }
            }
        });

        binding.etOtp3.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.length() == 1) {
                    binding.etOtp4.requestFocus();
                }
            }
        });

        binding.etOtp4.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.length() == 1) {
                    binding.etOtp5.requestFocus();
                }
            }
        });

        binding.etOtp5.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.length() == 1) {
                    binding.etOtp6.requestFocus();
                }
            }
        });

        binding.etOtp6.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = s.toString();
                if (str.length() == 1) {
                    String s1 = binding.etOtp1.getText().toString();
                    String s2 = binding.etOtp2.getText().toString();
                    String s3 = binding.etOtp3.getText().toString();
                    String s4 = binding.etOtp4.getText().toString();
                    String s5 = binding.etOtp5.getText().toString();

                    enteredCode = s1 + s2 + s3 + s4 + s5 + str;
                }
            }
        });
    }

    private void setupPhoneAuthCallbacks() {
        phoneAuthCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                String smsCode = phoneAuthCredential.getSmsCode();
                signInWithPhoneAuthCredential(smsCode);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {
                Log.d(TAG, e.toString());

                if(e instanceof FirebaseAuthInvalidCredentialsException) {
                    Toast.makeText(getContext(), "Invalid Phone Number", Toast.LENGTH_SHORT).show();
                } else if(e instanceof FirebaseTooManyRequestsException) {
                    Toast.makeText(getContext(), "Request Limit Exceeded, Try Again Later",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Some error occurred", Toast.LENGTH_SHORT).show();
                }

                Navigation.findNavController(getView()).navigateUp();
            }

            @Override
            public void onCodeSent(@NonNull String id,
                                   @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                super.onCodeSent(id, forceResendingToken);

                Toast.makeText(getContext(), "OTP Sent Successfully", Toast.LENGTH_SHORT).show();
                verificationId = id;

                hideSendingOtpProgress();
                startResendOtpTimer();
            }
        };
    }

    private void sendOTP() {
        showSendingOtpProgress();
        hideResendText();

        String phoneWithCountryCode = "+91" + phone;
        Log.d(TAG, "sendOTP: " + phoneWithCountryCode);

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder()
                .setPhoneNumber(phoneWithCountryCode)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(getActivity())
                .setCallbacks(phoneAuthCallbacks)
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void signInWithPhoneAuthCredential(String smsCode) {
        showProgressBar();

        PhoneAuthCredential credentials = PhoneAuthProvider.getCredential(verificationId, smsCode);

        auth.signInWithCredential(credentials)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        hideProgressBar();
                        if(task.isSuccessful()) {
                            if(timer != null) {
                                timer.cancel();
                            }

                            launchActivity();
                        } else {
                            Toast.makeText(getContext(), "Invalid OTP", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, task.getException().toString());
                        }
                    }
                });
    }

    private void startResendOtpTimer() {
        if(timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(62000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int)  millisUntilFinished / 1000;
                binding.txtOtpNotReceived.setText("Resend Code available in " + seconds + " seconds");
            }

            @Override
            public void onFinish() {
                binding.txtOtpNotReceived.setText("Didn't receive code?");
                showResendText();
            }
        };

        timer.start();
    }

    private void launchActivity() {
        if(auth.getCurrentUser() == null) {
            return;
        }

        repository.getUserFromFirebase(auth.getCurrentUser().getUid())
            .subscribe(new MaybeObserver<User>() {
                @Override
                public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable d) {
                    compositeDisposable.add(d);
                }

                @Override
                public void onSuccess(@io.reactivex.rxjava3.annotations.NonNull User user) {
                    CommonVariables.loggedInUser = user;

                    if(CommonVariables.loggedInUser.loggedInDevices.size() == 3) {
                        Toast.makeText(getContext(), "Three devices are already logged in",
                                Toast.LENGTH_SHORT).show();

                        launchAuthActivity();
                        auth.signOut();

                    } else {
                        getAndUpdateFcmToken();
                    }
                }

                @Override
                public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable e) {
                    Log.d(TAG, "onError: " + e);
                    launchAuthActivity();
                    auth.signOut();
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

    private void launchHomeActivity() {
        Intent intent = new Intent(getContext(), HomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void launchEditProfileActivity() {
        Intent intent = new Intent(getContext(), EditProfileActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void launchAuthActivity() {
        Intent intent = new Intent(getContext(), AuthActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void showProgressBar() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnNext.setVisibility(View.GONE);
    }

    private void hideProgressBar() {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnNext.setVisibility(View.VISIBLE);
    }

    private void hideSendingOtpProgress() {
        binding.llForm.setVisibility(View.VISIBLE);
        binding.llSendingOtp.setVisibility(View.GONE);
        binding.animOtp.setVisibility(View.VISIBLE);
    }

    private void showSendingOtpProgress() {
        binding.llForm.setVisibility(View.GONE);
        binding.llSendingOtp.setVisibility(View.VISIBLE);
        binding.animOtp.setVisibility(View.GONE);
    }

    private void showResendText() {
        binding.txtRequestAgain.setVisibility(View.VISIBLE);
    }

    private void hideResendText() {
        binding.txtRequestAgain.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
        compositeDisposable.clear();
    }
}
