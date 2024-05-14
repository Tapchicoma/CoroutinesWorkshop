import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.internal.os.OperatingSystem
import java.io.File

abstract class ExternalProcessBuildService : BuildService<BuildServiceParameters.None> {
    private val logger = Logging.getLogger(ExternalProcessBuildService::class.java)

    init {
        // Shutdown running server on Gradle daemon stop
        Runtime.getRuntime().addShutdownHook(Thread {
            serverProcess?.destroyForcibly()
            serverProcess = null
        })
    }

    @Synchronized
    fun startServer(
        serverDistPath: File,
        serverRunDir: File,
        serverPidFile: File,
        jdkPath: File,
    ) {
        if (serverProcess == null) {
            logger.warn("Starting server")
            if (serverPidFile.exists()) killDetachedServerProcess(serverPidFile)

            val serverCommand = if (OperatingSystem.current().isWindows) {
                "bin/server.bat"
            } else {
                "bin/server"
            }
            val processBuilder = ProcessBuilder()
                .directory(serverDistPath)
                .redirectOutput(serverRunDir.resolve("stdout.txt"))
                .redirectError(serverRunDir.resolve("stderr.txt"))
                .command(serverCommand)
            processBuilder.environment()["JAVA_HOME"] = jdkPath.absolutePath
            logger.info("Configured JAVA_HOME ${jdkPath.absolutePath}")
            serverProcess = processBuilder.start()
            serverRunDir.resolve(serverPidFile).writeText(serverProcess?.pid().toString())
            logger.warn("Server started with pid ${serverProcess?.pid()}")
        } else {
            logger.warn("Server is already running with pid: ${serverProcess?.pid()}")
        }
    }

    @Synchronized
    fun isServerRunning(): Boolean {
        return serverProcess != null
    }

    private fun killDetachedServerProcess(serverPidFile: File) {
        val detachedPid = serverPidFile.readText().toLongOrNull()
        if (detachedPid != null) {
            logger.warn("Trying to stop already running but detached service with pid: $detachedPid")
            try {
                val processHandle = ProcessHandle.of(detachedPid)
                processHandle.get().destroy()
            } catch (_: Exception) {
                logger.warn(
                    """
                    Failed to stop already running server process with pid: $detachedPid.
                    Please kill it manually and remove ${serverPidFile.absolutePath}
                    """.trimIndent()
                )
            }
        }
    }

    @Synchronized
    fun stopServer(serverPidFile: File) {
        if (serverProcess != null) {
            serverProcess?.destroy()
            serverProcess = null
            serverPidFile.delete()

            logger.warn("Server stopped")
        } else {
            if (serverPidFile.exists()) {
                killDetachedServerProcess(serverPidFile)
            } else {
                logger.warn("Server is already stopped")
            }
        }
    }

    companion object {
        private var serverProcess: Process? = null

        fun Project.registerExternalProcessBuildService(): Provider<ExternalProcessBuildService> =
            gradle
                .sharedServices
                .registerIfAbsent("external-process", ExternalProcessBuildService::class.java)
    }
}
