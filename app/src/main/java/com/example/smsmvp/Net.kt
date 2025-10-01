package com.example.smsmvp

import android.telephony.SmsManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    
    // Device ID - you can change this to identify your phone
    private const val DEVICE_ID = "android_phone_1"
    
    suspend fun sendIncomingSmsToServer(sender: String, body: String): String? {
        val json = JSONObject().apply {
            put("device_id", DEVICE_ID)
            put("frm", sender)
            put("text", body)
            put("received_at", System.currentTimeMillis() / 1000.0)
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val url = "$SERVER_URL/incoming"
        
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
                Log.d(TAG, "Incoming SMS sent to server successfully")
                HttpLogger.logResponse(url, response.code, responseBodyString)
                
                // Parse response to check if we should reply
                return try {
                    val responseJson = JSONObject(responseBodyString)
                    if (responseJson.optBoolean("should_reply", false)) {
                        responseJson.optString("reply_text", null)
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse server response", e)
                    null
                }
            } else {
                Log.e(TAG, "Failed to send incoming SMS to server: ${response.code}")
                HttpLogger.logResponse(url, response.code, responseBodyString, "HTTP ${response.code}")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "Network error sending incoming SMS to server", e)
            HttpLogger.logResponse(url, 0, null, e.message)
            throw e
        }
        return null
    }
    
    suspend fun pollForTasks() {
        val url = "$SERVER_URL/tasks?device_id=$DEVICE_ID&max_items=5"
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
                    processTasks(responseBodyString)
                }
            } else {
                Log.e(TAG, "Failed to poll tasks: ${response.code}")
                HttpLogger.logResponse(url, response.code, responseBodyString, "HTTP ${response.code}")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "Network error polling tasks", e)
            HttpLogger.logResponse(url, 0, null, e.message)
            throw e
        }
    }
    
    suspend fun registerDevice() {
        val json = JSONObject().apply {
            put("device_id", DEVICE_ID)
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val url = "$SERVER_URL/register"
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        
        HttpLogger.logRequest(url, "POST", json.toString())
        
        try {
            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "Device registered successfully")
                HttpLogger.logResponse(url, response.code, responseBodyString)
            } else {
                Log.e(TAG, "Failed to register device: ${response.code}")
                HttpLogger.logResponse(url, response.code, responseBodyString, "HTTP ${response.code}")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "Network error registering device", e)
            HttpLogger.logResponse(url, 0, null, e.message)
        }
    }
    
    private fun processTasks(jsonResponse: String) {
        try {
            // Parse the response object first
            val responseObj = JSONObject(jsonResponse)
            val tasks = responseObj.getJSONArray("tasks")
            
            Log.d(TAG, "Processing ${tasks.length()} task(s)")
            
            for (i in 0 until tasks.length()) {
                val task = tasks.getJSONObject(i)
                val taskType = task.getString("type")
                val taskId = task.getString("task_id")
                
                when (taskType) {
                    "send_sms" -> {
                        val to = task.getString("to")
                        val text = task.getString("text")
                        Log.d(TAG, "Sending SMS to $to: $text")
                        sendSms(to, text, taskId)
                    }
                    else -> {
                        Log.w(TAG, "Unknown task type: $taskType")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing tasks: ${e.message}", e)
        }
    }
    
    private fun sendSms(phoneNumber: String, message: String, taskId: String? = null) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d(TAG, "SMS sent to $phoneNumber: $message")
            
            // Report success to server if we have a task ID
            taskId?.let { 
                CoroutineScope(Dispatchers.IO).launch {
                    reportTaskStatus(it, "sent", "SMS sent successfully")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send SMS", e)
            
            // Report failure to server if we have a task ID
            taskId?.let { 
                CoroutineScope(Dispatchers.IO).launch {
                    reportTaskStatus(it, "failed", e.message ?: "Unknown error")
                }
            }
        }
    }
    
    private suspend fun reportTaskStatus(taskId: String, result: String, info: String) {
        val json = JSONObject().apply {
            put("device_id", DEVICE_ID)
            put("task_id", taskId)
            put("result", result)
            put("info", info)
        }
        
        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val url = "$SERVER_URL/status"
        
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        
        HttpLogger.logRequest(url, "POST", json.toString())
        
        try {
            val response = client.newCall(request).execute()
            val responseBodyString = response.body?.string() ?: ""
            
            if (response.isSuccessful) {
                Log.d(TAG, "Task status reported successfully")
                HttpLogger.logResponse(url, response.code, responseBodyString)
            } else {
                Log.e(TAG, "Failed to report task status: ${response.code}")
                HttpLogger.logResponse(url, response.code, responseBodyString, "HTTP ${response.code}")
            }
            response.close()
        } catch (e: IOException) {
            Log.e(TAG, "Network error reporting task status", e)
            HttpLogger.logResponse(url, 0, null, e.message)
        }
    }
}
