package org.vidbg

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ViDbgToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = ViDbgPanel()
        val state = project.service<ViDbgState>()
        state.onScenesChanged = panel::renderScenes
        state.onVariablesChanged = panel::renderVariables
        panel.bindSceneSelection { index -> state.selectScene(index) }

        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}
