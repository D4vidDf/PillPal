package com.d4viddf.medicationreminder.utils

import android.content.Context
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
import java.util.concurrent.TimeUnit
import org.json.JSONObject

object HyperIslandUtil {

    private const val TAG = "HyperIslandUtil"
    private const val HYPER_ISLAND_PROTOCOL_VERSION = 3

    private fun isSupported(context: Context): Boolean {
        Log.d(TAG, "Checking HyperIsland support...")
        val isXiaomi = isXiaomiDevice()
        Log.d(TAG, "isXiaomiDevice: $isXiaomi")
        val isProtocolSupported = isHyperIslandProtocolSupported(context)
        Log.d(TAG, "isHyperIslandProtocolSupported: $isProtocolSupported")
        val hasPermission = hasFocusPermission(context)
        Log.d(TAG, "hasFocusPermission: $hasPermission")
        val supportsIsland = isSupportIsland()
        Log.d(TAG, "isSupportIsland: $supportsIsland")
        val isSupported = isXiaomi && isProtocolSupported && hasPermission && supportsIsland
        Log.d(TAG, "isSupported: $isSupported")
        return isSupported
    }

    private fun isXiaomiDevice(): Boolean {
        return Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)
    }

    private fun isHyperIslandProtocolSupported(context: Context): Boolean {
        return try {
            val focusProtocolVersion = Settings.System.getInt(
                context.contentResolver,
                "miui_notification_focus_protocol", 0
            )
            Log.d(TAG, "HyperIsland protocol version: $focusProtocolVersion")
            focusProtocolVersion >= HYPER_ISLAND_PROTOCOL_VERSION
        } catch (e: Exception) {
            Log.e(TAG, "Error checking HyperIsland protocol version", e)
            false
        }
    }

    private fun hasFocusPermission(context: Context): Boolean {
        return try {
            val uri = Uri.parse("content://miui.statusbar.notification.public")
            val extras = Bundle()
            extras.putString("package", context.packageName)
            val bundle = context.contentResolver.call(uri, "canShowFocus", null, extras)
            val canShowFocus = bundle?.getBoolean("canShowFocus", false) ?: false
            Log.d(TAG, "Focus permission: $canShowFocus")
            canShowFocus
        } catch (e: Exception) {
            Log.e(TAG, "Error checking focus permission", e)
            false
        }
    }

    private fun isSupportIsland(): Boolean {
        return try {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getDeclaredMethod("getBoolean", String::class.java, Boolean::class.java)
            val isSupported = method.invoke(null, "persist.sys.feature.island", false) as Boolean
            Log.d(TAG, "Island support: $isSupported")
            isSupported
        } catch (e: Exception) {
            Log.e(TAG, "Error checking island support", e)
            false
        }
    }

    private fun getIconForMedicationForm(medicationForm: MedicationForm?): Int {
        return medicationForm?.imageUrl ?: R.drawable.ic_stat_medication
    }

    private fun buildHyperIslandJson(medicationName: String, timeRemainingMillis: Long, medicationColor: String?): String {
        val minutesRemaining = TimeUnit.MILLISECONDS.toMinutes(timeRemainingMillis)
        val islandParams = JSONObject().apply {
            put("param_v2", JSONObject().apply {
                put("business", "medication_reminder")
                put("updatable", true)
                put("ticker", "Time for $medicationName")
                put("param_island", JSONObject().apply {
                    put("islandProperty", 1)
                    medicationColor?.let {
                        put("highlightColor", it)
                    }
                    put("bigIslandArea", JSONObject().apply {
                        put("imageTextInfoLeft", JSONObject().apply {
                            put("type", 1)
                            put("picInfo", JSONObject().apply {
                                put("type", 1)
                                put("pic", "miui.focus.pic_imageText")
                            })
                            put("textInfo", JSONObject().apply {
                                put("frontTitle", "Next dose")
                                put("title", "$minutesRemaining min")
                                put("content", medicationName)
                            })
                        })
                    })
                    put("smallIslandArea", JSONObject().apply {
                        put("picInfo", JSONObject().apply {
                            put("type", 1)
                            put("pic", "miui.focus.pic_imageText")
                        })
                    })
                })
            })
        }
        return islandParams.toString()
    }

    private fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        return ContextCompat.getDrawable(context, drawableId)?.let { drawable ->
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    fun getHyperIslandExtrasBundle(context: Context, medicationName: String, timeRemainingMillis: Long, medicationColor: String?, medicationForm: MedicationForm?): Bundle {
        val bundle = Bundle()
        if (!isSupported(context)) {
            Log.d(TAG, "HyperIsland not supported on this device, returning empty bundle.")
            return bundle
        }

        Log.d(TAG, "HyperIsland is supported, building extras bundle.")
        val islandParams = buildHyperIslandJson(medicationName, timeRemainingMillis, medicationColor)
        bundle.putString("miui.focus.param", islandParams)

        val picsBundle = Bundle()
        val iconResId = getIconForMedicationForm(medicationForm)
        getBitmapFromVectorDrawable(context, iconResId)?.let { bitmap ->
            val icon = Icon.createWithBitmap(bitmap)
            picsBundle.putParcelable("miui.focus.pic_imageText", icon)
            bundle.putBundle("miui.focus.pics", picsBundle)
        }

        return bundle
    }
}
