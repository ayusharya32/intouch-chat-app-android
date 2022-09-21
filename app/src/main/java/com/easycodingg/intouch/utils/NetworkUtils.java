package com.easycodingg.intouch.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class NetworkUtils {
    private static final String TAG = "NetworkUtilsyy";
    private static NetworkUtils INSTANCE;

    private final Context context;
    private final MutableLiveData<Integer> networkStatusLiveData;
    private ConnectivityManager.NetworkCallback networkCallback;

    private NetworkUtils(Context context) {
        this.context = context.getApplicationContext();
        this.networkStatusLiveData = new MutableLiveData<>();
    }

    public static NetworkUtils getInstance(Context context) {
        Log.d(TAG, "getInstance: " + INSTANCE);

        if(INSTANCE == null) {
            INSTANCE = new NetworkUtils(context);
        }

        return INSTANCE;
    }

    public LiveData<Integer> getNetworkStatusLiveData() {
        return networkStatusLiveData;
    }

    public int getNetworkStatus() {
        int result = 0; // Returns connection type. 0: none; 1: mobile data; 2: wifi; 3: VPN

        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager != null) {
            NetworkCapabilities capabilities = connectivityManager
                    .getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    result = 1;

                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    result = 2;

                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
                    result = 3;
                }
            }
        }

        return result;
    }

    public void registerNetworkChangeListener() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network);

                networkStatusLiveData.postValue(getNetworkStatus());
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network);

                networkStatusLiveData.postValue(0);
            }
        };

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);

        } else {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }

    public void removeNetworkChangeListener() {
        if(networkCallback == null) {
            return;
        }

        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        connectivityManager.unregisterNetworkCallback(networkCallback);
    }
}
