package org.vidbg

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import org.vidbg.scene.Scene
import org.vidbg.snapshot.VariableSnapshot
import java.awt.BorderLayout
import javax.swing.DefaultComboBoxModel
import javax.swing.DefaultListModel
import javax.swing.JPanel

class ViDbgPanel : JPanel(BorderLayout()) {
    private val sceneModel = DefaultComboBoxModel<String>()
    private val sceneCombo = ComboBox(sceneModel)
    private val variablesModel = DefaultListModel<String>()
    private val variablesList = JBList(variablesModel)

    private var onSceneSelected: ((Int) -> Unit)? = null
    private var applyingSceneUpdate = false

    init {
        sceneCombo.addActionListener {
            if (!applyingSceneUpdate && sceneCombo.selectedIndex >= 0) {
                onSceneSelected?.invoke(sceneCombo.selectedIndex)
            }
        }
        add(sceneCombo, BorderLayout.NORTH)
        add(JBScrollPane(variablesList), BorderLayout.CENTER)
    }

    fun bindSceneSelection(callback: (Int) -> Unit) {
        onSceneSelected = callback
    }

    /** Called on the platform's callback thread (EDT already, per XValueNode contract). */
    fun renderScenes(scenes: List<Scene>, selected: Int) {
        applyingSceneUpdate = true
        try {
            sceneModel.removeAllElements()
            for (scene in scenes) {
                sceneModel.addElement(if (scene.isCurrent) "${scene.threadName} (stopped here)" else scene.threadName)
            }
            if (selected in scenes.indices) sceneCombo.selectedIndex = selected
        } finally {
            applyingSceneUpdate = false
        }
    }

    fun renderVariables(variables: List<VariableSnapshot>) {
        variablesModel.clear()
        for (v in variables) {
            val scopePrefix = v.scope?.let { "[$it] " }.orEmpty()
            val typePrefix = v.type?.let { "$it " }.orEmpty()
            val identitySuffix = v.identity?.let { " [#${it.id}]" }.orEmpty()
            variablesModel.addElement("$scopePrefix$typePrefix${v.name} = ${v.value}$identitySuffix")
        }
    }
}
