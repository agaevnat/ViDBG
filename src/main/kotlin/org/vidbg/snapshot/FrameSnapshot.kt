package org.vidbg.snapshot

import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XDebuggerTreeNodeHyperlink
import com.intellij.xdebugger.frame.XFullValueEvaluator
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValue
import com.intellij.xdebugger.frame.XValueChildrenList
import com.intellij.xdebugger.frame.XValueContainer
import com.intellij.xdebugger.frame.XValueGroup
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import org.vidbg.enrich.ValueEnricher
import javax.swing.Icon

/**
 * Locals of a container, pulled lazily on demand — same expand model real debuggers use.
 * Also expands any groups the debugger attaches (e.g. Java's "Static" group for the current
 * frame's class), flattened into the same list with [VariableSnapshot.scope] set to the group
 * name — plus, when called directly on a frame, whatever [ValueEnricher.globalsOf] contributes
 * (statics declared anywhere in the source file, not just the current method's own class).
 */
fun XValueContainer.collectVariables(project: Project, enricher: ValueEnricher?, onDone: (List<VariableSnapshot>) -> Unit) {
    var pending = 2
    val collected = mutableListOf<VariableSnapshot>()

    fun receive(batch: List<VariableSnapshot>) {
        collected += batch
        if (--pending == 0) onDone(collected)
    }

    collectScoped(enricher, scope = null, ::receive)

    if (this is XStackFrame && enricher != null) {
        enricher.globalsOf(project, this) { globals -> resolveValues(globals, enricher, "Globals", ::receive) }
    } else {
        receive(emptyList())
    }
}

/**
 * [XCompositeNode.addChildren] can be called several times with partial batches before the
 * final one (`last = true`) — each call's list is only that batch, not the cumulative set, so
 * entries/groups must be accumulated across calls rather than read off the last call alone.
 */
private fun XValueContainer.collectScoped(enricher: ValueEnricher?, scope: String?, onDone: (List<VariableSnapshot>) -> Unit) {
    val allEntries = mutableListOf<Pair<String, XValue>>()
    val allGroups = mutableListOf<XValueGroup>()

    computeChildren(object : XCompositeNode {
        override fun addChildren(children: XValueChildrenList, last: Boolean) {
            for (i in 0 until children.size()) allEntries += children.getName(i) to children.getValue(i)
            allGroups += children.topGroups
            allGroups += children.bottomGroups

            if (!last) return

            var pending = 1 + allGroups.size
            val collected = mutableListOf<VariableSnapshot>()

            fun receive(batch: List<VariableSnapshot>) {
                collected += batch
                if (--pending == 0) onDone(collected)
            }

            resolveValues(allEntries, enricher, scope, ::receive)
            for (group in allGroups) {
                group.collectScoped(enricher, group.name, ::receive)
            }
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun tooManyChildren(remaining: Int) = Unit
        override fun setAlreadySorted(alreadySorted: Boolean) = Unit
        override fun setErrorMessage(errorMessage: String) = onDone(emptyList())
        override fun setErrorMessage(errorMessage: String, link: XDebuggerTreeNodeHyperlink?) = onDone(emptyList())
        override fun setMessage(message: String, icon: Icon?, attributes: SimpleTextAttributes, link: XDebuggerTreeNodeHyperlink?) = Unit
    })
}

private fun resolveValues(
    entries: List<Pair<String, XValue>>,
    enricher: ValueEnricher?,
    scope: String?,
    onDone: (List<VariableSnapshot>) -> Unit,
) {
    if (entries.isEmpty()) {
        onDone(emptyList())
        return
    }

    val results = arrayOfNulls<VariableSnapshot>(entries.size)
    var remaining = entries.size

    entries.forEachIndexed { index, (name, xValue) ->
        xValue.computePresentation(object : XValueNode {
            override fun setPresentation(icon: Icon?, type: String?, value: String, hasChildren: Boolean) {
                results[index] = VariableSnapshot(name, type ?: enricher?.typeOf(xValue), value, hasChildren, scope, enricher?.identityOf(xValue))
                if (--remaining == 0) onDone(results.filterNotNull())
            }

            override fun setPresentation(icon: Icon?, presentation: XValuePresentation, hasChildren: Boolean) {
                val text = StringBuilder()
                presentation.renderValue(object : XValuePresentation.XValueTextRenderer {
                    override fun renderValue(value: String) { text.append(value) }
                    override fun renderStringValue(value: String) { text.append(value) }
                    override fun renderNumericValue(value: String) { text.append(value) }
                    override fun renderKeywordValue(value: String) { text.append(value) }
                    override fun renderValue(value: String, key: TextAttributesKey) { text.append(value) }
                    override fun renderStringValue(value: String, additionalSpecialCharsToHighlight: String?, maxLength: Int) { text.append(value) }
                    override fun renderComment(comment: String) = Unit
                    override fun renderSpecialSymbol(symbol: String) { text.append(symbol) }
                    override fun renderError(error: String) { text.append(error) }
                })
                results[index] = VariableSnapshot(name, presentation.type ?: enricher?.typeOf(xValue), text.toString(), hasChildren, scope, enricher?.identityOf(xValue))
                if (--remaining == 0) onDone(results.filterNotNull())
            }

            override fun setFullValueEvaluator(fullValueEvaluator: XFullValueEvaluator) = Unit
        }, XValuePlace.TREE)
    }
}
