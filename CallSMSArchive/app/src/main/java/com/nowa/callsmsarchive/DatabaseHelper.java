package com.nowa.callsmsarchive;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "archive.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_CALLS = "calls";
    public static final String TABLE_SMS = "sms";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CALLS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "number TEXT," +
                "name TEXT," +
                "type TEXT," +
                "duration INTEGER," +
                "timestamp INTEGER," +
                "saved_at INTEGER)");

        db.execSQL("CREATE TABLE " + TABLE_SMS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "address TEXT," +
                "name TEXT," +
                "body TEXT," +
                "type TEXT," +
                "timestamp INTEGER," +
                "saved_at INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CALLS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SMS);
        onCreate(db);
    }
}
