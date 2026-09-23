package org.vidbg

import com.intellij.openapi.components.Service
import org.vidbg.snapshot.VariableSnapshot

@Service(Service.Level.PROJECT)
class ViDbgState {
    // A panel can attach after a pause already happened (tool window opened late, or
    // recreated) — replay the last known variables immediately instead of leaving it blank
    // until the next pause.
    var onVariablesChanged: ((List<VariableSnapshot>) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(variables)
        }

    private var variables: List<VariableSnapshot> = emptyList()

    fun publishVariables(variables: List<VariableSnapshot>) {
        this.variables = variables
        onVariablesChanged?.invoke(variables)
    }
}
