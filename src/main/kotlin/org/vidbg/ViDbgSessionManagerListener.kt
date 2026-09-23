package org.vidbg

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.XDebuggerManagerListener
import org.vidbg.enrich.ValueEnricher
import org.vidbg.scene.collectScenes

class ViDbgSessionManagerListener(private val project: Project) : XDebuggerManagerListener {
    override fun processStarted(debugProcess: XDebugProcess) {
        val session = debugProcess.session
        val enricher = ValueEnricher.forProcess(debugProcess)

        session.addSessionListener(object : XDebugSessionListener {
            override fun sessionPaused() {
                session.collectScenes { scenes ->
                    project.service<ViDbgState>().publishScenes(scenes, enricher)
                }
            }
        })
    }
}
