package org.vidbg

import com.intellij.openapi.components.Service
import org.vidbg.enrich.ValueEnricher
import org.vidbg.scene.Scene
import org.vidbg.snapshot.VariableSnapshot
import org.vidbg.snapshot.collectVariables

@Service(Service.Level.PROJECT)
class ViDbgState {
    var onScenesChanged: ((scenes: List<Scene>, selected: Int) -> Unit)? = null
    var onVariablesChanged: ((List<VariableSnapshot>) -> Unit)? = null

    private var scenes: List<Scene> = emptyList()
    private var enricher: ValueEnricher? = null
    private var selected = 0

    fun publishScenes(scenes: List<Scene>, enricher: ValueEnricher?) {
        this.scenes = scenes
        this.enricher = enricher
        val current = scenes.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
        selectScene(current)
    }

    fun selectScene(index: Int) {
        if (index !in scenes.indices) return
        selected = index
        onScenesChanged?.invoke(scenes, selected)
        scenes[selected].topFrame.collectVariables(enricher) { variables ->
            onVariablesChanged?.invoke(variables)
        }
    }
}
