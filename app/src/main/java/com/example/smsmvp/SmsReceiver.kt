package com.example.smsmvp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import android.telephony.SmsMessage
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"
    
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive called! Action: ${intent?.action}")
        
        if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "SMS_RECEIVED_ACTION detected!")
            
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            Log.d(TAG, "Number of messages: ${messages.size}")
            
            for (message in messages) {
                val sender = message.originatingAddress ?: "Unknown"
                val body = message.messageBody ?: ""
                
                Log.d(TAG, "=== INCOMING SMS ===")
                Log.d(TAG, "From: $sender")
                Log.d(TAG, "Body: $body")
                Log.d(TAG, "==================")
                
                // Log to HTTP logs as well for easier viewing
                HttpLogger.logRequest(
                    "INCOMING_SMS",
                    "RECEIVED",
                    "From: $sender\nBody: $body"
                )
                
                // Send SMS data to server and check for auto-reply
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "Sending to server...")
                        val replyText = Net.sendIncomingSmsToServer(sender, body)
                        
                        // If server wants us to reply, send the reply
                        if (replyText != null) {
                            Log.d(TAG, "Server wants us to reply: $replyText")
                            try {
                                val smsManager = SmsManager.getDefault()
                                smsManager.sendTextMessage(sender, null, replyText, null, null)
                                Log.d(TAG, "✅ Auto-reply sent to $sender: $replyText")
                                
                                // Log the auto-reply
                                HttpLogger.logRequest(
                                    "AUTO_REPLY_SENT",
                                    "SUCCESS",
                                    "To: $sender\nReply: $replyText"
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Failed to send auto-reply", e)
                                HttpLogger.logResponse("AUTO_REPLY", 0, null, "Failed: ${e.message}")
                            }
                        } else {
                            Log.d(TAG, "No auto-reply needed")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to communicate with server", e)
                        HttpLogger.logResponse("INCOMING_SMS", 0, null, "Server error: ${e.message}")
                    }
                }
            }
        } else {
            Log.w(TAG, "Received intent with different action: ${intent?.action}")
        }
    }
}
