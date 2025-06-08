package com.d4viddf.medicationreminder.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private const val LOG_FILE_NAME = "app_log.txt"
    private const val MAX_LOG_SIZE_BYTES = 1 * 1024 * 1024 // 1 MB
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun initialize(context: Context) {
        try {
            val logDir = File(context.cacheDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            logFile = File(logDir, LOG_FILE_NAME)
            Log.i("FileLogger", "File logger initialized. Log file: ${logFile?.absolutePath}")
            checkLogSize() // Check size on init
        } catch (e: IOException) {
            Log.e("FileLogger", "Error initializing file logger", e)
        }
    }

    private fun checkLogSize() {
        logFile?.let {
            if (it.exists() && it.length() > MAX_LOG_SIZE_BYTES) {
                try {
                    // Simple rotation: delete if too large. Could be improved to rename/archive.
                    it.delete()
                    Log.i("FileLogger", "Log file was too large, deleted for rotation.")
                    // Re-create the file for new logs
                    it.createNewFile()
                } catch (e: IOException) {
                    Log.e("FileLogger", "Error rotating log file", e)
                }
            }
        }
    }

    @Synchronized // Ensure thread safety for file writing
    fun log(tag: String, message: String, throwable: Throwable? = null) {
        if (logFile == null) {
            Log.w("FileLogger", "Logger not initialized. Cannot write to file.")
            return
        }

        // Also log to Logcat for real-time debugging
        when {
            throwable != null -> Log.e(tag, message, throwable)
            tag.endsWith("Error") -> Log.e(tag, message) // Convention for error tags
            else -> Log.d(tag, message) // Default to debug for file logs mirroring Log.d
        }

        try {
            FileWriter(logFile, true).use { writer ->
                val logTimestamp = dateFormat.format(Date())
                writer.append("$logTimestamp [$tag] $message\n")
                if (throwable != null) {
                    writer.append(Log.getStackTraceString(throwable))
                    writer.append("\n")
                }
            }
        } catch (e: IOException) {
            // Avoid logging this error to FileLogger to prevent recursion if file system is full
            Log.e("FileLogger", "Error writing to log file", e)
        }
    }
}
