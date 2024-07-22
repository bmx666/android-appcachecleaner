package com.github.bmx666.appcachecleaner.clearcache.scenario.state

import android.os.ConditionVariable

internal class DefaultStateMachine: IStateMachine {

    /***
     * Steps to clean app cache:
     *
     * 1. open "App Info" settings for specific package
     * 2. if Accessibility failed to request event source (null), goto #6
     * 3. find "clear cache button":
     *    NOTE: some Settings UI can have it on "App Info" settings
     *    3.1. "clear cache button" not found, goto #4
     *    3.2. "clear cache button" found:
     *       3.2.1. if button is enabled, click it:
     *          3.2.1.1. if perform click was success, goto #6
     *          3.2.1.2. if perform click failed, goto #7
     *       3.2.2. if button is disabled (cache not calculated yet or 0 bytes), goto #6
     * 4. find "storage menu":
     *    4.1. "storage menu" not found, goto #6
     *    4.2. "storage menu" found:
     *       4.2.1. if menu is enabled, click it:
     *          4.2.1.1. if perform click was success, switched on "Storage Info" and goto #2
     *          4.2.1.2. if perform click failed, goto #7
     *       4.2.2. if menu was disabled, goto #6
     * 5. find RecyclerView:
     *    5.1. RecyclerView not found, goto #6
     *    5.2. RecyclerView found:
     *       5.2.1. if RecyclerView is enabled, scroll forward it:
     *          5.2.1.1. if perform scroll forward was success:
     *             5.2.1.1.1. wait minimal timeout for perform action
     *             5.2.1.1.2. if RecyclerView refresh failed, goto #6
     *             5.2.1.1.3. wait minimal timeout for perform action and goto #4
     *          5.2.1.2. if perform scroll forward failed, goto #6
     * 6. finish app clean process and move to the next
     * 7. interrupt clean process and halt
     ***/

    private enum class STATE {
        INIT,
        OPEN_APP_INFO,
        ACCESSIBILITY_EVENT,
        OPEN_STORAGE_INFO,
        FINISH_CLEAN_APP,
        INTERRUPTED,
        DELAY_FOR_NEXT_APP,
    }

    private var state = STATE.INIT
    private val condVarWaitEvent = ConditionVariable()
    private val condVarWaitState = ConditionVariable()
    private var interruptedByUser = false
    private var interruptedByAccessibilityEvent = false

    override fun waitAccessibilityEvent(timeoutMs: Long): Boolean {
        condVarWaitEvent.close()
        return condVarWaitEvent.block(timeoutMs)
    }

    override fun waitState(timeoutMs: Long): Boolean {
        condVarWaitState.close()
        return condVarWaitState.block(timeoutMs)
    }

    override fun init() {
        state = STATE.INIT
    }

    override fun setDelayForNextApp() {
        if (!isInterrupted())
            state = STATE.DELAY_FOR_NEXT_APP
        condVarWaitState.open()
    }

    override fun setAccessibilityEvent() {
        if (!isInterrupted() && isOpenAppInfo())
            state = STATE.ACCESSIBILITY_EVENT
        condVarWaitEvent.open()
    }

    override fun setOpenAppInfo() {
        if (!isInterrupted())
            state = STATE.OPEN_APP_INFO
        condVarWaitState.open()
    }

    override fun isOpenAppInfo(): Boolean {
        return state == STATE.OPEN_APP_INFO
    }

    override fun setFinishCleanApp() {
        if (!isInterrupted())
            state = STATE.FINISH_CLEAN_APP
        condVarWaitState.open()
    }

    override fun isFinishCleanApp(): Boolean {
        return state == STATE.FINISH_CLEAN_APP
    }

    override fun setInterrupted() {
        state = STATE.INTERRUPTED
        condVarWaitState.open()
    }

    override fun isInterrupted(): Boolean {
        return state == STATE.INTERRUPTED
    }

    override fun setInterruptedByUser() {
        interruptedByUser = true
    }

    override fun isInterruptedByUser(): Boolean {
        return interruptedByUser
    }

    override fun setInterruptedByAccessibilityEvent() {
        interruptedByAccessibilityEvent = true
    }

    override fun isInterruptedByAccessibilityEvent(): Boolean {
        return interruptedByAccessibilityEvent
    }

    override fun isDone(): Boolean {
        return isInit() or isInterrupted()
    }

    private fun isInit(): Boolean {
        return state == STATE.INIT
    }

    fun setOpenStorageInfo() {
        if (!isInterrupted())
            state = STATE.OPEN_STORAGE_INFO
        condVarWaitState.open()
    }

    fun isOpenStorageInfo(): Boolean {
        return state == STATE.OPEN_STORAGE_INFO
    }
}