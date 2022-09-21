package com.easycodingg.intouch.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.core.content.ContextCompat;

import com.easycodingg.intouch.R;
import com.easycodingg.intouch.models.Chat;
import com.easycodingg.intouch.models.LoggedInDevice;
import com.easycodingg.intouch.models.Message;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.utils.enums.MessageStatus;
import com.easycodingg.intouch.utils.enums.MessageType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class CommonMethods {
    private static final String TAG = "CommonMethodsyy";

    public static LoggedInDevice getCurrentLoggedInDevice(String fcmToken) {
        LoggedInDevice loggedInDevice = new LoggedInDevice();
        loggedInDevice.deviceId = CommonVariables.deviceId;
        loggedInDevice.deviceName = Build.MANUFACTURER + " " + Build.MODEL;
        loggedInDevice.fcmToken = fcmToken;
        loggedInDevice.loggedInTime = Calendar.getInstance().getTime();
        loggedInDevice.lastActive = Calendar.getInstance().getTime();

        return loggedInDevice;
    }

    public static void updateUserLoggedInDevices(User user, String fcmToken) {
        LoggedInDevice loggedInDevice = CommonMethods.getCurrentLoggedInDevice(fcmToken);

        if(user.loggedInDevices == null) {
            user.loggedInDevices = new ArrayList<>();
            user.loggedInDevices.add(loggedInDevice);
        }

        LoggedInDevice foundLoginDevice = null;

        for(LoggedInDevice device: user.loggedInDevices) {
            if(device.deviceId.equals(CommonVariables.deviceId)) {
                foundLoginDevice = device;
                break;
            }
        }

        user.loggedInDevices.remove(foundLoginDevice);
        user.loggedInDevices.add(loggedInDevice);
    }

    public static String getFormattedDate(Date date) {
        String myFormat = "dd/MM/yyyy";
        SimpleDateFormat sdf = new SimpleDateFormat(myFormat, Locale.getDefault());
        return sdf.format(date);
    }

    public static String getFileTypeFromUri(Context context, Uri uri) {
        return context.getContentResolver().getType(uri).split("/")[1];
    }

    public static String getPhoneNumberWithoutCountryCode(String phoneWithCode) {
        if(!phoneWithCode.contains("+91")) {
            return phoneWithCode;
        }

        return phoneWithCode.substring(3);
    }

    public static String getFormattedTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.UK);
        return sdf.format(date);
    }

    public static String getFormattedDateTime(Date date, String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(format, Locale.UK);
        return sdf.format(date);
    }

    public static int getMessageStatusImageId(MessageStatus status) {
        switch(status) {
            case WAITING_TO_SEND:
                return R.drawable.ic_clock;

            case SENT:
                return R.drawable.ic_check;

            default:
                return R.drawable.ic_double_check;
        }
    }

    public static int getMessageStatusImageColor(Context ctx, MessageStatus status) {
        switch(status) {
            case READ:
                return ContextCompat.getColor(ctx, R.color.blue_200);

            default:
                return ContextCompat.getColor(ctx, R.color.grey_500);
        }
    }

    public static String getOtherUserIdFromChat(Chat chat) {
        Log.d("CommonMethods", "getOtherUserIdFromChat: " + CommonVariables.loggedInUser);

        for(String userId: chat.users) {
            if(!userId.equals(CommonVariables.loggedInUser.id)) {
                return userId;
            }
        }

        return "";
    }

    public static String getMessageContentString(Message message) {
        switch(message.type) {
            case TYPE_TEXT:
                return message.content;

            case TYPE_IMAGE:
                return "Image";

            default:
                return "Document";
        }
    }

    public static String getChatMessageTime(Message message) {
        Log.d("Dateyyy", "getChatMessageTime: " + message);

        message.orderTimestamp = message.sendingTime;

        if(isDateOfToday(message.orderTimestamp)) {
            return getFormattedTime(message.orderTimestamp);

        } else if(isDateOfYesterday(message.orderTimestamp)) {
            return "Yesterday";

        } else {
            return getFormattedDate(message.orderTimestamp);
        }
    }

    public static String getLastSeenTime(Date lastSeen) {
        Log.d(TAG, "getLastSeenTime: " + lastSeen);

        if(isDateOfToday(lastSeen)) {
            return "today at " + getFormattedTime(lastSeen);

        } else if(isDateOfYesterday(lastSeen)) {
            return "yesterday at " + getFormattedTime(lastSeen);

        } else {
            long daysAgo = TimeUnit.MILLISECONDS.toDays(
                    Calendar.getInstance().getTimeInMillis() - lastSeen.getTime());

            Log.d(TAG, "getLastSeenTime: daysAgo -- " + daysAgo);
            return daysAgo < 7 ? daysAgo + " days ago" : "";
        }
    }

    public static boolean isDateOfToday(Date date) {
        Calendar todayCalendar = Calendar.getInstance();

        Calendar givenDateCalender = Calendar.getInstance();
        givenDateCalender.setTime(date);

        return todayCalendar.get(Calendar.DAY_OF_YEAR) == givenDateCalender.get(Calendar.DAY_OF_YEAR)
                && todayCalendar.get(Calendar.YEAR) == givenDateCalender.get(Calendar.YEAR);
    }

    public static boolean isDateOfYesterday(Date date) {
        Calendar yesterdayCalendar = Calendar.getInstance();
        yesterdayCalendar.add(Calendar.DAY_OF_YEAR, -1);

        Calendar givenDateCalender = Calendar.getInstance();
        givenDateCalender.setTime(date);

        return yesterdayCalendar.get(Calendar.DAY_OF_YEAR) == givenDateCalender.get(Calendar.DAY_OF_YEAR)
                && yesterdayCalendar.get(Calendar.YEAR) == givenDateCalender.get(Calendar.YEAR);
    }

    public static Bitmap getImageBitmapFromUri(Context context, Uri imageUri) throws Exception {
        if(Build.VERSION.SDK_INT < 28) {
            return MediaStore.Images.Media.getBitmap(context.getContentResolver(), imageUri);
        } else {
            ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), imageUri);
            return ImageDecoder.decodeBitmap(source);
        }
    }

    public static String getFileNameFromUri(Context context, Uri fileUri) {
        String fileName = "";

        if (fileUri.getScheme().equals("file")) {
            fileName = fileUri.getLastPathSegment();

        } else {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(fileUri, new String[]{
                        MediaStore.Images.ImageColumns.DISPLAY_NAME
                }, null, null, null);

                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME);

                    if(nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                        Log.d(TAG, "name is " + fileName);
                    }
                }

            }
            finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        return fileName;
    }

    public static String getFileExtensionFromContentUri(Context context, Uri uri) {
        Log.d(TAG, "getFileExtensionFromContentUri: Uri Path " + uri.getPath());

        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String type = mimeTypeMap.getExtensionFromMimeType(context.getContentResolver().getType(uri));

        Log.d(TAG, "getFileExtensionFromContentUri: Type " + type);

        return type;
    }

    public static String getExtensionFromFileName(String fileName) {
        List<String> dotSplitArray = Arrays.asList(fileName.split("\\."));
        return dotSplitArray.get(dotSplitArray.size() - 1);
    }

    public static String getMimeTypeFromFileName(String fileName) {
        String fileExtension = getExtensionFromFileName(fileName);

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);
    }

//    private void testingLiveData() {
//        Log.d(TAG, "testingLiveData: Called");
//
//        MutableLiveData<Integer> testLiveData = new MutableLiveData<>(999);
//
//        testLiveData.observe(this, new Observer<Integer>() {
//            @Override
//            public void onChanged(Integer integer) {
//                Log.d(TAG, "testingLiveData: " + integer);
//            }
//        });
//
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG, "testingLiveData Handler Called");
//                Log.d(TAG, "testingLiveData has observers: " + testLiveData.hasObservers());
//                Log.d(TAG, "testingLiveData has active observers: " + testLiveData.hasActiveObservers());
//
//                testLiveData.postValue(1);
//                testLiveData.postValue(2);
//                testLiveData.postValue(3);
//                testLiveData.postValue(4);
//                testLiveData.postValue(5);
//                testLiveData.postValue(6);
//                testLiveData.postValue(7);
//                testLiveData.postValue(8);
//                testLiveData.postValue(9);
//                testLiveData.postValue(10);
//                testLiveData.postValue(11);
//                testLiveData.postValue(12);
//                testLiveData.postValue(13);
//                testLiveData.postValue(14);
//                testLiveData.postValue(15);
//                testLiveData.postValue(16);
//                testLiveData.postValue(17);
//            }
//        }, 2000);
//    }
}
