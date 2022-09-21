package com.easycodingg.intouch.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.easycodingg.intouch.models.Chat;
import com.easycodingg.intouch.models.Message;
import com.easycodingg.intouch.models.User;
import com.easycodingg.intouch.models.UserStatus;

@Database(entities = {Message.class, Chat.class, User.class, UserStatus.class}, version = 1, exportSchema = false)
@TypeConverters({IntouchDbConverters.class})
public abstract class IntouchDatabase extends RoomDatabase {
    public abstract IntouchDao intouchDao();

    public static volatile IntouchDatabase INSTANCE;

    public static IntouchDatabase getInstance(Context context) {
        if(INSTANCE == null) {
            synchronized(IntouchDatabase.class) {
                if(INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            IntouchDatabase.class, "intouchDB").build();
                }
            }
        }

        return INSTANCE;
    }
}
