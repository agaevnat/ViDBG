package org.vidbg.scene

import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XSuspendContext

/** One scene per suspended thread — the platform already tracks this, we just list it. */
fun XDebugSession.collectScenes(onDone: (List<Scene>) -> Unit) {
    val suspendContext = suspendContext
    if (suspendContext == null) {
        onDone(emptyList())
        return
    }
    val active = suspendContext.activeExecutionStack

    suspendContext.computeExecutionStacks(object : XSuspendContext.XExecutionStackContainer {
        override fun addExecutionStack(stacks: MutableList<out XExecutionStack>, last: Boolean) {
            if (!last) return
            val scenes = stacks.mapNotNull { stack ->
                stack.topFrame?.let { frame -> Scene(stack.displayName, stack === active, frame) }
            }
            onDone(scenes)
        }

        override fun errorOccurred(errorMessage: String) = onDone(emptyList())
    })
}
