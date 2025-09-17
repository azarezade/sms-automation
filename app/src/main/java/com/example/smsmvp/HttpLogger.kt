package com.example.smsmvp

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

object HttpLogger {
    private const val PREFS_NAME = "http_logs"
    private const val LOGS_KEY = "logs"
    private const val MAX_LOGS = 100
    
    private lateinit var prefs: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun logRequest(url: String, method: String, requestBody: String? = null) {
        val logEntry = JSONObject().apply {
            put("timestamp", dateFormat.format(Date()))
            put("type", "REQUEST")
            put("url", url)
            put("method", method)
            put("body", requestBody ?: "")
        }
        addLog(logEntry)
    }
    
    fun logResponse(url: String, statusCode: Int, responseBody: String? = null, error: String? = null) {
        val logEntry = JSONObject().apply {
            put("timestamp", dateFormat.format(Date()))
            put("type", "RESPONSE")
            put("url", url)
            put("statusCode", statusCode)
            put("body", responseBody ?: "")
            put("error", error ?: "")
        }
        addLog(logEntry)
    }
    
    private fun addLog(logEntry: JSONObject) {
        if (!::prefs.isInitialized) return
        
        val currentLogs = getLogs()
        currentLogs.put(logEntry)
        
        // Keep only the last MAX_LOGS entries
        if (currentLogs.length() > MAX_LOGS) {
            val trimmedLogs = JSONArray()
            for (i in (currentLogs.length() - MAX_LOGS) until currentLogs.length()) {
                trimmedLogs.put(currentLogs.getJSONObject(i))
            }
            saveLogs(trimmedLogs)
        } else {
            saveLogs(currentLogs)
        }
    }
    
    fun getLogs(): JSONArray {
        if (!::prefs.isInitialized) return JSONArray()
        
        val logsString = prefs.getString(LOGS_KEY, "[]")
        return try {
            JSONArray(logsString)
        } catch (e: Exception) {
            JSONArray()
        }
    }
    
    private fun saveLogs(logs: JSONArray) {
        prefs.edit().putString(LOGS_KEY, logs.toString()).apply()
    }
    
    fun clearLogs() {
        if (::prefs.isInitialized) {
            prefs.edit().remove(LOGS_KEY).apply()
        }
    }
}
