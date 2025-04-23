package eu.droogers.smsmatrix

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

/**
 * Created by gerben on 7-10-17.
 */
class MatrixService : Service() {
    private var mx: Matrix? = null
    private var mms: MMSMonitor? = null
    private var mChannelId = ""
    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mChannelId.isEmpty()) {
                mChannelId = createNotificationChannel("sync", "Sync Service")
            }
            val notificationBuilder = NotificationCompat.Builder(this, mChannelId)
            val notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("SMS Matrix Bridge") // Add a title
                .setContentText(applicationInfo.loadLabel(packageManager))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build()

            try {
                startForeground(1, notification)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start foreground service", e)
                // If we can't start foreground, possibly stop the service
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val sp = getSharedPreferences("settings", MODE_PRIVATE)
        val botUsername = sp.getString("botUsername", "")!!
        val botPassword = sp.getString("botPassword", "")!!
        val username = sp.getString("username", "")!!
        val device = sp.getString("device", "")!!
        val hsUrl = sp.getString("hsUrl", "")!!
        val syncDelay = sp.getString("syncDelay", "12")!!
        val syncTimeout = sp.getString("syncTimeout", "60")!!

        val hasValidSettings =
            !botUsername.isEmpty() && !botPassword.isEmpty() && !username.isEmpty() && !device.isEmpty() && !hsUrl.isEmpty() && !syncDelay.isEmpty() && !syncTimeout.isEmpty()

        if (mx == null && hasValidSettings) {
            mx = Matrix(
                application,
                hsUrl,
                botUsername,
                botPassword,
                username,
                device,
                syncDelay,
                syncTimeout
            )
            Log.e(TAG, "onStartCommand: $hsUrl")
            Toast.makeText(this, "service starting:", Toast.LENGTH_SHORT).show()
        } else if (mx == null) {
            Toast.makeText(this, "Missing Information", Toast.LENGTH_SHORT).show()
            // Return immediately if we don't have valid settings to create the mx object
            return START_NOT_STICKY
        }

        Log.e(TAG, "onStartCommand: Service")

        // Process SMS only if we have a valid mx object
        if (intent != null && mx != null) {
            val phone = intent.getStringExtra("SendSms_phone")
            val type = intent.getStringExtra("SendSms_type")
            if (phone != null && type != null) {
                if (type == Matrix.MESSAGE_TYPE_TEXT || type == Matrix.MESSAGE_TYPE_NOTICE) {
                    val body = intent.getStringExtra("SendSms_body")
                    if (body != null) {
                        mx!!.sendMessage(phone, body, type)
                    }
                } else if (type == Matrix.MESSAGE_TYPE_IMAGE || type == Matrix.MESSAGE_TYPE_VIDEO) {
                    val body = intent.getByteArrayExtra("SendSms_body")
                    val fileName = intent.getStringExtra("SendSms_fileName")
                    val contentType = intent.getStringExtra("SendSms_contentType")
                    if (body != null && fileName != null && contentType != null) {
                        mx!!.sendFile(phone, body, type, fileName, contentType)
                    }
                }
            }
        }

        // Initialize MMS monitor only if we have a valid mx object
        if (this.mms == null && mx != null) {
            this.mms = MMSMonitor(this, applicationContext)
            mms!!.startMMSMonitoring()
        }

        return START_NOT_STICKY
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onDestroy() {
        if (mx != null) {
            mx!!.destroy()
        }
        if (this.mms != null) {
            mms!!.stopMMSMonitoring()
            this.mms = null
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "MatrixService"
    }
}
