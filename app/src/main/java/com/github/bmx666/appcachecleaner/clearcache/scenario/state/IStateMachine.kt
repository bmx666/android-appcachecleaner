package com.github.bmx666.appcachecleaner.clearcache.scenario.state

internal interface IStateMachine {
    fun waitAccessibilityEvent(timeoutMs: Long): Boolean
    fun waitState(timeoutMs: Long): Boolean
    fun init()
    fun setDelayForNextApp()
    fun setAccessibilityEvent()
    fun setOpenAppInfo()
    fun isOpenAppInfo(): Boolean
    fun setFinishCleanApp()
    fun isFinishCleanApp(): Boolean
    fun setInterrupted()
    fun isInterrupted(): Boolean
    fun setInterruptedByUser()
    fun isInterruptedByUser(): Boolean
    fun setInterruptedByAccessibilityEvent()
    fun isInterruptedByAccessibilityEvent(): Boolean
    fun isDone(): Boolean
}