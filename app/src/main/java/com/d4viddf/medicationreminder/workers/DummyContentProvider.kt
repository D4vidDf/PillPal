package com.d4viddf.medicationreminder.workers

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri

abstract class DummyContentProvider : ContentProvider() {
    override fun onCreate() = true

    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?) = null
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0

    override fun getType(uri: Uri) = null
}