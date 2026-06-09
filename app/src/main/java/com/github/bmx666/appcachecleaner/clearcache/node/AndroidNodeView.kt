package com.github.bmx666.appcachecleaner.clearcache.node

import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo

// Production NodeView: thin wrapper over a live AccessibilityNodeInfo. parent/getChild wrap
// lazily on access (a fresh wrapper per call is fine - it just forwards to the same live
// node; no identity is relied on). The action methods map straight onto the framework.
class AndroidNodeView(val node: AccessibilityNodeInfo) : NodeView {

    override val className: CharSequence? get() = node.className
    override val viewIdResourceName: CharSequence? get() = node.viewIdResourceName
    override val text: CharSequence? get() = node.text
    override val isEnabled: Boolean get() = node.isEnabled
    override val isClickable: Boolean get() = node.isClickable
    override val childCount: Int get() = node.childCount
    override val parent: NodeView? get() = node.parent?.let { AndroidNodeView(it) }

    override fun getChild(index: Int): NodeView? =
        node.getChild(index)?.let { AndroidNodeView(it) }

    override fun click(): Boolean =
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

    override fun scrollForward(): Boolean =
        node.performAction(
            AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD.id,
            Bundle()
        )

    override fun refresh(): Boolean = node.refresh()
}
