package org.vidbg.scene

import com.intellij.xdebugger.frame.XStackFrame

/** One suspended thread's execution line: its top frame + whatever locals/globals are visible from there. */
data class Scene(
    val threadName: String,
    val isCurrent: Boolean,
    val topFrame: XStackFrame,
)
