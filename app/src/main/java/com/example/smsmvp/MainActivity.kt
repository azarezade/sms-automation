package com.example.smsmvp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val SMS_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize HttpLogger
        HttpLogger.init(this)
        
        // Setup UI
        setupUI()
        
        // Request SMS permissions
        if (!hasRequiredPermissions()) {
            requestSmsPermissions()
        } else {
            startPollingService()
        }
        
        Toast.makeText(this, "SMS MVP Started", Toast.LENGTH_SHORT).show()
    }
    
    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        
        val logsButton = Button(this).apply {
            text = "View HTTP Logs"
            textSize = 16f
            setPadding(16, 16, 16, 16)
            setOnClickListener {
                val intent = Intent(this@MainActivity, LogsActivity::class.java)
                startActivity(intent)
            }
        }
        
        layout.addView(logsButton)
        setContentView(layout)
    }

    private fun hasRequiredPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )
        
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_SMS
        )
        
        ActivityCompat.requestPermissions(this, permissions, SMS_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startPollingService()
                Toast.makeText(this, "Permissions granted! Starting service...", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "SMS permissions required for app to work", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startPollingService() {
        val serviceIntent = Intent(this, PollService::class.java)
        startForegroundService(serviceIntent)
    }
}
