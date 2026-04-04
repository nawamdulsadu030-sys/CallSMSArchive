package com.nowa.callsmsarchive;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
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
                Toast.makeText(MainActivity.this, "Loading...", Toast.LENGTH_SHORT).show();
                loadCalls();
            }
        });

        btnSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Loading...", Toast.LENGTH_SHORT).show();
                loadSms();
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
            loadCalls();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        loadCalls();
    }

    private void loadCalls() {
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
                items.add("No call records yet.\nMake or receive a call first.");
            }
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            items.clear();
            items.add("Error: " + e.getMessage());
            adapter.notifyDataSetChanged();
        }
    }

    private void loadSms() {
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
                items.add("No SMS records yet.\nReceive an SMS first.");
            }
            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            items.clear();
            items.add("Error: " + e.getMessage());
            adapter.notifyDataSetChanged();
        }
    }
}
