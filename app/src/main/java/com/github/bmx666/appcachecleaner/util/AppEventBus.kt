package com.github.bmx666.appcachecleaner.util

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Type-safe, in-process replacement for the deprecated androidx LocalBroadcastManager.
 *
 * Events are broadcast to every active collector (Activity + Accessibility service),
 * mirroring the previous "send Intent to all registered receivers" behavior.
 * replay = 0 -> events emitted while no collector is active are dropped, exactly as
 * LocalBroadcastManager dropped broadcasts with no registered receiver.
 */
sealed interface AppEvent {
    data class AppInfo(val packageName: String?) : AppEvent
    data class ClearCache(val packageList: ArrayList<String>?) : AppEvent
    data class ClearCacheFinish(val message: String?, val packageName: String?) : AppEvent
    data class ClearData(val packageList: ArrayList<String>?) : AppEvent
    data class ClearDataFinish(val message: String?, val packageName: String?) : AppEvent
    data object StopAccessibilityService : AppEvent
    data object StopAccessibilityServiceFeedback : AppEvent
}

object AppEventBus {

    private val _events = MutableSharedFlow<AppEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()

    /** Non-suspending emit; buffer + DROP_OLDEST guarantee it never blocks the caller. */
    fun emit(event: AppEvent) {
        _events.tryEmit(event)
    }
}
