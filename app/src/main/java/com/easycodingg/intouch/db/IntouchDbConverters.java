package com.easycodingg.intouch.db;

import android.net.Uri;

import androidx.room.TypeConverter;

import com.easycodingg.intouch.models.LoggedInDevice;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.easycodingg.intouch.models.Alias;

import java.lang.reflect.Type;
import java.util.Date;
import java.util.List;

public class IntouchDbConverters {
    @TypeConverter
    public static Date fromTimestampToDate(Long value) {
        return value == null ? null : new Date(value);
    }

    @TypeConverter
    public static Long dateToTimestamp(Date date) {
        return date == null ? null : date.getTime();
    }

    @TypeConverter
    public List<String> jsonToStringList(String listJson) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>(){}.getType();

        return listJson == null ? null : gson.fromJson(listJson, type);
    }

    @TypeConverter
    public String stringListToJson(List<String> stringList) {
        return stringList == null ? null : new Gson().toJson(stringList);
    }

    @TypeConverter
    public List<Alias> jsonToAliasList(String listJson) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<Alias>>(){}.getType();

        return listJson == null ? null : gson.fromJson(listJson, type);
    }

    @TypeConverter
    public String aliasListToJson(List<Alias> aliases) {
        return aliases == null ? null : new Gson().toJson(aliases);
    }

    @TypeConverter
    public Uri stringToUri(String uriString) {
        return uriString != null ? Uri.parse(uriString) : null;
    }

    @TypeConverter
    public String uriToString(Uri uri) {
        return uri != null ? uri.toString() : null;
    }

    @TypeConverter
    public List<LoggedInDevice> jsonToLoggedInDevices(String loggedInDeviceListJson) {
        Gson gson = new Gson();
        Type type = new TypeToken<List<LoggedInDevice>>(){}.getType();

        return loggedInDeviceListJson == null ? null : gson.fromJson(loggedInDeviceListJson, type);
    }

    @TypeConverter
    public String loggedInDeviceListToJson(List<LoggedInDevice> loggedInDevices) {
        return loggedInDevices == null ? null : new Gson().toJson(loggedInDevices);
    }
}
