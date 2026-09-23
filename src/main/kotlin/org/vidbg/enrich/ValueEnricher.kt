package org.vidbg.enrich

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValue

data class ObjectIdentity(val id: Long, val typeName: String)

/**
 * One implementation per debugger backend (JVM/JDI today). The platform's own XValue/XStackFrame
 * layer already covers every language for free; this seam only exists for details a specific
 * debugger exposes beyond that (e.g. real object identity for a reference graph, or resolving
 * "globals" — a concept the generic XDebugger API has no notion of at all).
 *
 * Dispatches on [XDebugProcess], not [com.intellij.xdebugger.XDebugSession] — during
 * XDebuggerManagerListener#processStarted the session's back-reference to its process isn't
 * settled yet, so `session.debugProcess` can NPE there. The process instance is always safe.
 */
interface ValueEnricher {
    fun isApplicable(process: XDebugProcess): Boolean
    fun identityOf(value: XValue): ObjectIdentity?

    /** Declared type when the platform's own presentation omits it (e.g. primitives, String). */
    fun typeOf(value: XValue): String? = null

    /** Static/global values declared in the frame's source file, not just used by it. Optional. */
    fun globalsOf(project: Project, frame: XStackFrame, onDone: (List<Pair<String, XValue>>) -> Unit) {
        onDone(emptyList())
    }

    companion object {
        val EP_NAME: ExtensionPointName<ValueEnricher> =
            ExtensionPointName.create("org.vidbg.valueEnricher")

        fun forProcess(process: XDebugProcess): ValueEnricher? =
            EP_NAME.extensionList.firstOrNull { it.isApplicable(process) }
    }
}
