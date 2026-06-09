package com.github.bmx666.appcachecleaner.clearcache.node

// Framework-free view of one accessibility node. The clear scenarios + node finders depend
// on this instead of android.view.accessibility.AccessibilityNodeInfo so the whole UI-walk
// decision logic (button finding, retry bounds, force-stop state machine) is unit-testable
// on a plain JVM with a scripted fake tree. AndroidNodeView adapts the real node in prod.
//
// Surface is exactly what the scenarios use: structure (className/viewId/text + children +
// parent), state (enabled/clickable), and the three live actions (click, scroll-forward,
// refresh). refresh() re-reads the node from the live window and reports success; a fake
// models it as a (possibly state-mutating) scripted step.
interface NodeView {
    val className: CharSequence?
    val viewIdResourceName: CharSequence?
    val text: CharSequence?
    val isEnabled: Boolean
    val isClickable: Boolean
    val childCount: Int
    val parent: NodeView?

    fun getChild(index: Int): NodeView?

    fun click(): Boolean
    fun scrollForward(): Boolean
    fun refresh(): Boolean
}
