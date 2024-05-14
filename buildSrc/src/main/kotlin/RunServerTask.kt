import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.work.Incremental
import org.gradle.work.InputChanges

abstract class RunServerTask : DefaultTask() {

    @get:Incremental
    @get:InputDirectory
    abstract val distributionDir: DirectoryProperty

    @get:Internal
    abstract val runDir: DirectoryProperty

    @get:OutputFile
    val serverPidFile: Provider<RegularFile> = runDir.file("server.pid")

    @get:Internal
    abstract val externalProcessBuildService: Property<ExternalProcessBuildService>

    @get:Internal
    abstract val javaLauncher: Property<JavaLauncher>

    @TaskAction
    fun runServer(inputChanges: InputChanges) {
        val runDirectory = runDir.get().asFile.apply { mkdir() }

        if (!inputChanges.isIncremental ||
            inputChanges.getFileChanges(distributionDir).toList().isNotEmpty()
        ) {
            if (externalProcessBuildService.get().isServerRunning()) {
                logger.info("Restarting server")
                externalProcessBuildService.get().stopServer(serverPidFile.get().asFile)
            }
        }

        externalProcessBuildService.get().startServer(
            distributionDir.get().asFile,
            runDirectory,
            serverPidFile.get().asFile,
            javaLauncher.get().metadata.installationPath.asFile,
        )
    }
}