package com.nowa.callsmsarchive;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.telephony.TelephonyManager;

public class CallReceiver extends BroadcastReceiver {

    private static String lastNumber = "";
    private static long callStartTime = 0;
    private static boolean isIncoming = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Intent.ACTION_NEW_OUTGOING_CALL.equals(action)) {
            lastNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            if (lastNumber == null) lastNumber = "";
            isIncoming = false;
            callStartTime = System.currentTimeMillis();
        } else {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(state)) {
                lastNumber = (number != null) ? number : "";
                isIncoming = true;
                callStartTime = System.currentTimeMillis();
            } else if (TelephonyManager.EXTRA_STATE_OFFHOOK.equals(state)) {
                callStartTime = System.currentTimeMillis();
            } else if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                if (!lastNumber.isEmpty()) {
                    long duration = (System.currentTimeMillis() - callStartTime) / 1000;
                    String type = isIncoming ? "INCOMING" : "OUTGOING";
                    saveCall(context, lastNumber, type, duration, callStartTime);
                    lastNumber = "";
                }
            }
        }
    }

    private void saveCall(Context context, String number, String type, long duration, long timestamp) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("number", number);
        values.put("name", "");
        values.put("type", type);
        values.put("duration", duration);
        values.put("timestamp", timestamp);
        values.put("saved_at", System.currentTimeMillis());

        db.insert(DatabaseHelper.TABLE_CALLS, null, values);
        db.close();
    }
}
