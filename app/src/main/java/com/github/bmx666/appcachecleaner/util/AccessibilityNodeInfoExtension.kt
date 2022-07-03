package com.github.bmx666.appcachecleaner.util

import android.view.accessibility.AccessibilityNodeInfo

fun AccessibilityNodeInfo.lowercaseCompareText(text: CharSequence?): Boolean {
    return this.lowercaseCompareText(text?.toString())
}

fun AccessibilityNodeInfo.lowercaseCompareText(text: String?): Boolean {
    return this.text?.toString()?.lowercase().contentEquals(text?.lowercase())
}

fun AccessibilityNodeInfo.findNestedChildByClassName(classNames: Array<String>): AccessibilityNodeInfo? {

    this.getAllChild().forEach { childNode ->
        childNode.findNestedChildByClassName(classNames)?.let { return it }
    }

    classNames.forEach { className ->
        if (this.className?.contentEquals(className) == true)
            return this
    }

    return null
}

fun AccessibilityNodeInfo.findClickable(): AccessibilityNodeInfo? {
    return when {
        this.isClickable -> this
        else -> this.parent?.findClickable()
    }
}

fun AccessibilityNodeInfo.performClick(): Boolean? {
    return this.findClickable()?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
}

fun AccessibilityNodeInfo.getAllChild(): Iterator<AccessibilityNodeInfo> {

    return object : Iterator<AccessibilityNodeInfo> {

        val childCount = this@getAllChild.childCount
        var currentIdx = 0

        override fun hasNext(): Boolean {
            return childCount > 0 && currentIdx < childCount
        }

        override fun next(): AccessibilityNodeInfo {
            return this@getAllChild.getChild(currentIdx++)
        }
    }
}