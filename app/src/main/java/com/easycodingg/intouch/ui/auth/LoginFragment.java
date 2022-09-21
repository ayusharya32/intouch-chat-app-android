package com.easycodingg.intouch.ui.auth;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.navigation.Navigation;

import com.easycodingg.intouch.databinding.FragmentLoginBinding;
import com.easycodingg.intouch.utils.NetworkUtils;

public class LoginFragment extends Fragment {
    private static final String TAG = "LoginFragmentyy";
    private FragmentLoginBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentLoginBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onGetStartedClicked();
            }
        });


        NetworkUtils networkUtils = NetworkUtils.getInstance(getContext());

        networkUtils.getNetworkStatusLiveData().observe(getViewLifecycleOwner(), new Observer<Integer>() {
            @Override
            public void onChanged(Integer statusCode) {
                Log.d(TAG, "Network Available: " + statusCode);
            }
        });
    }

    private void onGetStartedClicked() {
        String phone = binding.etPhone.getText().toString();

        if(phone.isEmpty()) {
            return;
        }

        if(phone.length() != 10) {
            Toast.makeText(getContext(), "Invalid Phone Number", Toast.LENGTH_SHORT).show();
            return;
        }

        Navigation.findNavController(getView())
                .navigate(LoginFragmentDirections.actionNavLoginToNavOtp(phone));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        binding = null;
    }
}
