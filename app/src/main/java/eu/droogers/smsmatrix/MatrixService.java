package eu.droogers.smsmatrix;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

/**
 * Created by gerben on 7-10-17.
 */

public class MatrixService extends Service {
    private Matrix mx;
    private MMSMonitor mms;
    private String mChannelId = "";
    private static final String TAG = "MatrixService";

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mChannelId.isEmpty()) {
                mChannelId = createNotificationChannel("sync", "Sync Service");
            }
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, mChannelId);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("SMS Matrix Bridge")  // Add a title
                    .setContentText(getApplicationInfo().loadLabel(getPackageManager()))
                    .setPriority(NotificationCompat.PRIORITY_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            try {
                startForeground(1, notification);
            } catch (Exception e) {
                Log.e(TAG, "Failed to start foreground service", e);
                // If we can't start foreground, possibly stop the service
                stopSelf();
                return START_NOT_STICKY;
            }
        }

        SharedPreferences sp = getSharedPreferences("settings", Context.MODE_PRIVATE);
        String botUsername = sp.getString("botUsername", "");
        String botPassword = sp.getString("botPassword", "");
        String username = sp.getString("username", "");
        String device = sp.getString("device", "");
        String hsUrl = sp.getString("hsUrl", "");
        String syncDelay = sp.getString("syncDelay", "12");
        String syncTimeout = sp.getString("syncTimeout", "60");

        boolean hasValidSettings = !botUsername.isEmpty() && !botPassword.isEmpty() && !username.isEmpty()
                && !device.isEmpty() && !hsUrl.isEmpty() && !syncDelay.isEmpty()
                && !syncTimeout.isEmpty();

        if (mx == null && hasValidSettings) {
            mx = new Matrix(getApplication(), hsUrl, botUsername, botPassword, username, device, syncDelay, syncTimeout);
            Log.e(TAG, "onStartCommand: " + hsUrl);
            Toast.makeText(this, "service starting:", Toast.LENGTH_SHORT).show();
        } else if (mx == null) {
            Toast.makeText(this, "Missing Information", Toast.LENGTH_SHORT).show();
            // Return immediately if we don't have valid settings to create the mx object
            return START_NOT_STICKY;
        }

        Log.e(TAG, "onStartCommand: Service");

        // Process SMS only if we have a valid mx object
        if (intent != null && mx != null) {
            String phone = intent.getStringExtra("SendSms_phone");
            String type = intent.getStringExtra("SendSms_type");
            if (phone != null && type != null) {
                if (type.equals(Matrix.MESSAGE_TYPE_TEXT) || type.equals(Matrix.MESSAGE_TYPE_NOTICE)) {
                    String body = intent.getStringExtra("SendSms_body");
                    if (body != null) {
                        mx.sendMessage(phone, body, type);
                    }
                } else if (type.equals(Matrix.MESSAGE_TYPE_IMAGE) || type.equals(Matrix.MESSAGE_TYPE_VIDEO)) {
                    byte[] body = intent.getByteArrayExtra("SendSms_body");
                    String fileName = intent.getStringExtra("SendSms_fileName");
                    String contentType = intent.getStringExtra("SendSms_contentType");
                    if (body != null && fileName != null && contentType != null) {
                        mx.sendFile(phone, body, type, fileName, contentType);
                    }
                }
            }
        }

        // Initialize MMS monitor only if we have a valid mx object
        if (this.mms == null && mx != null) {
            this.mms = new MMSMonitor(this, getApplicationContext());
            this.mms.startMMSMonitoring();
        }

        return START_NOT_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String createNotificationChannel(String channelId, String channelName){
        NotificationChannel chan = new NotificationChannel(channelId,
                channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager service = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        service.createNotificationChannel(chan);
        return channelId;
    }

    @Override
    public void onDestroy() {
        if (mx != null) {
            mx.destroy();
        }
        if (this.mms != null) {
            this.mms.stopMMSMonitoring();
            this.mms = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
