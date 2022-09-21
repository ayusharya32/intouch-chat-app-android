package com.easycodingg.intouch.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.easycodingg.intouch.db.IntouchRepository;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {
    private static final String TAG = "LocationUtils";
    private static final String KEY_PERMANENTLY_DENIED_PERMISSIONS = "KEY_PERMANENTLY_DENIED_PERMISSIONS";

    private static PermissionUtils INSTANCE;

    private Context applicationContext;
    private SharedPreferences sharedPreferences;

    public PermissionUtils(Context context) {
        this.applicationContext = context.getApplicationContext();
        this.sharedPreferences = applicationContext.getSharedPreferences(
                IntouchRepository.INTOUCH_PREFS, Context.MODE_PRIVATE);

        updateDeniedPermissionsList();
    }

    public static PermissionUtils getInstance(Context context) {
        if(INSTANCE == null) {
            INSTANCE = new PermissionUtils(context);
        }

        return INSTANCE;
    }

    public boolean readExternalStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public boolean writeExternalStoragePermissionGranted() {
        return ContextCompat.checkSelfPermission(applicationContext,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    public Intent getAppLocationSettingsIntent() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", applicationContext.getPackageName(), null);
        intent.setData(uri);

        return intent;
    }

    public void updateDeniedPermissionsList() {
        if(readExternalStoragePermissionGranted()) {
            removePermissionPermanentlyDenied(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if(writeExternalStoragePermissionGranted()) {
            removePermissionPermanentlyDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    public void setPermissionPermanentlyDenied(String permissionName) {
        Log.d(TAG, "setPermissionPermanentlyDenied: Called");

        List<String> deniedPermissions = new ArrayList<>();
        String deniedPermissionsJson = sharedPreferences.getString(KEY_PERMANENTLY_DENIED_PERMISSIONS, "");

        if(!deniedPermissionsJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>(){}.getType();
            deniedPermissions = gson.fromJson(deniedPermissionsJson, type);
        }

        if(deniedPermissions.contains(permissionName)) {
            return;
        }

        deniedPermissions.add(permissionName);

        Log.d(TAG, "setPermissionPermanentlyDenied: " + deniedPermissionsJson);
        Log.d(TAG, "setPermissionPermanentlyDenied: " + deniedPermissions);

        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(KEY_PERMANENTLY_DENIED_PERMISSIONS, new Gson().toJson(deniedPermissions));

        prefsEditor.apply();
    }

    public void removePermissionPermanentlyDenied(String permissionName) {
        List<String> deniedPermissions = new ArrayList<>();
        String deniedPermissionsJson = sharedPreferences.getString(KEY_PERMANENTLY_DENIED_PERMISSIONS, "");

        if(!deniedPermissionsJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>(){}.getType();
            deniedPermissions = gson.fromJson(deniedPermissionsJson, type);
        }

        deniedPermissions.remove(permissionName);

        SharedPreferences.Editor prefsEditor = sharedPreferences.edit();
        prefsEditor.putString(KEY_PERMANENTLY_DENIED_PERMISSIONS, new Gson().toJson(deniedPermissions));

        prefsEditor.apply();
    }

    public List<String> getDeniedPermissionsList() {
        List<String> deniedPermissions = new ArrayList<>();
        String deniedPermissionsJson = sharedPreferences.getString(KEY_PERMANENTLY_DENIED_PERMISSIONS, "");

        if(!deniedPermissionsJson.isEmpty()) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>(){}.getType();
            deniedPermissions = gson.fromJson(deniedPermissionsJson, type);
        }

        return deniedPermissions;
    }
}
