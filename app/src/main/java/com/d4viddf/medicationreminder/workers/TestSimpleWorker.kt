package com.d4viddf.medicationreminder.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.d4viddf.medicationreminder.data.SimplestDependency

// QUITA @HiltWorker
class TestSimpleWorker /* QUITA @AssistedInject */ constructor(
    appContext: Context,
    workerParams: WorkerParameters,
    private val simplestDependency: SimplestDependency // Dependencia pasada directamente
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val TAG = "TestSimpleWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "TestSimpleWorker (via CustomFactory) is working!")
        Log.d(TAG, "SimplestDependency says: ${simplestDependency.doSomething()}")
        return Result.success()
    }
}