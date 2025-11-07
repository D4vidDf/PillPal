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
        return isXiaomiDevice() &&
                isHyperIslandProtocolSupported(context) &&
                hasFocusPermission(context) &&
                isSupportIsland()
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
            return bundle
        }

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
