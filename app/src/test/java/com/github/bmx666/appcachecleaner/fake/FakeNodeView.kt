package com.github.bmx666.appcachecleaner.fake

import com.github.bmx666.appcachecleaner.clearcache.node.NodeView

// Scriptable in-memory NodeView for pure-JVM scenario tests. Build a tree with child();
// click()/scrollForward()/refresh() return the configured results and record click count.
class FakeNodeView(
    override val className: CharSequence? = null,
    override val viewIdResourceName: CharSequence? = null,
    override val text: CharSequence? = null,
    override val isClickable: Boolean = false,
    var enabledState: Boolean = true,
    var clickResult: Boolean = true,
    var scrollResult: Boolean = false,
    var refreshResult: Boolean = true,
) : NodeView {

    private val children = mutableListOf<FakeNodeView>()
    override var parent: NodeView? = null

    override val isEnabled: Boolean get() = enabledState
    override val childCount: Int get() = children.size
    override fun getChild(index: Int): NodeView? = children.getOrNull(index)

    var clickCount = 0
        private set

    override fun click(): Boolean {
        clickCount++
        return clickResult
    }

    override fun scrollForward(): Boolean = scrollResult
    override fun refresh(): Boolean = refreshResult

    /** Attach [node] as a child (sets its parent) and return this for chaining. */
    fun child(node: FakeNodeView): FakeNodeView {
        node.parent = this
        children.add(node)
        return this
    }
}
