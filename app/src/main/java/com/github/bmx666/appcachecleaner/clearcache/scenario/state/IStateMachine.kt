package com.github.bmx666.appcachecleaner.clearcache.scenario.state

internal interface IStateMachine {
    fun waitState(timeoutMs: Long): Boolean
    fun init()
    fun setDelayForNextApp()
    fun setOpenAppInfo()
    fun setFinishCleanApp()
    fun isFinishCleanApp(): Boolean
    fun setInterrupted()
    fun isInterrupted(): Boolean
    fun isDone(): Boolean
}