package org.vidbg

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.XDebuggerManagerListener
import org.vidbg.enrich.ValueEnricher
import org.vidbg.snapshot.collectVariables

class ViDbgSessionManagerListener(private val project: Project) : XDebuggerManagerListener {
    override fun processStarted(debugProcess: XDebugProcess) {
        val session = debugProcess.session
        val enricher = ValueEnricher.forProcess(debugProcess)

        session.addSessionListener(object : XDebugSessionListener {
            override fun sessionPaused() {
                val frame = session.currentStackFrame ?: return
                // sessionPaused fires on the debugger's own command thread, not the EDT, and
                // the async chain below may resolve on any thread — the panel is Swing, so the
                // final hop back must be forced onto the EDT rather than assumed.
                frame.collectVariables(project, enricher) { variables ->
                    ApplicationManager.getApplication().invokeLater {
                        project.service<ViDbgState>().publishVariables(variables)
                    }
                }
            }
        })
    }
}
