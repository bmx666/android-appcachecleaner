package com.github.bmx666.appcachecleaner.util

import android.view.accessibility.AccessibilityNodeInfo
import com.github.bmx666.appcachecleaner.log.Logger

fun AccessibilityNodeInfo.lowercaseCompareText(text: CharSequence?): Boolean {
    return this.lowercaseCompareText(text?.toString())
}

fun AccessibilityNodeInfo.lowercaseCompareText(text: String?): Boolean {
    return this.text?.toString()?.lowercase().contentEquals(text?.lowercase())
}

fun AccessibilityNodeInfo.findByViewIdResourceName(text: String): Boolean {
    return this.viewIdResourceName?.contentEquals(text) == true
}

fun AccessibilityNodeInfo.findByViewIdResourceNames(
    viewIdResourceNames: Array<String>)
: Boolean {
    return viewIdResourceNames.any { viewIdResourceName ->
        this.findByViewIdResourceName(viewIdResourceName)
    }
}

fun AccessibilityNodeInfo.findByViewIdResourceName(regex: Regex): Boolean {
    return this.viewIdResourceName?.matches(regex) == true
}

fun AccessibilityNodeInfo.findNestedChildByViewIdResourceName(
    viewIdResourceName: String)
        : AccessibilityNodeInfo? {
    this.getAllChild().forEach { childNode ->
        childNode?.findNestedChildByViewIdResourceName(viewIdResourceName)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.findByViewIdResourceName(viewIdResourceName)
    }
}

fun AccessibilityNodeInfo.findNestedChildByViewIdResourceNames(
    viewIdResourceNames: Array<String>)
        : AccessibilityNodeInfo? {
    this.getAllChild().forEach { childNode ->
        childNode?.findNestedChildByViewIdResourceNames(viewIdResourceNames)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.findByViewIdResourceNames(viewIdResourceNames)
    }
}

fun AccessibilityNodeInfo.findByClassName(className: String): Boolean {
    return this.className?.contentEquals(className) == true
}

fun AccessibilityNodeInfo.findByClassNames(classNames: Array<String>): Boolean {
    return classNames.any { className ->
        this.findByClassName(className)
    }
}

fun AccessibilityNodeInfo.findNestedChildByClassNames(classNames: Array<String>): AccessibilityNodeInfo? {
    this.getAllChild().forEach { childNode ->
        childNode?.findNestedChildByClassNames(classNames)?.let { return it }
    }

    return this.takeIf { nodeInfo ->
        nodeInfo.findByClassNames(classNames)
    }
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

fun AccessibilityNodeInfo.getAllChild(): Iterator<AccessibilityNodeInfo?> {

    return object : Iterator<AccessibilityNodeInfo?> {

        val childCount = this@getAllChild.childCount
        var currentIdx = 0

        override fun hasNext(): Boolean {
            return childCount > 0 && currentIdx < childCount
        }

        override fun next(): AccessibilityNodeInfo? {
            return this@getAllChild.getChild(currentIdx++)
        }
    }
}

fun AccessibilityNodeInfo.showTree(eventTime: Long, level: Int) {
    Logger.d("[$eventTime] " + ">".repeat(level) +
            " " + this.className +
            ":" + this.text +
            ":" + this.viewIdResourceName)
    this.getAllChild().forEach { childNode ->
        childNode?.showTree(eventTime, level + 1)
    }
}

private inline fun <reified T> AccessibilityNodeInfo.takeIfMatchesGeneric(
    findTextView: Boolean,
    findButton: Boolean,
    viewIdResourceName: T,
    arrayText: ArrayList<CharSequence>
): AccessibilityNodeInfo? {
    return this.takeIf { nodeInfo ->
        val isTextViewFound = findTextView &&
                nodeInfo.findByClassName("android.widget.TextView")

        val isButtonFound = findButton &&
                nodeInfo.findByClassName("android.widget.Button")

        val isViewIdResourceNameMatch = !(isTextViewFound || isButtonFound) &&
                when (viewIdResourceName) {
                    is String -> nodeInfo.findByViewIdResourceName(viewIdResourceName)
                    is Regex -> nodeInfo.findByViewIdResourceName(viewIdResourceName)
                    else -> false
                }

        (isTextViewFound || isButtonFound || isViewIdResourceNameMatch) &&
            arrayText.any { text -> nodeInfo.lowercaseCompareText(text) }
    }
}

fun AccessibilityNodeInfo.takeIfMatches(
    findTextView: Boolean,
    findButton: Boolean,
    viewIdResourceName: String,
    arrayText: ArrayList<CharSequence>
): AccessibilityNodeInfo? {
    return this.takeIfMatchesGeneric(findTextView, findButton, viewIdResourceName, arrayText)
}

fun AccessibilityNodeInfo.takeIfMatches(
    findTextView: Boolean,
    findButton: Boolean,
    viewIdResourceName: Regex,
    arrayText: ArrayList<CharSequence>
): AccessibilityNodeInfo? {
    return this.takeIfMatchesGeneric(findTextView, findButton, viewIdResourceName, arrayText)
}