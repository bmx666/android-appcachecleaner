package com.github.bmx666.appcachecleaner.util

import android.os.ConditionVariable

class CleanCacheStateMachine {

    /***
     * Steps to clean app cache:
     *
     * 1. open "App Info" settings for specific package
     * 2. if Accessibility failed to request event source (null), goto #5
     * 3. find "clear cache button":
     *    NOTE: some Settings UI can have it on "App Info" settings
     *    3.1. "clear cache button" not found, goto #4
     *    3.2. "clear cache button" found:
     *       3.2.1. if button is enabled, click it:
     *          3.2.1.1. if perform click was success, goto #5
     *          3.2.1.2. if perform click failed, goto #6
     *       3.2.2. if button is disabled (cache not calculated yet or 0 bytes), goto #5
     * 4. find "storage menu":
     *    4.1. "storage menu" not found, goto #5
     *    4.2. "storage menu" found:
     *       4.2.1. if menu is enabled, click it:
     *          4.2.1.1. if perform click was success, switched on "Storage Info" and goto #2
     *          4.2.1.2. if perform click failed, goto #6
     *       4.2.2. if menu was disabled, goto #5
     * 5. finish app clean process and move to the next
     * 6. interrupt clean process and halt
     ***/

    private enum class STATE {
        INIT,
        OPEN_APP_INFO,
        OPEN_STORAGE_INFO,
        FINISH_CLEAN_APP,
        INTERRUPTED,
    }

    private var state = STATE.INIT
    private val condVarWaitState = ConditionVariable()

    fun waitState(timeoutMs: Long): Boolean {
        condVarWaitState.close()
        return condVarWaitState.block(timeoutMs)
    }

    fun init() {
        state = STATE.INIT
    }

    fun setOpenAppInfo() {
        if (!isInterrupted())
            state = STATE.OPEN_APP_INFO
        condVarWaitState.open()
    }

    fun setStorageInfo() {
        if (!isInterrupted())
            state = STATE.OPEN_STORAGE_INFO
        condVarWaitState.open()
    }

    fun setFinishCleanApp() {
        if (!isInterrupted())
            state = STATE.FINISH_CLEAN_APP
        condVarWaitState.open()
    }

    fun setInterrupted() {
        state = STATE.INTERRUPTED
        condVarWaitState.open()
    }

    fun isInit(): Boolean {
        return state == STATE.INIT
    }

    fun isFinishCleanApp(): Boolean {
        return state == STATE.FINISH_CLEAN_APP
    }

    fun isInterrupted(): Boolean {
        return state == STATE.INTERRUPTED
    }

    fun isDone(): Boolean {
        return isInit() or isInterrupted()
    }
}