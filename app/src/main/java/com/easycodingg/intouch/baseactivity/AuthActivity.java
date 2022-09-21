package com.easycodingg.intouch.baseactivity;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.easycodingg.intouch.R;
import com.easycodingg.intouch.databinding.ActivityAuthBinding;
import com.easycodingg.intouch.utils.CommonMethods;
import com.easycodingg.intouch.utils.CommonVariables;

public class AuthActivity extends AppCompatActivity {
    private static final String TAG = "Authyyyy";

    private ActivityAuthBinding binding;

    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAuthBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
    }
}
