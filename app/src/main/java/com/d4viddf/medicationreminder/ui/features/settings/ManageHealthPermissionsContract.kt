package com.d4viddf.medicationreminder.ui.features.settings

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class ManageHealthPermissionsContract : ActivityResultContract<String, Unit>() {
    override fun createIntent(context: Context, input: String): Intent {
        val intent = Intent("android.intent.action.VIEW_PERMISSION_USAGE")
        intent.addCategory("android.intent.category.HEALTH_PERMISSIONS")
        intent.putExtra(Intent.EXTRA_PACKAGE_NAME, input)
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?) {
        // No result is returned from this intent.
    }
}
