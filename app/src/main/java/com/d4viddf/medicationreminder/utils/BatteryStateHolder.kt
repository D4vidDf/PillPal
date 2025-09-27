package com.d4viddf.medicationreminder.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BatteryStateHolder @Inject constructor() {
    private val _batteryLevels = MutableSharedFlow<Pair<String, Int>>(replay = 0)
    val batteryLevels = _batteryLevels.asSharedFlow()

    suspend fun newBatteryLevel(nodeId: String, level: Int) {
        _batteryLevels.emit(nodeId to level)
    }
}
