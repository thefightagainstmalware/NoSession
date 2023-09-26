@file:Suppress("UnstableApiUsage")

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
        val includeDir = javaHome.map { it.resolve("include") }
        toolChain.set(toolChains.getByName("clang"))
        compilerArgs.add("-I")
        compilerArgs.add(includeDir.get().absolutePath)
        compilerArgs.add("-I")
        compilerArgs.add(includeDir.get().resolve("win32").absolutePath)
        compilerArgs.add("-I")
        compilerArgs.add(project.projectDir.resolve("chromium/").absolutePath)
        compilerArgs.add("-I")
        compilerArgs.add(project.projectDir.resolve("chromium/third_party/abseil-cpp/").absolutePath)
        compilerArgs.add("-I")
        compilerArgs.add(project.projectDir.resolve("chromium/third_party/googletest/src/googletest/include/").absolutePath)
        compilerArgs.add("-std=c++20")
        compilerArgs.add("-fsanitize=address,undefined") // i do not trust my c++ skills
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
