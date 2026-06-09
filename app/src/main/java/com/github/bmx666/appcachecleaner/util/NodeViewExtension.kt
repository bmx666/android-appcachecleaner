package com.github.bmx666.appcachecleaner.util

import com.github.bmx666.appcachecleaner.clearcache.node.NodeView
import com.github.bmx666.appcachecleaner.log.Logger

fun NodeView.lowercaseCompareText(text: CharSequence?): Boolean {
    return this.lowercaseCompareText(text?.toString())
}

fun NodeView.lowercaseCompareText(text: String?): Boolean {
    return this.text?.toString()?.lowercase().contentEquals(text?.lowercase())
}

fun NodeView.findByViewIdResourceName(text: String): Boolean {
    return this.viewIdResourceName?.contentEquals(text) == true
}

fun NodeView.findByViewIdResourceNames(
    viewIdResourceNames: Array<String>)
: Boolean {
    return viewIdResourceNames.any { viewIdResourceName ->
        this.findByViewIdResourceName(viewIdResourceName)
    }
}

fun NodeView.findByViewIdResourceName(regex: Regex): Boolean {
    return this.viewIdResourceName?.matches(regex) == true
}

/**
 * Single parametrized post-order depth-first walk of the node subtree.
 * Recurses children first and returns the first non-null [transform] result,
 * otherwise applies [transform] to this node. All node finders delegate here
 * to avoid duplicating the recursion (see DefaultClearScenario / XiaomiMIUI).
 * Note: recursive => cannot be inline; [transform] is invoked per node.
 */
fun NodeView.findNode(
    transform: (NodeView) -> NodeView?)
        : NodeView? {
    this.getAllChild().forEach { childNode ->
        childNode?.findNode(transform)?.let { return it }
    }

    return transform(this)
}

fun NodeView.findNestedChildByViewIdResourceName(
    viewIdResourceName: String)
        : NodeView? =
    findNode { node ->
        node.takeIf { it.findByViewIdResourceName(viewIdResourceName) }
    }

fun NodeView.findNestedChildByViewIdResourceNames(
    viewIdResourceNames: Array<String>)
        : NodeView? =
    findNode { node ->
        node.takeIf { it.findByViewIdResourceNames(viewIdResourceNames) }
    }

fun NodeView.findByClassName(className: String): Boolean {
    return this.className?.contentEquals(className) == true
}

fun NodeView.findByClassNames(classNames: Array<String>): Boolean {
    return classNames.any { className ->
        this.findByClassName(className)
    }
}

fun NodeView.findNestedChildByClassNames(classNames: Array<String>): NodeView? =
    findNode { node ->
        node.takeIf { it.findByClassNames(classNames) }
    }

fun NodeView.findClickable(): NodeView? {
    return when {
        this.isClickable -> this
        else -> this.parent?.findClickable()
    }
}

fun NodeView.performClick(): Boolean? {
    return this.findClickable()?.click()
}

fun NodeView.getAllChild(): Iterator<NodeView?> {

    return object : Iterator<NodeView?> {

        val childCount = this@getAllChild.childCount
        var currentIdx = 0

        override fun hasNext(): Boolean {
            return childCount > 0 && currentIdx < childCount
        }

        override fun next(): NodeView? {
            return this@getAllChild.getChild(currentIdx++)
        }
    }
}

fun NodeView.showTree(eventTime: Long, level: Int) {
    Logger.d("[$eventTime] " + ">".repeat(level) +
            " " + this.className +
            ":" + this.text +
            ":" + this.viewIdResourceName)
    this.getAllChild().forEach { childNode ->
        childNode?.showTree(eventTime, level + 1)
    }
}

private inline fun <reified T> NodeView.takeIfMatchesGeneric(
    findTextView: Boolean,
    findButton: Boolean,
    viewIdResourceName: T,
    arrayText: ArrayList<CharSequence>
): NodeView? {
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

fun NodeView.takeIfMatches(
    findTextView: Boolean,
    findButton: Boolean,
    viewIdResourceName: String,
    arrayText: ArrayList<CharSequence>
): NodeView? {
    return this.takeIfMatchesGeneric(findTextView, findButton, viewIdResourceName, arrayText)
}

fun NodeView.takeIfMatches(
    findTextView: Boolean,
    findButton: Boolean,
    viewIdResourceName: Regex,
    arrayText: ArrayList<CharSequence>
): NodeView? {
    return this.takeIfMatchesGeneric(findTextView, findButton, viewIdResourceName, arrayText)
}
