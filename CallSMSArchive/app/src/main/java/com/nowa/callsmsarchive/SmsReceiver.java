package com.nowa.callsmsarchive;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.telephony.SmsMessage;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage msg = SmsMessage.createFromPdu((byte[]) pdu);
            String sender = msg.getOriginatingAddress();
            String body = msg.getMessageBody();
            long timestamp = msg.getTimestampMillis();
            saveSms(context, sender, body, "INBOX", timestamp);
        }
    }

    public static void saveSms(Context context, String address, String body, String type, long timestamp) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("address", address);
        values.put("name", "");
        values.put("body", body);
        values.put("type", type);
        values.put("timestamp", timestamp);
        values.put("saved_at", System.currentTimeMillis());

        db.insert(DatabaseHelper.TABLE_SMS, null, values);
        db.close();
    }
}
