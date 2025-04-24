package eu.droogers.smsmatrix

import android.Manifest.permission
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Arrays

class MainActivity : Activity() {
    private var sp: SharedPreferences? = null
    private var botUsername: EditText? = null
    private var botPassword: EditText? = null
    private var username: EditText? = null
    private var device: EditText? = null
    private var hsUrl: EditText? = null
    private var syncDelay: EditText? = null
    private var syncTimeout: EditText? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sp = getSharedPreferences("settings", MODE_PRIVATE)
        botUsername = findViewById(R.id.editText_botUsername)
        botPassword = findViewById(R.id.editText_botpassword)
        username = findViewById(R.id.editText_username)
        device = findViewById(R.id.editText_device)
        hsUrl = findViewById(R.id.editText_hsUrl)
        syncDelay = findViewById(R.id.editText_syncDelay)
        syncTimeout = findViewById(R.id.editText_syncTimeout)

        botUsername.setText(sp.getString("botUsername", ""))
        botPassword.setText(sp.getString("botPassword", ""))
        username.setText(sp.getString("username", ""))
        device.setText(sp.getString("device", ""))
        hsUrl.setText(sp.getString("hsUrl", ""))
        syncDelay.setText(sp.getString("syncDelay", "12"))
        syncTimeout.setText(sp.getString("syncTimeout", "30"))

        val saveButton = findViewById<Button>(R.id.button_save)
        saveButton.setOnClickListener { v: View? ->
            if (!checkPermissions()) {
                askPermissions()
            } else {
                saveSettingsAndStartService()
            }
        }

        if (!checkPermissions()) {
            askPermissions()
        } else {
            startService()
        }
    }

    private fun saveSettingsAndStartService() {
        val editor = sp!!.edit()
        editor.putString("botUsername", botUsername!!.text.toString())
        editor.putString("botPassword", botPassword!!.text.toString())
        editor.putString("username", username!!.text.toString())
        editor.putString("device", device!!.text.toString())
        editor.putString("hsUrl", hsUrl!!.text.toString())
        editor.putString("syncDelay", syncDelay!!.text.toString())
        editor.putString("syncTimeout", syncTimeout!!.text.toString())
        editor.apply()

        Log.e(ContentValues.TAG, "onClick: " + botUsername!!.text.toString())
        startService()
    }

    private fun checkPermissions(): Boolean {
        val requiredPermissions: MutableList<String> =
            ArrayList(Arrays.asList(*PERMISSIONS_REQUIRED_BASE))

        // Add POST_NOTIFICATIONS permission for Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(permission.POST_NOTIFICATIONS)
        }

        for (permission in requiredPermissions) {
            val result = ContextCompat.checkSelfPermission(applicationContext, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun askPermissions() {
        val requiredPermissions: MutableList<String> =
            ArrayList(Arrays.asList(*PERMISSIONS_REQUIRED_BASE))

        // Add POST_NOTIFICATIONS permission for Android 13+ (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions.add(permission.POST_NOTIFICATIONS)
        }

        ActivityCompat.requestPermissions(
            this,
            requiredPermissions.toTypedArray<String>(),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }

            if (allGranted) {
                saveSettingsAndStartService()
            } else {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun startService() {
        val intent = Intent(this, MatrixService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    companion object {
        var mx: Matrix? = null
        private val PERMISSIONS_REQUIRED_BASE = arrayOf(
            permission.READ_SMS,
            permission.SEND_SMS,
            permission.RECEIVE_SMS,
            permission.READ_PHONE_STATE,
            permission.READ_CONTACTS,
            permission.READ_EXTERNAL_STORAGE
        )
        private const val PERMISSION_REQUEST_CODE = 200
    }
}