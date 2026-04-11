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
import android.provider.ContactsContract;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PERM_REQUEST = 100;
    private ListView listView;
    private Button btnCalls, btnSms;
    private TextView tvTitle, tvCount, tvTodayCalls, tvTodaySms;
    private List<String[]> dataList = new ArrayList<>();
    private boolean showingCalls = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        btnCalls = findViewById(R.id.btnCalls);
        btnSms = findViewById(R.id.btnSms);
        tvTitle = findViewById(R.id.tvTitle);
        tvCount = findViewById(R.id.tvCount);
        tvTodayCalls = findViewById(R.id.tvTodayCalls);
        tvTodaySms = findViewById(R.id.tvTodaySms);

        btnCalls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingCalls = true;
                tvTitle.setText("📞 Call Records");
                tvTitle.setBackgroundColor(0xFFE8EAF6);
                tvTitle.setTextColor(0xFF1A237E);
                importAndShowCalls();
            }
        });

        btnSms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showingCalls = false;
                tvTitle.setText("✉️ SMS Records");
                tvTitle.setBackgroundColor(0xFFE8F5E9);
                tvTitle.setTextColor(0xFF1B5E20);
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
            updateTodayBar();
            importAndShowCalls();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        updateTodayBar();
        importAndShowCalls();
    }

    private void updateTodayBar() {
        try {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long todayStart = cal.getTimeInMillis();

            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();

            Cursor cc = db.rawQuery(
                "SELECT COUNT(*) FROM calls WHERE timestamp >= ?",
                new String[]{String.valueOf(todayStart)});
            int callCount = 0;
            if (cc.moveToFirst()) callCount = cc.getInt(0);
            cc.close();

            Cursor sc = db.rawQuery(
                "SELECT COUNT(*) FROM sms WHERE timestamp >= ?",
                new String[]{String.valueOf(todayStart)});
            int smsCount = 0;
            if (sc.moveToFirst()) smsCount = sc.getInt(0);
            sc.close();
            db.close();

            tvTodayCalls.setText("📞 " + callCount + " Calls");
            tvTodaySms.setText("✉️ " + smsCount + " SMS");
        } catch (Exception e) {}
    }

    private String getContactName(String number) {
        try {
            Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
            Cursor c = getContentResolver().query(uri,
                new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME},
                null, null, null);
            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                c.close();
                return name;
            }
            if (c != null) c.close();
        } catch (Exception e) {}
        return null;
    }

    private String formatDuration(long seconds) {
        if (seconds <= 0) return "Missed";
        if (seconds < 60) return seconds + "s";
        long min = seconds / 60;
        long sec = seconds % 60;
        if (min < 60) return min + "m " + sec + "s";
        long hr = min / 60;
        long mn = min % 60;
        return hr + "h " + mn + "m";
    }

    private String getDateLabel(long timestamp) {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayStart = today.getTimeInMillis();
        long yesterdayStart = todayStart - 86400000L;

        if (timestamp >= todayStart) return "TODAY";
        if (timestamp >= yesterdayStart) return "YESTERDAY";
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(timestamp));
    }

    private void importAndShowCalls() {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor c = getContentResolver().query(
                CallLog.Calls.CONTENT_URI, null, null, null,
                CallLog.Calls.DATE + " DESC");

            if (c != null) {
                while (c.moveToNext()) {
                    String number = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.NUMBER));
                    int typeInt = c.getInt(c.getColumnIndexOrThrow(CallLog.Calls.TYPE));
                    long date = c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DATE));
                    long duration = c.getLong(c.getColumnIndexOrThrow(CallLog.Calls.DURATION));
                    String cachedName = c.getString(c.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME));
                    String type = typeInt == CallLog.Calls.INCOMING_TYPE ? "INCOMING" :
                                  typeInt == CallLog.Calls.MISSED_TYPE ? "MISSED" : "OUTGOING";
                    String name = (cachedName != null && !cachedName.isEmpty()) ?
                                   cachedName : getContactName(number);

                    Cursor ex = db.rawQuery(
                        "SELECT id FROM calls WHERE number=? AND timestamp=?",
                        new String[]{number, String.valueOf(date)});
                    if (ex.getCount() == 0) {
                        ContentValues values = new ContentValues();
                        values.put("number", number);
                        values.put("name", name != null ? name : "");
                        values.put("type", type);
                        values.put("duration", duration);
                        values.put("timestamp", date);
                        values.put("saved_at", System.currentTimeMillis());
                        db.insert("calls", null, values);
                    }
                    ex.close();
                }
                c.close();
            }
            db.close();
            updateTodayBar();
            showCalls();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCalls() {
        try {
            dataList.clear();
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT * FROM calls ORDER BY timestamp DESC LIMIT 500", null);
            SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

            String lastSection = "";
            while (c.moveToNext()) {
                String number = c.getString(c.getColumnIndexOrThrow("number"));
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                String type = c.getString(c.getColumnIndexOrThrow("type"));
                long ts = c.getLong(c.getColumnIndexOrThrow("timestamp"));
                long dur = c.getLong(c.getColumnIndexOrThrow("duration"));

                String section = getDateLabel(ts);
                if (!section.equals(lastSection)) {
                    dataList.add(new String[]{"SECTION", section, "", "", "", "", ""});
                    lastSection = section;
                }

                String displayName = (name != null && !name.isEmpty()) ? name : "Unknown";
                String avatar = displayName.substring(0, 1).toUpperCase();
                String badge = "INCOMING".equals(type) ? "Incoming" :
                               "MISSED".equals(type) ? "Missed" : "Outgoing";
                String timeStr = "TODAY".equals(section) || "YESTERDAY".equals(section) ?
                                  timeFmt.format(new Date(ts)) : dateFmt.format(new Date(ts));

                dataList.add(new String[]{avatar, displayName, number, timeStr, badge,
                    formatDuration(dur), type});
            }
            c.close();
            db.close();

            int total = dataList.size();
            tvCount.setText(total + " records");
            setupAdapter(true);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void importAndShowSms() {
        try {
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Uri smsUri = Uri.parse("content://sms/inbox");
            Cursor c = getContentResolver().query(smsUri, null, null, null, "date DESC");

            if (c != null) {
                while (c.moveToNext()) {
                    String address = c.getString(c.getColumnIndexOrThrow("address"));
                    String body = c.getString(c.getColumnIndexOrThrow("body"));
                    long date = c.getLong(c.getColumnIndexOrThrow("date"));
                    String name = getContactName(address);

                    Cursor ex = db.rawQuery(
                        "SELECT id FROM sms WHERE address=? AND timestamp=?",
                        new String[]{address, String.valueOf(date)});
                    if (ex.getCount() == 0) {
                        ContentValues values = new ContentValues();
                        values.put("address", address);
                        values.put("name", name != null ? name : "");
                        values.put("body", body);
                        values.put("type", "INBOX");
                        values.put("timestamp", date);
                        values.put("saved_at", System.currentTimeMillis());
                        db.insert("sms", null, values);
                    }
                    ex.close();
                }
                c.close();
            }
            db.close();
            updateTodayBar();
            showSms();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showSms() {
        try {
            dataList.clear();
            DatabaseHelper dbHelper = new DatabaseHelper(this);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.rawQuery(
                "SELECT * FROM sms ORDER BY timestamp DESC LIMIT 500", null);
            SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

            String lastSection = "";
            while (c.moveToNext()) {
                String address = c.getString(c.getColumnIndexOrThrow("address"));
                String name = c.getString(c.getColumnIndexOrThrow("name"));
                String body = c.getString(c.getColumnIndexOrThrow("body"));
                long ts = c.getLong(c.getColumnIndexOrThrow("timestamp"));

                String section = getDateLabel(ts);
                if (!section.equals(lastSection)) {
                    dataList.add(new String[]{"SECTION", section, "", "", "", "", ""});
                    lastSection = section;
                }

                String displayName = (name != null && !name.isEmpty()) ? name : "Unknown";
                String avatar = displayName.substring(0, 1).toUpperCase();
                String timeStr = "TODAY".equals(section) || "YESTERDAY".equals(section) ?
                                  timeFmt.format(new Date(ts)) : dateFmt.format(new Date(ts));
                String preview = (body != null && body.length() > 50) ?
                                  body.substring(0, 50) + "..." : body;

                dataList.add(new String[]{avatar, displayName, address, timeStr,
                    preview != null ? preview : "", "", ""});
            }
            c.close();
            db.close();

            tvCount.setText(dataList.size() + " records");
            setupAdapter(false);
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setupAdapter(final boolean isCalls) {
        ArrayAdapter<String[]> adapter = new ArrayAdapter<String[]>(
            this, 0, dataList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                String[] item = dataList.get(position);

                if ("SECTION".equals(item[0])) {
                    TextView tv = new TextView(getContext());
                    tv.setText(item[1]);
                    tv.setPadding(24, 12, 24, 8);
                    tv.setTextSize(11f);
                    tv.setTextColor(0xFF757575);
                    tv.setBackgroundColor(0xFFF5F5F5);
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    tv.setLetterSpacing(0.1f);
                    return tv;
                }

                View v = getLayoutInflater().inflate(
                    isCalls ? R.layout.item_call : R.layout.item_sms,
                    parent, false);

                TextView tvAvatar = v.findViewById(R.id.tvAvatar);
                TextView tvName = v.findViewById(R.id.tvName);
                TextView tvNumber = v.findViewById(R.id.tvNumber);
                TextView tvDate = v.findViewById(R.id.tvDate);

                tvAvatar.setText(item[0]);
                tvName.setText(item[1]);
                tvNumber.setText(item[2]);
                tvDate.setText(item[3]);

                if (isCalls) {
                    TextView tvBadge = v.findViewById(R.id.tvBadge);
                    TextView tvDur = v.findViewById(R.id.tvDur);
                    tvBadge.setText(item[4]);
                    tvDur.setText(item[5]);

                    if ("INCOMING".equals(item[6])) {
                        tvAvatar.setBackgroundColor(0xFFE3F2FD);
                        tvAvatar.setTextColor(0xFF0C447C);
                        tvBadge.setBackgroundColor(0xFFE3F2FD);
                        tvBadge.setTextColor(0xFF0C447C);
                    } else if ("MISSED".equals(item[6])) {
                        tvAvatar.setBackgroundColor(0xFFFFEBEE);
                        tvAvatar.setTextColor(0xFFB71C1C);
                        tvBadge.setBackgroundColor(0xFFFFEBEE);
                        tvBadge.setTextColor(0xFFB71C1C);
                    } else {
                        tvAvatar.setBackgroundColor(0xFFF3E5F5);
                        tvAvatar.setTextColor(0xFF4A148C);
                        tvBadge.setBackgroundColor(0xFFF3E5F5);
                        tvBadge.setTextColor(0xFF4A148C);
                    }
                } else {
                    TextView tvBody = v.findViewById(R.id.tvBody);
                    tvBody.setText(item[4]);
                    tvAvatar.setBackgroundColor(0xFFE8F5E9);
                    tvAvatar.setTextColor(0xFF1B5E20);
                }
                return v;
            }
        };
        listView.setAdapter(adapter);
    }
}
