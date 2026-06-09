package com.github.bmx666.appcachecleaner.fake

import com.github.bmx666.appcachecleaner.platform.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher

// Routes every role to the one test dispatcher so coroutine work is deterministic.
class TestDispatcherProvider(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val main: CoroutineDispatcher get() = dispatcher
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
}
