package com.d4viddf.medicationreminder.utils

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.d4viddf.medicationreminder.R
import com.d4viddf.medicationreminder.data.model.MedicationForm
import com.d4viddf.medicationreminder.receivers.ReminderBroadcastReceiver
import com.d4viddf.medicationreminder.services.PreReminderForegroundService
import com.d4viddf.medicationreminder.utils.constants.IntentActionConstants
import com.d4viddf.medicationreminder.utils.constants.IntentExtraConstants
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object HyperIslandUtil {

    private const val TAG = "HyperIslandUtil"
    private const val HYPER_ISLAND_PROTOCOL_VERSION = 3
    const val ACTION_KEY_MARK_AS_TAKEN = "miui.focus.action.mark_as_taken"
    const val ACTION_KEY_STOP_REMINDER = "miui.focus.action.stop_reminder"


    fun isSupported(context: Context): Boolean {
        Log.d(TAG, "Checking HyperIsland support...")
        val isXiaomi = isXiaomiDevice()
        val hasPermission = hasFocusPermission(context)
        val supportsIsland = isSupportIsland()
        val isSupported = isXiaomi && hasPermission && supportsIsland
        Log.d(TAG, "isSupported: $isSupported")
        return isSupported
    }

    private fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    private fun hasFocusPermission(context: Context): Boolean {
        return try {
            val uri = Uri.parse("content://miui.statusbar.notification.public")
            val extras = Bundle()
            extras.putString("package", context.packageName)
            val bundle = context.contentResolver.call(uri, "canShowFocus", null, extras)
            bundle?.getBoolean("canShowFocus", false) ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking focus permission", e)
            false
        }
    }

    private fun isSupportIsland(): Boolean {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getDeclaredMethod("getBoolean", String::class.java, Boolean::class.java)
            method.invoke(null, "persist.sys.feature.island", false) as Boolean
        } catch (e: Exception) {
            Log.e(TAG, "Error checking island support", e)
            false
        }
    }

    private fun getIconForMedicationForm(medicationForm: MedicationForm?): Int {
        return medicationForm?.imageUrl ?: R.drawable.ic_stat_medication
    }

    fun buildHyperIslandJson(medicationName: String, actualTakeTimeMillis: Long, medicationColor: String?): String {
        val firstWord = medicationName.split(" ").firstOrNull() ?: ""
        val islandParams = JSONObject().apply {
            put("param_v2", JSONObject().apply {
                put("business", "medication_reminder")
                put("updatable", true)
                put("ticker", "Time for $medicationName")
                put("param_island", JSONObject().apply {
                    put("islandProperty", 1)
                    medicationColor?.let { put("highlightColor", it) }
                    put("bigIslandArea", JSONObject().apply {
                        put("picInfo", JSONObject().apply {
                            put("type", 1)
                            put("pic", "miui.focus.pic_imageText")
                        })
                        put("textInfo", JSONObject().apply {
                            put("title", firstWord)
                            put("type", 3) // Countdown timer type
                            put("targetTime", actualTakeTimeMillis)
                        })
                        put("actions", listOf(
                            JSONObject().apply { put("action", ACTION_KEY_MARK_AS_TAKEN) },
                            JSONObject().apply { put("action", ACTION_KEY_STOP_REMINDER) }
                        ))
                    })
                    put("smallIslandArea", JSONObject().apply {
                        put("picInfo", JSONObject().apply {
                            put("type", 1)
                            put("pic", "miui.focus.pic_imageText")
                        })
                        put("textInfo", JSONObject().apply {
                            put("type", 3) // Countdown timer type
                            put("targetTime", actualTakeTimeMillis)
                        })
                    })
                })
                put("baseInfo", JSONObject().apply {
                    put("title", "Next dose: $medicationName")
                    put("content", "Next dose in ${TimeUnit.MILLISECONDS.toMinutes(actualTakeTimeMillis - System.currentTimeMillis())} minutes")
                    medicationColor?.let { put("colorTitle", it) }
                    put("type", 2)
                })
            })
        }
        return islandParams.toString()
    }

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        return ContextCompat.getDrawable(context, drawableId)?.let { drawable ->
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    fun getHyperIslandExtrasBundle(context: Context, reminderId: Int, medicationName: String, actualTakeTimeMillis: Long, medicationColor: String?, medicationForm: MedicationForm?): Bundle {
        val bundle = Bundle()
        if (!isSupported(context)) {
            return bundle
        }

        val islandParams = buildHyperIslandJson(medicationName, actualTakeTimeMillis, medicationColor)
        Log.d(TAG, "HyperIsland JSON Payload: $islandParams")
        bundle.putString("miui.focus.param", islandParams)

        val picsBundle = Bundle()
        val iconResId = getIconForMedicationForm(medicationForm)
        getBitmapFromVectorDrawable(context, iconResId)?.let { bitmap ->
            val icon = Icon.createWithBitmap(bitmap)
            picsBundle.putParcelable("miui.focus.pic_imageText", icon)
        }
        bundle.putBundle("miui.focus.pics", picsBundle)

        // Actions
        val actionsBundle = Bundle()
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Mark as Taken Action
        val markAsActionIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = IntentActionConstants.ACTION_MARK_AS_TAKEN
            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderId)
        }
        val markAsTakenPendingIntent = PendingIntent.getBroadcast(context, reminderId + 1001, markAsActionIntent, pendingIntentFlags)
        val markAsAction = Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_check), context.getString(R.string.prereminder_action_taken), markAsTakenPendingIntent).build()
        actionsBundle.putParcelable(ACTION_KEY_MARK_AS_TAKEN, markAsAction)

        // Stop Reminder Action
        val stopServiceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
            action = IntentActionConstants.ACTION_STOP_PRE_REMINDER
            putExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, reminderId)
        }
        val stopServicePendingIntent = PendingIntent.getService(context, reminderId + 1002, stopServiceIntent, pendingIntentFlags)
        val stopAction = Notification.Action.Builder(Icon.createWithResource(context, R.drawable.rounded_close_24), context.getString(R.string.stop), stopServicePendingIntent).build()
        actionsBundle.putParcelable(ACTION_KEY_STOP_REMINDER, stopAction)

        bundle.putBundle("miui.focus.actions", actionsBundle)

        return bundle
    }
}