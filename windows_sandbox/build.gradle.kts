import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName

plugins {
    `cpp-library`
    `java-base`

}
val lib: Configuration by configurations.creating

library {
    if (Os.isFamily(Os.FAMILY_WINDOWS)) {
        linkage.set(listOf(Linkage.SHARED))
        targetMachines.set(listOf(machines.windows.x86_64))
    }
}

project.afterEvaluate {
    tasks.withType(CppCompile::class) {
        enabled = Os.isFamily(Os.FAMILY_WINDOWS)

        val javaHome = rootProject.the<JavaToolchainService>()
                .compilerFor(rootProject.the<JavaPluginExtension>().toolchain)
                .map { it.metadata.installationPath.asFile }
        val javaHomeIncludeDir = javaHome.map { it.resolve("include") }
        if (this.toolChain.get() is GccCompatibleToolChain) {
            compilerArgs.add("-I")
            compilerArgs.add(javaHomeIncludeDir.get().absolutePath)
            compilerArgs.add("-I")
            compilerArgs.add(javaHomeIncludeDir.get().resolve("win32").absolutePath)
            compilerArgs.add("-Wall")
            compilerArgs.add("-Wextra")
            compilerArgs.add("-Werror")
            compilerArgs.add("-std=c++17")
        } else {
            compilerArgs.add("/I" + javaHomeIncludeDir.get().absolutePath)
            compilerArgs.add("/I" + javaHomeIncludeDir.get().resolve("win32").absolutePath)
            compilerArgs.add("/Wall")
            compilerArgs.add("/WX")
            compilerArgs.add("/wd4668") // 4668: macro is not defined
            compilerArgs.add("/wd4820") // 4820: 'bytes' bytes padding added after construct 'member_name'
            compilerArgs.add("/wd4710") // 4710: function not inlined
            compilerArgs.add("/wd4711") // 4711: function is inlined. there is no winning with with msvc
            compilerArgs.add("/std:c++17")
            compilerArgs.add("/EHa")
        }
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
            } + "/" + targetPlatform.operatingSystem.name)
        }
    }
    artifacts.add(lib.name, archive)
}
