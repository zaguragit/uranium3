package posidon.uranium.graphics

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.GL30
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.ArrayList

class Mesh(
    indices: IntArray,
    vbos: List<VBO>
) {

    class VBO (
        val floats: FloatArray,
        val size: Int
    )

    var vaoId = 0
    var vertexCount = 0

    private var vboIdList = ArrayList<Int>()

    init {
        var indicesBuffer: IntBuffer? = null
        try {
            vertexCount = indices.size
            vaoId = GL30.glGenVertexArrays()
            GL30.glBindVertexArray(vaoId)

            // Index VBO
            val vboId = GL15.glGenBuffers()
            vboIdList.add(vboId)
            indicesBuffer = MemoryUtil.memAllocInt(indices.size)
            indicesBuffer.put(indices).flip()
            GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboId)
            GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW)

            for (i in vbos.indices) {
                val vbo = vbos[i]
                var buffer: FloatBuffer? = null
                try {
                    val vboId = GL15.glGenBuffers()
                    vboIdList.add(vboId)
                    buffer = MemoryUtil.memAllocFloat(vbo.floats.size)
                    buffer.put(vbo.floats).flip()
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
                    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW)
                    GL20.glVertexAttribPointer(i, vbo.size, GL11.GL_FLOAT, false, 0, 0)
                    GL30.glEnableVertexAttribArray(i)
                } finally {
                    if (buffer != null) MemoryUtil.memFree(buffer)
                }
            }
        } finally {
            if (indicesBuffer != null) MemoryUtil.memFree(indicesBuffer)
        }
    }

    fun delete() {
        bind()
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
        for (i in vboIdList.indices) {
            val vboId = vboIdList[i]
            GL20.glDisableVertexAttribArray(i)
            GL15.glDeleteBuffers(vboId)
        }
        GL30.glBindVertexArray(0)
        GL30.glDeleteVertexArrays(vaoId)
    }

    internal fun bind() {
        GL30.glBindVertexArray(vaoId)
        GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboIdList[0])
    }
}