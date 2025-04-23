package eu.droogers.smsmatrix;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.READ_SMS;
import static android.Manifest.permission.RECEIVE_SMS;
import static android.Manifest.permission.SEND_SMS;
import static android.Manifest.permission.POST_NOTIFICATIONS;
import static android.content.ContentValues.TAG;

public class MainActivity extends Activity {
    static Matrix mx;
    private SharedPreferences sp;
    private EditText botUsername;
    private EditText botPassword;
    private EditText username;
    private EditText device;
    private EditText hsUrl;
    private EditText syncDelay;
    private EditText syncTimeout;
    private static final String[] PERMISSIONS_REQUIRED_BASE = new String[]{
            READ_SMS, SEND_SMS, RECEIVE_SMS, READ_PHONE_STATE, READ_CONTACTS, READ_EXTERNAL_STORAGE
    };
    private static final int PERMISSION_REQUEST_CODE = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = getSharedPreferences("settings", Context.MODE_PRIVATE);
        botUsername = findViewById(R.id.editText_botUsername);
        botPassword = findViewById(R.id.editText_botpassword);
        username = findViewById(R.id.editText_username);
        device = findViewById(R.id.editText_device);
        hsUrl = findViewById(R.id.editText_hsUrl);
        syncDelay = findViewById(R.id.editText_syncDelay);
        syncTimeout = findViewById(R.id.editText_syncTimeout);

        botUsername.setText(sp.getString("botUsername", ""));
        botPassword.setText(sp.getString("botPassword", ""));
        username.setText(sp.getString("username", ""));
        device.setText(sp.getString("device", ""));
        hsUrl.setText(sp.getString("hsUrl", ""));
        syncDelay.setText(sp.getString("syncDelay", "12"));
        syncTimeout.setText(sp.getString("syncTimeout", "30"));

        Button saveButton = findViewById(R.id.button_save);
        saveButton.setOnClickListener(v -> {
            if (!checkPermissions()) {
                askPermissions();
            } else {
                saveSettingsAndStartService();
            }
        });

        if (!checkPermissions()) {
            askPermissions();
        } else {
            startService();
        }
    }

    private void saveSettingsAndStartService() {
        SharedPreferences.Editor editor = sp.edit();
        editor.putString("botUsername", botUsername.getText().toString());
        editor.putString("botPassword", botPassword.getText().toString());
        editor.putString("username", username.getText().toString());
        editor.putString("device", device.getText().toString());
        editor.putString("hsUrl", hsUrl.getText().toString());
        editor.putString("syncDelay", syncDelay.getText().toString());
        editor.putString("syncTimeout", syncTimeout.getText().toString());
        editor.apply();

        Log.e(TAG, "onClick: " + botUsername.getText().toString());
        startService();
    }

    private boolean checkPermissions() {
        List<String> requiredPermissions = new ArrayList<>(Arrays.asList(PERMISSIONS_REQUIRED_BASE));

        // Add POST_NOTIFICATIONS permission for Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(POST_NOTIFICATIONS);
        }

        for (String permission: requiredPermissions) {
            int result = ContextCompat.checkSelfPermission(getApplicationContext(), permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void askPermissions() {
        List<String> requiredPermissions = new ArrayList<>(Arrays.asList(PERMISSIONS_REQUIRED_BASE));

        // Add POST_NOTIFICATIONS permission for Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(POST_NOTIFICATIONS);
        }

        ActivityCompat.requestPermissions(this,
                requiredPermissions.toArray(new String[0]),
                PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                saveSettingsAndStartService();
            } else {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_LONG).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void startService() {
        Intent intent = new Intent(this, MatrixService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}