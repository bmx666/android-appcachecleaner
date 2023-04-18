package com.github.bmx666.appcachecleaner.clearcache.scenario.state

import android.os.ConditionVariable

class XiaomiMIUIStateMachine: IStateMachine {

    /***
     * Steps to clean app cache:
     *
     * 1. open "App Info" settings for specific package
     * 2. if Accessibility failed to request event source (null), goto #7
     * 3. If "Clear Data Dialog" state:
     *    3.1. "Clear Cache" Menu Item" not found, goto #7
     *    3.2. "Clear Cache" Menu Item" found:
     *       3.2.1. if menu item is enabled, click it:
     *          3.2.1.1. if perform click was success, switched on "Clear Cache Dialog" and goto #2
     *          3.2.1.2. if perform click failed, goto #8
     *       3.2.2. if menu item is disabled, goto #7
     * 4. If "Clear Cache Dialog" state:
     *    4.1. "OK" button" not found, goto #7
     *    4.2. "OK" button" found:
     *       4.2.1. if button is enabled, click it:
     *          4.2.1.1. if perform click was success, goto #7
     *          4.2.1.2. if perform click failed, goto #8
     *       4.2.2. if button is disabled, goto #7
     * 5. find "clear cache button":
     *    NOTE: some Settings UI can have it on "App Info" settings
     *    5.1. "Clear Cache Button" not found, goto #6
     *    5.2. "Clear Cache Button" found:
     *       5.2.1. if button is enabled, click it:
     *          5.2.1.1. if perform click was success, switched on "Clear Cache Dialog" and goto #2
     *          5.2.1.2. if perform click failed, goto #8
     *       5.2.2. if button is disabled, goto #6
     * 6. find "Clear Data Button":
     *    6.1. "Clear Data Button" not found, goto #7
     *    6.2. "Clear Data Button" found:
     *       6.2.1. if menu is enabled, click it:
     *          6.2.1.1. if perform click was success, switched on "Clear Data Dialog" and goto #2
     *          6.2.1.2. if perform click failed, goto #8
     *       6.2.2. if menu was disabled, goto #7
     * 7. finish app clean process and move to the next
     * 8. interrupt clean process and halt
     ***/

    private enum class STATE {
        INIT,
        OPEN_APP_INFO,
        OPEN_CLEAR_DATA_DIALOG,
        OPEN_CLEAR_CACHE_DIALOG,
        FINISH_CLEAN_APP,
        INTERRUPTED,
    }

    private var state = STATE.INIT
    private val condVarWaitState = ConditionVariable()

    override fun waitState(timeoutMs: Long): Boolean {
        condVarWaitState.close()
        return condVarWaitState.block(timeoutMs)
    }

    override fun init() {
        state = STATE.INIT
    }

    override fun setOpenAppInfo() {
        if (!isInterrupted())
            state = STATE.OPEN_APP_INFO
        condVarWaitState.open()
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

    override fun isDone(): Boolean {
        return isInit() or isInterrupted()
    }

    private fun isInit(): Boolean {
        return state == STATE.INIT
    }

    fun setOpenClearDataDialog() {
        if (!isInterrupted())
            state = STATE.OPEN_CLEAR_DATA_DIALOG
        condVarWaitState.open()
    }

    fun isOpenClearDataDialog(): Boolean {
        return state == STATE.OPEN_CLEAR_DATA_DIALOG
    }

    fun setOpenClearCacheDialog() {
        if (!isInterrupted())
            state = STATE.OPEN_CLEAR_CACHE_DIALOG
        condVarWaitState.open()
    }

    fun isOpenClearCacheDialog(): Boolean {
        return state == STATE.OPEN_CLEAR_CACHE_DIALOG
    }
}