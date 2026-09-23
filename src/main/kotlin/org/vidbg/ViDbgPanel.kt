package org.vidbg

import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import org.vidbg.snapshot.VariableSnapshot
import java.awt.BorderLayout
import javax.swing.DefaultListModel
import javax.swing.JPanel

class ViDbgPanel : JPanel(BorderLayout()) {
    private val variablesModel = DefaultListModel<String>()
    private val variablesList = JBList(variablesModel)

    init {
        add(JBScrollPane(variablesList), BorderLayout.CENTER)
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
