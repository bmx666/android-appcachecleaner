package com.github.bmx666.appcachecleaner.fake

import com.github.bmx666.appcachecleaner.clearcache.node.NodeView

// Scriptable in-memory NodeView for pure-JVM scenario tests. Build a tree with child();
// click()/scrollForward()/refresh() return the configured results and record click/refresh
// counts.
//
// State that the Android O+ clear-cache choreography depends on can be scripted to mutate
// across the manager's drive loop:
//  - [disableOnClick]  : a successful click flips the node to disabled, modelling a real
//                        "Clear cache" button that greys out once the cache is gone (the
//                        post-click "still enabled?" guard then sees it satisfied).
//  - [enableOnRefresh] : the FIRST refresh() flips the node to enabled (one-shot), modelling
//                        the enabled-wait loop where the button is initially disabled (cache
//                        still summing) and then settles enabled. One-shot so the later
//                        post-click refresh does not re-enable a button the click greyed out.
class FakeNodeView(
    override val className: CharSequence? = null,
    override val viewIdResourceName: CharSequence? = null,
    override val text: CharSequence? = null,
    isClickable: Boolean = false,
    var enabledState: Boolean = true,
    var clickResult: Boolean = true,
    var scrollResult: Boolean = false,
    var refreshResult: Boolean = true,
    var disableOnClick: Boolean = false,
    var enableOnRefresh: Boolean = false,
) : NodeView {

    private val children = mutableListOf<FakeNodeView>()
    override var parent: NodeView? = null

    // Mutable so disableOnClick can be modelled and tests can flip clickability mid-run.
    var clickableState: Boolean = isClickable
    override val isClickable: Boolean get() = clickableState

    override val isEnabled: Boolean get() = enabledState
    override val childCount: Int get() = children.size
    override fun getChild(index: Int): NodeView? = children.getOrNull(index)

    var clickCount = 0
        private set
    var refreshCount = 0
        private set

    override fun click(): Boolean {
        clickCount++
        if (clickResult && disableOnClick)
            enabledState = false
        return clickResult
    }

    override fun scrollForward(): Boolean = scrollResult

    override fun refresh(): Boolean {
        refreshCount++
        if (refreshResult && enableOnRefresh) {
            enabledState = true
            enableOnRefresh = false // one-shot: don't re-enable after a later click greys it out
        }
        return refreshResult
    }

    /** Attach [node] as a child (sets its parent) and return this for chaining. */
    fun child(node: FakeNodeView): FakeNodeView {
        node.parent = this
        children.add(node)
        return this
    }
}
