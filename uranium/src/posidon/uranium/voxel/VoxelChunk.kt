package posidon.uranium.voxel

import posidon.library.types.Vec3i
import posidon.uranium.graphics.Renderer
import posidon.uranium.graphics.Mesh
import kotlin.collections.ArrayList
import kotlin.concurrent.thread
import kotlin.math.sqrt

abstract class VoxelChunk<V : Voxel>(
    val position: Vec3i,
    val chunkMap: VoxelChunkMap<*>
) {

    inline val size get() = chunkMap.chunkSize
    val blocks = arrayOfNulls<Voxel>(size * size * size)

    operator fun get(pos: Vec3i): V? = blocks[pos.x * size * size + pos.y * size + pos.z] as V?
    operator fun get(x: Int, y: Int, z: Int): V? = blocks[x * size * size + y * size + z] as V?
    operator fun set(pos: Vec3i, voxel: V?) { blocks[pos.x * size * size + pos.y * size + pos.z] = voxel }

    inline val absolutePosition get() = position * size

    fun destroy() {
        willBeRendered = false
        Renderer.runOnThread {
            mesh?.delete()
        }
    }

    companion object {
        private const val SOUTH = 0
        private const val NORTH = 1
        private const val EAST = 2
        private const val WEST = 3
        private const val TOP = 4
        private const val BOTTOM = 5
    }

    var willBeRendered = false
    inline val allNeighboringChunksAreLoaded get() = chunkMap[position.copy(x = position.x + 1)] != null &&
        chunkMap[position.copy(x = position.x - 1)] != null &&
        chunkMap[position.copy(y = position.y + 1)] != null &&
        chunkMap[position.copy(y = position.y - 1)] != null &&
        chunkMap[position.copy(z = position.z + 1)] != null &&
        chunkMap[position.copy(z = position.z - 1)] != null

    var mesh: Mesh? = null
        private set

    /*var isFull = false
        private set*/

    fun generateMeshAsync() = thread (isDaemon = true) {

        class VoxelFace(val voxel: Voxel) {
            var transparent = false
            var side = 0
            fun equals(face: VoxelFace?) = face!!.transparent == transparent && face.voxel.id == voxel.id
        }

        //println("chunk received")

        fun getVoxelFace(x: Int, y: Int, z: Int, side: Int): VoxelFace? {
            val chunk = when {
                x > size - 1 -> chunkMap[position.copy(x = position.x + 1)]!!
                y > size - 1 -> chunkMap[position.copy(y = position.y + 1)]!!
                z > size - 1 -> chunkMap[position.copy(z = position.z + 1)]!!
                x < 0 -> chunkMap[position.copy(x = position.x - 1)]!!
                y < 0 -> chunkMap[position.copy(y = position.y - 1)]!!
                z < 0 -> chunkMap[position.copy(z = position.z - 1)]!!
                else -> return this[x, y, z]?.let {
                    VoxelFace(it).apply { this.side = side }
                }
            }

            val smallX = if (x % size < 0) size + x % size else x % size
            val smallY = if (y % size < 0) size + y % size else y % size
            val smallZ = if (z % size < 0) size + z % size else z % size

            return chunk[smallX, smallY, smallZ]?.let {
                VoxelFace(it).apply { this.side = side }
            }
        }

        val vertices = ArrayList<Float>()
        val indices = ArrayList<Int>()
        val uv = ArrayList<Float>()
        val normals = ArrayList<Float>()

        var i: Int
        var j: Int
        var k: Int
        var l: Int
        var w: Int
        var h: Int
        var u: Int
        var v: Int
        var n: Int
        var side = 0
        val x = intArrayOf(0, 0, 0)
        val q = intArrayOf(0, 0, 0)
        val du = intArrayOf(0, 0, 0)
        val dv = intArrayOf(0, 0, 0)

        val mask = arrayOfNulls<VoxelFace>(size * size)

        var voxelFace: VoxelFace?
        var voxelFace1: VoxelFace?

        /**
         * We start with the lesser-spotted boolean for-loop (also known as the old flippy floppy).
         *
         * The variable backFace will be TRUE on the first iteration and FALSE on the second - this allows
         * us to track which direction the indices should run during creation of the quad.
         *
         * This loop runs twice, and the inner loop 3 times - totally 6 iterations - one for each
         * voxel face.
         */
        var backFace = true
        var b = false
        while (b != backFace) {
            for (d in 0..2) {
                u = (d + 1) % 3
                v = (d + 2) % 3
                x[0] = 0
                x[1] = 0
                x[2] = 0
                q[0] = 0
                q[1] = 0
                q[2] = 0
                q[d] = 1
                when (d) {
                    0 -> side = if (backFace) WEST else EAST
                    1 -> side = if (backFace) BOTTOM else TOP
                    2 -> side = if (backFace) SOUTH else NORTH
                }
                x[d] = -1
                while (x[d] < size) {
                    n = 0
                    x[v] = 0
                    while (x[v] < size) {
                        x[u] = 0
                        while (x[u] < size) {
                            voxelFace = getVoxelFace(x[0], x[1], x[2], side)
                            voxelFace1 = getVoxelFace(x[0] + q[0], x[1] + q[1], x[2] + q[2], side)
                            mask[n++] = if (voxelFace != null && voxelFace1 != null && voxelFace.equals(voxelFace1)) null else if (backFace) voxelFace1 else voxelFace
                            x[u]++
                        }
                        x[v]++
                    }
                    x[d]++

                    n = 0
                    j = 0
                    while (j < size) {
                        i = 0
                        while (i < size) {
                            if (mask[n] != null) {
                                w = 1
                                while (i + w < size && mask[n + w] != null && mask[n + w]!!.equals(mask[n])) {
                                    w++
                                }

                                var done = false
                                h = 1
                                while (j + h < size) {
                                    k = 0
                                    while (k < w) {
                                        if (mask[n + k + h * size] == null || !mask[n + k + h * size]!!.equals(mask[n])) {
                                            done = true
                                            break
                                        }
                                        k++
                                    }
                                    if (done) {
                                        break
                                    }
                                    h++
                                }

                                /*
                                 * Here we check the "transparent" attribute in the VoxelFace class to ensure that we don't mesh
                                 * any culled faces.
                                 */
                                if (!mask[n]!!.transparent) {
                                    x[u] = i
                                    x[v] = j
                                    du[0] = 0
                                    du[1] = 0
                                    du[2] = 0
                                    du[u] = w
                                    dv[0] = 0
                                    dv[1] = 0
                                    dv[2] = 0
                                    dv[v] = h


                                    /// ADD QUAD TO LIST

                                    val minIndex = uv.size / 2

                                    fun addPoint(x: Int, y: Int, z: Int, nx: Int, ny: Int, nz: Int, voxel: Voxel) {
                                        vertices.add(x.toFloat())
                                        vertices.add(y.toFloat())
                                        vertices.add(z.toFloat())

                                        val (uvX, uvY) = voxel.getUV()
                                        uv.add(uvX)
                                        uv.add(uvY)

                                        normals.add(nx.toFloat())
                                        normals.add(ny.toFloat())
                                        normals.add(nz.toFloat())
                                    }

                                    var nx = du[1] * dv[2] - du[2] * dv[1]
                                    var ny = du[2] * dv[0] - du[0] * dv[2]
                                    var nz = du[0] * dv[1] - du[1] * dv[0]
                                    val length = sqrt(nx * nx + ny * ny + nz * nz.toDouble()).toFloat()

                                    if (backFace) {
                                        indices.add(2 + minIndex)
                                        indices.add(0 + minIndex)
                                        indices.add(1 + minIndex)
                                        indices.add(1 + minIndex)
                                        indices.add(3 + minIndex)
                                        indices.add(2 + minIndex)

                                        nx = -(nx / length).toInt()
                                        ny = -(ny / length).toInt()
                                        nz = -(nz / length).toInt()
                                    } else {
                                        indices.add(2 + minIndex)
                                        indices.add(3 + minIndex)
                                        indices.add(1 + minIndex)
                                        indices.add(1 + minIndex)
                                        indices.add(0 + minIndex)
                                        indices.add(2 + minIndex)

                                        nx = (nx / length).toInt()
                                        ny = (ny / length).toInt()
                                        nz = (nz / length).toInt()
                                    }

                                    val voxel = mask[n]!!.voxel

                                    addPoint(x[0], x[1], x[2], nx, ny, nz, voxel)
                                    addPoint(x[0] + dv[0], x[1] + dv[1], x[2] + dv[2], nx, ny, nz, voxel)
                                    addPoint(x[0] + du[0], x[1] + du[1], x[2] + du[2], nx, ny, nz, voxel)
                                    addPoint(x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2] + du[2] + dv[2], nx, ny, nz, voxel)
                                }

                                /*
                                 * We zero out the mask
                                 */
                                l = 0
                                while (l < h) {
                                    k = 0
                                    while (k < w) {
                                        mask[n + k + l * size] = null
                                        ++k
                                    }
                                    ++l
                                }

                                /*
                                 * And then finally increment the counters and continue
                                 */
                                i += w
                                n += w
                            } else {
                                i++
                                n++
                            }
                        }
                        j++
                    }
                }
            }
            backFace = backFace && b
            b = !b
        }

        val tmpVertices = vertices.toFloatArray()
        val tmpIndices = indices.toIntArray()
        val tmpUv = uv.toFloatArray()
        val tmpNormals = normals.toFloatArray()

        Renderer.runOnThread {
            val oldMesh = mesh
            mesh = Mesh(tmpIndices, listOf(Mesh.VBO(tmpVertices, 3), Mesh.VBO(tmpUv, 2), Mesh.VBO(tmpNormals, 3)))
            willBeRendered = true
            /*willBeRendered = chunkMap[position.copy(x = position.x + 1)]?.isFull != true ||
                chunkMap[position.copy(x = position.x - 1)]?.isFull != true ||
                chunkMap[position.copy(y = position.y + 1)]?.isFull != true ||
                chunkMap[position.copy(y = position.y - 1)]?.isFull != true ||
                chunkMap[position.copy(z = position.z + 1)]?.isFull != true ||
                chunkMap[position.copy(z = position.z - 1)]?.isFull != true*/
            oldMesh?.delete()
            //println("mesh generated")
        }

        vertices.clear()
        indices.clear()
        uv.clear()
        normals.clear()
    }
}