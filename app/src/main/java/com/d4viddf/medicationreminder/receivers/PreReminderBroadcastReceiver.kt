package com.d4viddf.medicationreminder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.d4viddf.medicationreminder.services.PreReminderForegroundService
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
import com.d4viddf.medicationreminder.utils.constants.IntentExtraConstants

class PreReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("PreReminderReceiver", "onReceive called with action: ${intent.action}")
        if (intent.action == IntentActionConstants.ACTION_TRIGGER_PRE_REMINDER_SERVICE) {
            val serviceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
                // Copy all extras from the broadcast intent to the service intent
                putExtras(intent.extras ?: return)
            }
            Log.d("PreReminderReceiver", "Starting PreReminderForegroundService")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
