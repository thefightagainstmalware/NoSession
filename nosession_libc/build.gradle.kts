import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    `cpp-library`
    `java-base`

}
val lib: Configuration by configurations.creating

library {
    linkage.set(listOf(Linkage.SHARED))
    targetMachines.set(listOf(machines.linux.x86_64))

}

project.afterEvaluate {
    tasks.withType(CppCompile::class) {
        val javaHome = rootProject.the<JavaToolchainService>()
            .compilerFor(rootProject.the<JavaPluginExtension>().toolchain)
            .map { it.metadata.installationPath.asFile }
        val includeDir = javaHome.map { it.resolve("include") }
        val os = targetPlatform.get().operatingSystem
        compilerArgs.add("-I")
        compilerArgs.add(includeDir.map { it.absolutePath })
        compilerArgs.add("-I")
        compilerArgs.add(includeDir.map {
            it.resolve(
                when {
                    os.isMacOsX -> "darwin"
                    os.isLinux -> "linux"
                    os.isWindows -> "win32"
                    os.isFreeBSD -> "freebsd"
                    else -> TODO("Unsupported operating system")
                }
            ).absolutePath
        })
        if (os.isLinux) {
            compilerArgs.add("-D_FILE_OFFSET_BITS=64")
        }
        if (os.isMacOsX) {
            compilerArgs.add("-mmacosx-version-min=10.4")
        }
    }

    tasks.withType(AbstractLinkTask::class) {
        if (targetPlatform.get().operatingSystem.isMacOsX)
            linkerArgs.add("-mmacosx-version-min=10.4")
    }
    val linkRelease: AbstractLinkTask by tasks
    val archive by tasks.creating(Zip::class) {
        archivesName.set("archive.jar")
        destinationDirectory.set(layout.buildDirectory.dir("libs"))
        from(linkRelease.linkedFile) {
            val targetPlatform = linkRelease.targetPlatform.get()
            into("native/" + when(val arch = targetPlatform.architecture.name) {
                "x86-64" -> "amd64"
                else -> arch
            } + "/" +targetPlatform.operatingSystem.name)
        }
    }
    artifacts.add(lib.name, archive)
}
