package com.github.bmx666.appcachecleaner.util

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultEventBusTest {

    @Test
    fun `emit is delivered to an active collector`() = runTest {
        val bus = DefaultEventBus()
        bus.events.test {
            bus.emit(AppEvent.AppInfo("com.example"))
            assertEquals(AppEvent.AppInfo("com.example"), awaitItem())

            bus.emit(AppEvent.StopAccessibilityService)
            assertEquals(AppEvent.StopAccessibilityService, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
