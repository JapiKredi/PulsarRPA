package ai.platon.pulsar.common

import ai.platon.pulsar.common.ResourceLoader.getURLOrNull
import java.nio.file.*

class ResourceWalker(
    val baseDir: String = ""
): AutoCloseable {
    companion object {
        val SPRING_PACKED_RESOURCE_BASE_DIR = "BOOT-INF/classes/"
        val SPRING_PACKED_RESOURCE_WALKER = ResourceWalker(SPRING_PACKED_RESOURCE_BASE_DIR)
    }

    private val logger = getLogger(this)

    // When the jar is packed using spring-boot, the resources are put into directory BOOT-INF/classes
    val baseURI = getURLOrNull(baseDir)?.toURI()

    private var fileSystem: FileSystem? = null

    fun getPath(resourceName: String): Path? {
        val resource0 = "$baseDir$resourceName"
        val uri = getURLOrNull(resource0)?.toURI() ?: return null

        try {
            return if (uri.scheme == "jar") {
                val env: MutableMap<String, String> = HashMap()
                fileSystem = FileSystems.newFileSystem(uri, env)
                fileSystem?.getPath(resource0)
            } else {
                Paths.get(uri)
            }
        } catch (t: Throwable) {
            logger.warn("Unexpected failure get path | $resource0", t)
        } finally {
            // Do not close
            // fileSystem?.close()
        }

        return null
    }

    /**
     * Files.list(getPath(resourceBase)) is OK
     * */
    fun list(dir: String): Set<Path> {
        val paths = mutableSetOf<Path>()
        walk(dir, 1) {
            if (!it.toString().endsWith(dir)) {
                paths.add(it)
            }
        }

        return paths
    }

    /**
     * Files.walk(path, maxDepth) is OK
     * */
    fun walk(start: String, maxDepth: Int, visitor: (Path) -> Unit) {
        val path = getPath(start)
        
        val walk = Files.walk(path, maxDepth)
        val it = walk.iterator()
        while (it.hasNext()) {
            try {
                val p = it.next()
                visitor(p)
            } catch (e: Exception) {
                logger.warn("Failed to visit path | $path", e)
            }
        }
    }

    override fun close() {
        fileSystem?.close()
    }
}
