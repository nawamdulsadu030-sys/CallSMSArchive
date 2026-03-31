package com.nowa.callsmsarchive;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 100;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> items = new ArrayList<>();
    private Button btnCalls, btnSms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        btnCalls = findViewById(R.id.btnCalls);
        btnSms = findViewById(R.id.btnSms);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        listView.setAdapter(adapter);

        btnCalls.setOnClickListener(v -> loadCalls());
        btnSms.setOnClickListener(v -> loadSms());

        checkPermissions();
    }

    private void checkPermissions() {
        String[] perms = {
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.READ_PHONE_STATE
        };

        boolean allGranted = true;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, perms, PERM_REQUEST);
        } else {
            loadCalls();
        }
    }

    @Override
    public void onRequestPermissionsResult(int req, String[] perms, int[] results) {
        super.onRequestPermissionsResult(req, perms, results);
        loadCalls();
    }

    private void loadCalls() {
        items.clear();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM calls ORDER BY timestamp DESC", null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        while (c.moveToNext()) {
            String number = c.getString(c.getColumnIndexOrThrow("number"));
            String type = c.getString(c.getColumnIndexOrThrow("type"));
            long ts = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            long dur = c.getLong(c.getColumnIndexOrThrow("duration"));

            String typeLabel = "INCOMING".equals(type) ? "ලැබුණු" : "ගිය";
            String durLabel = dur > 0 ? (dur + " සැකන්ඩ්") : "මිස් කෝල්";
            items.add(typeLabel + " | " + number + "\n" + sdf.format(new Date(ts)) + " | " + durLabel);
        }
        c.close();
        db.close();

        if (items.isEmpty()) items.add("Call records නැත. Call කිහිපයක් කරන්න.");
        adapter.notifyDataSetChanged();
    }

    private void loadSms() {
        items.clear();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM sms ORDER BY timestamp DESC", null);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        while (c.moveToNext()) {
            String address = c.getString(c.getColumnIndexOrThrow("address"));
            String body = c.getString(c.getColumnIndexOrThrow("body"));
            long ts = c.getLong(c.getColumnIndexOrThrow("timestamp"));
            String preview = body != null ? (body.length() > 50 ? body.substring(0, 50) + "..." : body) : "";
            items.add(address + "\n" + sdf.format(new Date(ts)) + "\n" + preview);
        }
        c.close();
        db.close();

        if (items.isEmpty()) items.add("SMS records නැත.");
        adapter.notifyDataSetChanged();
    }
}
