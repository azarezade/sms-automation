package com.example.smsmvp

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.LinearLayout
import android.graphics.Color
import android.graphics.Typeface
import androidx.activity.ComponentActivity
import org.json.JSONArray
import org.json.JSONObject

class LogsActivity : ComponentActivity() {
    private lateinit var logsContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        HttpLogger.init(this)
        setupUI()
        loadLogs()
    }
    
    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }
        
        // Title
        val title = TextView(this).apply {
            text = "HTTP Communication Logs"
            textSize = 20f
            setTypeface(null, Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        }
        layout.addView(title)
        
        // Buttons container
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        
        // Refresh button
        val refreshButton = Button(this).apply {
            text = "Refresh"
            setOnClickListener { loadLogs() }
        }
        buttonContainer.addView(refreshButton)
        
        // Clear logs button
        val clearButton = Button(this).apply {
            text = "Clear Logs"
            setOnClickListener { 
                HttpLogger.clearLogs()
                loadLogs()
            }
        }
        buttonContainer.addView(clearButton)
        
        layout.addView(buttonContainer)
        
        // Logs container in scroll view
        logsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        
        scrollView = ScrollView(this).apply {
            addView(logsContainer)
        }
        
        layout.addView(scrollView)
        setContentView(layout)
    }
    
    private fun loadLogs() {
        logsContainer.removeAllViews()
        
        val logs = HttpLogger.getLogs()
        
        if (logs.length() == 0) {
            val noLogsText = TextView(this).apply {
                text = "No logs available"
                setPadding(0, 16, 0, 16)
                setTextColor(Color.GRAY)
            }
            logsContainer.addView(noLogsText)
            return
        }
        
        // Display logs in reverse order (newest first)
        for (i in logs.length() - 1 downTo 0) {
            try {
                val logEntry = logs.getJSONObject(i)
                val logView = createLogView(logEntry)
                logsContainer.addView(logView)
            } catch (e: Exception) {
                // Skip malformed log entries
            }
        }
        
        // Scroll to top to show newest logs
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_UP) }
    }
    
    private fun createLogView(logEntry: JSONObject): TextView {
        val timestamp = logEntry.optString("timestamp", "Unknown time")
        val type = logEntry.optString("type", "UNKNOWN")
        val url = logEntry.optString("url", "")
        val method = logEntry.optString("method", "")
        val statusCode = logEntry.optInt("statusCode", 0)
        val body = logEntry.optString("body", "")
        val error = logEntry.optString("error", "")
        
        val logText = StringBuilder()
        logText.append("[$timestamp] $type\n")
        
        when (type) {
            "REQUEST" -> {
                logText.append("$method $url\n")
                if (body.isNotEmpty()) {
                    logText.append("Body: $body\n")
                }
            }
            "RESPONSE" -> {
                logText.append("URL: $url\n")
                logText.append("Status: $statusCode\n")
                if (body.isNotEmpty()) {
                    logText.append("Response: $body\n")
                }
                if (error.isNotEmpty()) {
                    logText.append("Error: $error\n")
                }
            }
        }
        
        return TextView(this).apply {
            text = logText.toString()
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            
            // Color code by type
            when (type) {
                "REQUEST" -> setTextColor(Color.parseColor("#0066CC"))
                "RESPONSE" -> {
                    if (statusCode in 200..299) {
                        setTextColor(Color.parseColor("#009900"))
                    } else if (error.isNotEmpty()) {
                        setTextColor(Color.parseColor("#CC0000"))
                    } else {
                        setTextColor(Color.parseColor("#FF6600"))
                    }
                }
                else -> setTextColor(Color.BLACK)
            }
            
            // Add margin between log entries
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 8)
            layoutParams = params
        }
    }
}
