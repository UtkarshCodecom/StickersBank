package com.stickers.bank.data.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class DatabaseClient {

    public static final String APP_DB = "stickerD";
    private static final String TAG = DatabaseClient.class.getSimpleName();
    private static DatabaseClient mInstance;
    private Context mContext;
    //our app database object
    private AppDatabase appDatabase;

    private DatabaseClient(Context mContext) {
        this.mContext = mContext;

        //creating the app database with Room database builder
        //MyToDos is the name of the database
        appDatabase = Room.databaseBuilder(mContext, AppDatabase.class, APP_DB)
                .build();
    }

    // Migration path definition from version 5 to version 1.
    Migration MIGRATION_5_1 = new Migration(5, 1) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
        }
    };

    public static synchronized DatabaseClient getInstance(Context mCtx) {
        if (mInstance == null) {
            mInstance = new DatabaseClient(mCtx);
        }
        return mInstance;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }
}
