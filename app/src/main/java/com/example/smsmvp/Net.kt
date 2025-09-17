package com.example.smsmvp

import android.telephony.SmsManager
import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object Net {
    private val TAG = "Net"
    private val client = OkHttpClient()
    
    // TODO: Replace with your actual server URL
    private const val SERVER_URL = "http://YOUR_SERVER_IP:8000"
    
    suspend fun sendSmsToServer(sender: String, body: String) {
        val json = JSONObject().apply {
            put("from", sender)
            put("body", body)
            put("timestamp", System.currentTimeMillis())
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$SERVER_URL/sms/received")
            .post(requestBody)
            .build()
        
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "SMS sent to server successfully")
            } else {
                Log.e(TAG, "Failed to send SMS to server: ${response.code}")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending SMS to server", e)
            throw e
        }
    }
    
    suspend fun pollForCommands() {
        val request = Request.Builder()
            .url("$SERVER_URL/commands/poll")
            .get()
            .build()
        
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                responseBody?.let { processCommands(it) }
            } else {
                Log.e(TAG, "Failed to poll commands: ${response.code}")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "Network error polling commands", e)
            throw e
        }
    }
    
    private fun processCommands(jsonResponse: String) {
        try {
            val json = JSONObject(jsonResponse)
            val commands = json.optJSONArray("commands")
            
            commands?.let {
                for (i in 0 until it.length()) {
                    val command = it.getJSONObject(i)
                    val action = command.getString("action")
                    
                    when (action) {
                        "send_sms" -> {
                            val to = command.getString("to")
                            val message = command.getString("message")
                            sendSms(to, message)
                        }
                        else -> {
                            Log.w(TAG, "Unknown command action: $action")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing commands", e)
        }
    }
    
    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "SMS sent to $phoneNumber: $message")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
        }
    }
}
