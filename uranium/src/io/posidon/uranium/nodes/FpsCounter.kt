package io.posidon.uranium.nodes

import io.posidon.uranium.graphics.Renderer
import io.posidon.uranium.nodes.spatial.Eye
import kotlin.math.round

class FpsCounter : Node() {

    private var renderCycles = 0
    private var localDelta = 0.0

    var fps: Int = 0
        private set

    override fun render(renderer: Renderer, eye: Eye) {
        super.render(renderer, eye)
        renderCycles++
    }

    override fun update(delta: Double) {
        super.update(delta)
        localDelta += delta
        if (localDelta > 1.0) {
            val frames = renderCycles
            renderCycles = 0
            fps = round(frames / localDelta).toInt()
            localDelta = 0.0
        }
    }
}