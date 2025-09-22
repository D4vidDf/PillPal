package com.d4viddf.medicationreminder.ui.features.settings

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient

class ManageHealthPermissionsContract : ActivityResultContract<String, Unit>() {
    override fun createIntent(context: Context, input: String): Intent {
        return Intent(HealthConnectClient.ACTION_MANAGE_HEALTH_PERMISSIONS)
            .putExtra(Intent.EXTRA_PACKAGE_NAME, input)
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
        // No result is returned from this intent.
    }
}
