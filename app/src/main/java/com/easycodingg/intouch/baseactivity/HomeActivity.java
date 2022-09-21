package com.easycodingg.intouch.baseactivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.easycodingg.intouch.R;
import com.easycodingg.intouch.databinding.ActivityHomeBinding;
import com.easycodingg.intouch.db.IntouchRepository;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.services.IntouchService;
import com.easycodingg.intouch.utils.CommonVariables;
import com.easycodingg.intouch.utils.Constants;

import java.util.Calendar;
import java.util.UUID;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

public class HomeActivity extends AppCompatActivity {
    private static final String TAG = "HomeActivityyyy";

    private ActivityHomeBinding binding;

    private AppBarConfiguration appBarConfiguration;
    private NavController navController;
    private FirebaseFirestore db;

    private IntouchRepository repository;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        repository = IntouchRepository.getInstance(this);

        setSupportActionBar(binding.toolbar);
        setupActivity();

        Log.d(TAG, "Home: " + CommonVariables.loggedInUser);
    }

    private void createUser() {
        binding.progressBar.setVisibility(View.VISIBLE);

        User user = new User();

        user.id = UUID.randomUUID().toString();
        user.name = "Kelly";
        user.phone = "6666666666";
        user.description = "";
        user.dateOfBirth = Calendar.getInstance().getTime();
        user.profileImageUrl = "https://firebasestorage.googleapis.com/v0/b/chat-6e051.appspot.com/o/user.png?alt=media&token=36617080-02de-4399-8c23-ed0b0255be78";

        db.collection(Constants.COLLECTION_USERS)
                .document(user.id)
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        binding.progressBar.setVisibility(View.GONE);

                        if(task.isSuccessful()) {
                            CommonVariables.loggedInUser = user;
                            setupActivity();
                        }
                    }
                });
    }

    private void setupActivity() {
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_my_chats, R.id.nav_my_documents, R.id.nav_my_drawings
        ).build();

        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        startIntouchService();
    }

    private void startIntouchService() {
        Intent intent = new Intent(this, IntouchService.class);
        intent.setAction(IntouchService.ACTION_START_SERVICE);
        startService(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_btn_settings:
                startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: Called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: Called");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Called");

        // TODO: Destroy Service, if app is closed by back button
        compositeDisposable.clear();
    }
}