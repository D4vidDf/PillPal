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
    const val ACTION_KEY_STOP_REMINDER_TEST = "miui.focus.action.stop_reminder_test"


    fun isSupported(context: Context): Boolean {
        Log.d(TAG, "Checking HyperIsland support...")
        val isXiaomi = isXiaomiDevice()
        val hasPermission = hasFocusPermission(context)
        val supportsIsland = isSupportIsland()
        val isSupported = isXiaomi && supportsIsland
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

    fun buildHyperIslandJson(context: Context, medicationId: Int, medicationName: String, actualTakeTimeMillis: Long, medicationColor: String?): String {
        val islandParams = JSONObject().apply {
            put("param_v2", JSONObject().apply {
                put("protocol", HYPER_ISLAND_PROTOCOL_VERSION)
                put("business", "medication_reminder")
                put("updatable", true)
                put("ticker", "Time for $medicationName")
                put("isShownNotification", true)
                put("islandFirstFloat", true)

                val deepLinkUri = "pillpal://medication/$medicationId"
                put("smallWindowInfo", JSONObject().apply {
                    put("targetPage", deepLinkUri)
                })

                put("param_island", JSONObject().apply {
                    put("islandProperty", 1)
                    medicationColor?.let { put("highlightColor", it) }

                    put("bigIslandArea", JSONObject().apply {
                        put("imageTextInfoLeft", JSONObject().apply {
                            put("type", 1)
                            put("picInfo", JSONObject().apply {
                                put("type", 1)
                                put("pic", "miui.focus.pic_imageText")
                            })
                            put("textInfo", JSONObject().apply {
                                put("title", "")
                                put("useHighLight", false)
                            })
                        })

                        val remainingMillis = actualTakeTimeMillis - System.currentTimeMillis()
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
                        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60
                        val timeString = String.format("%02d:%02d", minutes, seconds)

                        put("sameWidthDigitInfo", JSONObject().apply {
                            put("timerInfo", JSONObject().apply {
                                put("timerType", 1)
                                put("timerTotal", TimeUnit.MINUTES.toMillis(30))
                                put("timerWhen", actualTakeTimeMillis - TimeUnit.MINUTES.toMillis(30))
                                put("timerCurrent", remainingMillis)
                            })
                            put("digit", timeString)
                            put("showHighlightColor", true)
                        })
                    })

                    put("smallIslandArea", JSONObject().apply {
                        put("picInfo", JSONObject().apply {
                            put("type", 1)
                            put("pic", "miui.focus.pic_imageText")
                        })
                    })
                })

                val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(actualTakeTimeMillis - System.currentTimeMillis())
                put("chatInfo", JSONObject().apply {
                    put("type", 2)
                    put("title", medicationName)
                    put("content", context.getString(R.string.prereminder_chat_content, minutesRemaining))
                    put("picFunction", "miui.focus.pic_imageText")
                    put("actions", listOf(
                        JSONObject().apply {
                            put("type", 2)
                            put("action", ACTION_KEY_MARK_AS_TAKEN)
                            put("progressInfo", JSONObject().apply {
                                val progress = (100 - (minutesRemaining * 100 / 30)).coerceIn(0, 100)
                                put("progress", progress)
                                medicationColor?.let { put("colorProgress", it) }
                            })
                        }
                    ))
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

    fun getHyperIslandActionAndPicsBundle(context: Context, reminderId: Int, medicationForm: MedicationForm?): Bundle {
        val bundle = Bundle()
        if (!isSupported(context)) {
            return bundle
        }

        val picsBundle = Bundle()
        val iconResId = getIconForMedicationForm(medicationForm)
        getBitmapFromVectorDrawable(context, iconResId)?.let { bitmap ->
            val icon = Icon.createWithBitmap(bitmap)
            // This key MUST match the "pic" value used in the JSON payload (miui.focus.pic_imageText)
            picsBundle.putParcelable("miui.focus.pic_imageText", icon)
        }
        bundle.putBundle("miui.focus.pics", picsBundle)

        // Actions
        val actionsBundle = Bundle()
        // FLAG_IMMUTABLE is highly recommended for security/compatibility
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        // Mark as Taken Action
        val markAsActionIntent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = IntentActionConstants.ACTION_MARK_AS_TAKEN
            putExtra(IntentExtraConstants.EXTRA_REMINDER_ID, reminderId)
        }
        val markAsTakenPendingIntent = PendingIntent.getBroadcast(context, reminderId + 1001, markAsActionIntent, pendingIntentFlags)
        // This key (ACTION_KEY_MARK_AS_TAKEN) MUST match the "action" value used in the JSON payload hintInfo block
        val markAsAction = Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_check), context.getString(R.string.prereminder_action_taken), markAsTakenPendingIntent).build()
        actionsBundle.putParcelable(ACTION_KEY_MARK_AS_TAKEN, markAsAction)

        // Stop Reminder Action
        val stopServiceIntent = Intent(context, PreReminderForegroundService::class.java).apply {
            action = IntentActionConstants.ACTION_STOP_PRE_REMINDER
            putExtra(IntentExtraConstants.EXTRA_SERVICE_REMINDER_ID, reminderId)
        }
        val stopServicePendingIntent = PendingIntent.getService(context, reminderId + 1002, stopServiceIntent, pendingIntentFlags)
        // This key (ACTION_KEY_STOP_REMINDER) MUST match the "action" value used in the JSON payload bigIslandArea actions
        val stopAction = Notification.Action.Builder(Icon.createWithResource(context, R.drawable.rounded_close_24), context.getString(R.string.stop), stopServicePendingIntent).build()
        actionsBundle.putParcelable(ACTION_KEY_STOP_REMINDER, stopAction)

        // Add the actions bundle to the main bundle
        bundle.putBundle("miui.focus.actions", actionsBundle)

        return bundle
    }

    fun showTestNotification(context: Context) {
        if (!isSupported(context)) {
            Log.d(TAG, "Test notification skipped: HyperIsland not supported on this device.")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val currentTime = System.currentTimeMillis()
        val islandParams = """
            {
                "param_v2": {
                    "protocol": 1,
                    "business":"pillpal",
                    "enableFloat": true,
                    "updatable": true,
                    "ticker": "ticker",
                    "tickerPic": "miui.focus.pic_ticker",
                    "smallWindowInfo": {
                        "targetPage": "com.d4viddf.medicationreminder.MainActivity"
                    },
                    "isShownNotification": true,
                    "islandFirstFloat": true,
                    "enableFloat": true,
                     "param_island": {
                        "islandProperty": 1,
                        "bigIslandArea": {
                            "imageTextInfoLeft": {
                                "type": 1,
                                "picInfo": {
                                    "type": 1,
                                    "pic": "miui.focus.pic_imageText"
                                },
                                "textInfo": {
                                    "title": "",
                                    "useHighLight": false
                                }
                            },
                            "sameWidthDigitInfo": {
                                "timerInfo":{
                                    "timerType":1,
                                    "timerTotal":360000,
                                    "timerWhen":360000,
                                    "timerCurrent":0
                                },
                                "digit":"30:00",
                                "showHighlightColor": true
                            }
                        },
                        "smallIslandArea": {
                            "picInfo": {
                                "type": 1,
                                "pic": "miui.focus.pic_imageText"
                            }
                        },
                        "shareData": {
                            "title": "share_title"
                        }
                    },
                    "chatInfo": {
                        "type": 2,
                        "title": "PillPal Test",
                        "content": "Next dosage in 30 minutes",
                        "picFunction": "miui.focus.pic_imageText",
                        "actions": [
                            {
                                "type": 2,
                                "action": "miui.focus.action_test",

                                "progressInfo": {
                                    "progress":20,
                                    "colorProgress":"#FF8514"
                                },
                                "actionIntent":"XXXX"
                            }
                        ]
                    },

                    "actions": [
                            {
                                "type": 2,
                                "action": "miui.focus.action_test",
                                "actionIcon": "miui.focus.pic_imageText",
                                "progressInfo": {
                                    "progress":20,
                                    "colorProgress":"#FF8514"
                                },
                                "actionTitle":"Taken",
                                "actionIntent":"XXXX"
                            }
                        ]


                }
            }
        """.trimIndent()

        val builder = Notification.Builder(context, com.d4viddf.medicationreminder.utils.constants.NotificationConstants.PRE_REMINDER_CHANNEL_ID)
            .setContentTitle("Test Title")
            .setContentText("Test Text")
            .setSmallIcon(R.drawable.ic_stat_medication)

        val bundle = Bundle()
        val actions = Bundle()

        // Action 1 (Progress button)
        val intent = Intent("miui.focus.action_test_clicked")
        val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val action = Notification.Action
            .Builder(Icon.createWithResource(context, R.drawable.ic_check), "Test Action", pendingIntent)
            .build()
        actions.putParcelable("miui.focus.action_test", action)

        // Action 2 (Standard button)
        val stopIntent = Intent("miui.focus.action_stop_test_clicked")
        val stopPendingIntent = PendingIntent.getBroadcast(context, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopAction = Notification.Action
            .Builder(Icon.createWithResource(context, R.drawable.rounded_close_24), "Stop Test", stopPendingIntent)
            .build()
        actions.putParcelable(ACTION_KEY_STOP_REMINDER_TEST, stopAction)

        bundle.putBundle("miui.focus.actions", actions)

        val pics = Bundle()
        // Use the app's full-color launcher icon instead of the black vector
        val appIconBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher_round)
        if (appIconBitmap != null) {
            val icon = Icon.createWithBitmap(appIconBitmap)
            pics.putParcelable("miui.focus.pic_imageText", icon)
            pics.putParcelable("miui.focus.pic_highlight", icon)
            pics.putParcelable("miui.focus.pic_aod", icon)
            pics.putParcelable("miui.focus.pic_ticker", icon)
        }
        bundle.putBundle("miui.focus.pics", pics)

        builder.addExtras(bundle)
        val notification = builder.build()
        notification.extras.putString("miui.focus.param", islandParams)

        val notificationIdUsingTimestamp = System.currentTimeMillis().toInt()


        notificationManager.notify(notificationIdUsingTimestamp, notification) // Using a unique ID for the test notification
        Log.d(TAG, "Test notification dispatched.")
    }
}