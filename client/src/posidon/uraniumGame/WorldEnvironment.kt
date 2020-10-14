package posidon.uraniumGame

import posidon.library.types.Vec3f
import posidon.uranium.nodes.environment.Environment
import posidon.uranium.nodes.environment.setSunRotationDeg
import kotlin.math.abs
import kotlin.math.pow

class WorldEnvironment : Environment {

    companion object {
        private val SKY_NORMAL = Vec3f(0.4f, 0.65f, 0.956f)
        private val SKY_NIGHT = Vec3f(0f, 0.04f, 0.113f)
        private val SKY_LIGHT_DAY = Vec3f(1f, 1f, 1f)
        private val SKY_LIGHT_NIGHT = Vec3f(0f, 0f, 0f)
        private val AMBIENT_LIGHT_DAY = Vec3f(.9f, .9f, .9f)
        private val AMBIENT_LIGHT_NIGHT = Vec3f(0.09f, 0.137f, 0.180f)
        private const val MAX_TIME = 600.0
    }

    override val skyColor = Vec3f.zero()
    override val skyLight = Vec3f.zero()
    override val ambientLight = Vec3f.zero()
    override val sunNormal = Vec3f(0f, 1f, 0f)

    var time = MAX_TIME / 2.0
    var timeSpeed = 1

    override fun update(delta: Double) {
        time += timeSpeed * delta
        time %= MAX_TIME

        val a = (time - MAX_TIME / 2f) / MAX_TIME * 2

        skyColor.set(Vec3f.blend(SKY_NIGHT, SKY_NORMAL, a.pow(2.0).toFloat()))
        skyLight.set(Vec3f.blend(SKY_LIGHT_NIGHT, SKY_LIGHT_DAY, abs(a).toFloat()))
        ambientLight.set(Vec3f.blend(AMBIENT_LIGHT_NIGHT, AMBIENT_LIGHT_DAY, a.pow(2.0).toFloat()))

        setSunRotationDeg(a * 180.0)
    }
}