package com.nowa.callsmsarchive;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.Telephony;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 100;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> items;
    private Button btnCalls, btnSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        btnCalls = findViewById(R.id.btnCalls);
        btnSms = findViewById(R.id.btnSms);

        items = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);

        btnCalls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importAndShowCalls();
            }
        });

        btnSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                importAndShowSms();
            }
        });

        requestPermissions();
    }

    private void requestPermissions() {
        String[] perms = {
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CONTACTS
        };

        List<String> needed = new ArrayList<>();
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERM_REQUEST);
        } else {
            importAndShowCalls();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        importAndShowCalls();
    }

    private void importAndShowCalls() {
        Toast.makeText(this, "Importing calls...", Toast.LENGTH_SHORT).show();
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentResolver cr = getContentResolver();
            Cursor c = cr.query(
                CallLog.Calls.CONTENT_URI,
                null, null, null,
                CallLog.Calls.DATE + " DESC"
            );

            if (c != null) {
                while (c.moveToNext()) {
                    String number = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    int typeInt = c.getInt(c.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    long duration = c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DURATION));

                    String type = typeInt == CallLog.Calls.INCOMING_TYPE ? "INCOMING" : "OUTGOING";

                    Cursor existing = db.rawQuery(
                        "SELECT id FROM calls WHERE number=? AND timestamp=?",
                        new String[]{number, String.valueOf(date)}
                    );

                    if (existing.getCount() == 0) {
                        ContentValues values = new ContentValues();
                        values.put("number", number);
                        values.put("name", "");
                        values.put("type", type);
                        values.put("duration", duration);
                        values.put("timestamp", date);
                        values.put("saved_at", System.currentTimeMillis());
                        db.insert("calls", null, values);
                    }
                    existing.close();
                }
                c.close();
            }
            db.close();

            showCalls();

        } catch (Exception e) {
            items.clear();
            items.add("Error: " + e.getMessage());
            adapter.notifyDataSetChanged();
        }
    }

    private void showCalls() {
        try {
            items.clear();
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM calls ORDER BY timestamp DESC LIMIT 100", null);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

            while (c.moveToNext()) {
                String number = c.getString(c.getColumnIndexOrThrow("number"));
                String type = c.getString(c.getColumnIndexOrThrow("type"));
                long ts = c.getLong(c.getColumnIndexOrThrow("timestamp"));
                long dur = c.getLong(c.getColumnIndexOrThrow("duration"));
                String typeLabel = "INCOMING".equals(type) ? "Incoming" : "Outgoing";
                String durLabel = dur > 0 ? (dur + "s") : "Missed";
                items.add(typeLabel + " | " + number + "\n" + sdf.format(new Date(ts)) + " | " + durLabel);
            }
            c.close();
            db.close();

            if (items.isEmpty()) {
                items.add("No calls found.");
            } else {
                Toast.makeText(this, items.size() + " calls loaded!", Toast.LENGTH_SHORT).show();
            }
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            items.clear();
            items.add("Error: " + e.getMessage());
            adapter.notifyDataSetChanged();
        }
    }

    private void importAndShowSms() {
        Toast.makeText(this, "Importing SMS...", Toast.LENGTH_SHORT).show();
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            ContentResolver cr = getContentResolver();
            Uri smsUri = Uri.parse("content://sms/inbox");
            Cursor c = cr.query(smsUri, null, null, null, "date DESC");

            if (c != null) {
                while (c.moveToNext()) {
                    String address = c.getString(c.getColumnIndexOrThrow("address"));
                    String body = c.getString(c.getColumnIndexOrThrow("body"));
                    long date = c.getLong(c.getColumnIndexOrThrow("date"));

                    Cursor existing = db.rawQuery(
                        "SELECT id FROM sms WHERE address=? AND timestamp=?",
                        new String[]{address, String.valueOf(date)}
                    );

                    if (existing.getCount() == 0) {
                        ContentValues values = new ContentValues();
                        values.put("address", address);
                        values.put("name", "");
                        values.put("body", body);
                        values.put("type", "INBOX");
                        values.put("timestamp", date);
                        values.put("saved_at", System.currentTimeMillis());
                        db.insert("sms", null, values);
                    }
                    existing.close();
                }
                c.close();
            }
            db.close();

            showSms();

        } catch (Exception e) {
            items.clear();
            items.add("Error: " + e.getMessage());
            adapter.notifyDataSetChanged();
        }
    }

    private void showSms() {
        try {
            items.clear();
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT * FROM sms ORDER BY timestamp DESC LIMIT 100", null);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

            while (c.moveToNext()) {
                String address = c.getString(c.getColumnIndexOrThrow("address"));
                String body = c.getString(c.getColumnIndexOrThrow("body"));
                long ts = c.getLong(c.getColumnIndexOrThrow("timestamp"));
                String preview = (body != null && body.length() > 60) ? body.substring(0, 60) + "..." : body;
                items.add(address + "\n" + sdf.format(new Date(ts)) + "\n" + preview);
            }
            c.close();
            db.close();

            if (items.isEmpty()) {
                items.add("No SMS found.");
            } else {
                Toast.makeText(this, items.size() + " SMS loaded!", Toast.LENGTH_SHORT).show();
            }
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            items.clear();
            items.add("Error: " + e.getMessage());
            adapter.notifyDataSetChanged();
        }
    }
}
