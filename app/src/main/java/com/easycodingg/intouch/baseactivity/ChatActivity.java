package com.easycodingg.intouch.baseactivity;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.easycodingg.intouch.R;
import com.easycodingg.intouch.databinding.ActivityChatBinding;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivityyyy";
    private ActivityChatBinding binding;

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        Log.d(TAG, "onCreate: " + getSupportActionBar());

        setupActivity();
    }

    private void setupActivity() {
        appBarConfiguration = new AppBarConfiguration.Builder(
                // Add Top Level Fragments Here
        ).build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        navController.addOnDestinationChangedListener(new NavController.OnDestinationChangedListener() {
            @Override
            public void onDestinationChanged(@NonNull NavController navController,
                                             @NonNull NavDestination navDestination,
                                             @Nullable Bundle bundle) {
                switch(navDestination.getId()) {
                    case R.id.nav_chat:
                    case R.id.nav_image_viewer:
                        getSupportActionBar().hide();
                        break;

                    default:
                        getSupportActionBar().show();
                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }
}
