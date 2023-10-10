@file:Suppress("UnstableApiUsage")

import org.apache.tools.ant.taskdefs.condition.Os
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import java.io.ByteArrayOutputStream

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

fun pprc(project: Project, path: String): File {
    return project.projectDir.resolve("chromium/$path")
}

fun isParent(parent: File, child: File): Boolean {
    var childCopy = child;
    while (true) {
        childCopy = File(childCopy.parent);
        if (childCopy.parent == null) {
            break;
        } else if (childCopy == parent) {
            return true;
        }
    }
    return false;
}

project.afterEvaluate {
    tasks.withType(CppCompile::class) {
        enabled = Os.isFamily(Os.FAMILY_WINDOWS)
        toolChain.set(toolChains.getByName("clang"))
        val javaHome = rootProject.the<JavaToolchainService>()
                .compilerFor(rootProject.the<JavaPluginExtension>().toolchain)
                .map { it.metadata.installationPath.asFile }
        val javaHomeIncludeDir = javaHome.map { it.resolve("include") }

        val stdout = ByteArrayOutputStream()
        exec {
            commandLine("cmd", "/c", "echo | clang -v -E - 2>&1 | findstr lib\\clang")
            standardOutput = stdout
        }

        val includeDirs = arrayOf(
                pprc(project, "third_party/libc++/src/include"),
                pprc(project, "buildtools/third_party/libc++"),
                stdout.toString().trim(),
                javaHomeIncludeDir.get(),
                javaHomeIncludeDir.get().resolve("win32"),
                pprc(project, ""), // chromium dir
                pprc(project, "third_party/abseil-cpp/"),
                pprc(project, "third_party/googletest/src/googletest/include/"),
                pprc(project, "third_party/perfetto/"),
                pprc(project, "third_party/perfetto/include/"),
                pprc(project, "third_party/boringssl/src/include/"),
                pprc(project, "third_party/protobuf/src/"),
                pprc(project, "third_party/perfetto/build_config/"),
                "C:\\Program Files (x86)\\Windows Kits\\10\\Include\\10.0.22621.0\\winrt"
        )


        includeDirs.map {
            compilerArgs.add("-I");
            if (it is File)
                compilerArgs.add(it.absolutePath);
            else if (it is String)
                compilerArgs.add(it);
        }
        val defines = arrayOf(
                "-D__WRL_ENABLE_FUNCTION_STATICS__",
                "-D_HAS_NODISCARD",
                "-D_CRT_NONSTDC_NO_WARNINGS",
                "-D_WINSOCK_DEPRECATED_NO_WARNINGS",
                "-D_LIBCPP_ENABLE_SAFE_MODE=1",
                "-DCOMPONENT_BUILD",
                "-D__STD_C",
                "-D_CRT_RAND_S",
                "-D_CRT_SECURE_NO_DEPRECATE",
                "-D_SCL_SECURE_NO_DEPRECATE",
                "-D_ATL_NO_OPENGL",
                "-D_WINDOWS",
                "-DCERT_CHAIN_PARA_HAS_EXTRA_FIELDS",
                "-DPSAPI_VERSION=2",
                "-DWIN32",
                "-D_SECURE_ATL",
                "-DWIN32_LEAN_AND_MEAN",
                "-DNOMINMAX",
                "-D_UNICODE",
                "-DUNICODE",
                "-DNTDDI_VERSION=NTDDI_WIN10_NI",
                "-D_WIN32_WINNT=0x0A00",
                "-DWINVER=0x0A00",
        )
        defines.map {
            compilerArgs.add(it)
        }
        val excludedSources = arrayOf(
                "base/allocator/partition_allocator/dangling_raw_ptr_checks.cc"
        ).map {
            project.projectDir.resolve("chromium").resolve(it)
        }
        val excludedDirs = arrayOf(
                "base/allocator/partition_allocator/partition_alloc_base/posix",
                "base/allocator/partition_allocator/partition_alloc_base/fushsia",
                "base/allocator/partition_allocator/partition_alloc_base/apple",
                "base/allocator/partition_allocator/partition_alloc_base/ios",
                "base/allocator/partition_allocator/partition_alloc_base/mac",
        ).map {
            project.projectDir.resolve("chromium").resolve(it)
        }
        val excludedEndings = arrayOf(
                "fuzzer.cc",
                "test.cc",
                "checks.cc",
                "testing.cc",
                "atomicops_internals_portable.cc"
        )
        val excludeAnywhere = arrayOf(
                "apple",
                "ios",
                "android",
                "fuchsia",
                "posix",
                "arm",
                "arm64",
                "riscv64",
                "x86",
                "glibc",
                "elf", // executable and linker format, linux exe format
                "trace",
                "linux",
                "fsevents", // mac fs notifier
                "inotify", // linux fs notifier
                "kqueue", // i think this is the fuschia fs notifier?
                "i18n",
                "mac"
        )
        val additionalSources = File("chromium").walk().filter {
            it.extension == "cc" && it !in excludedSources && !excludedEndings.map {ending -> it.path.endsWith(ending)}.contains(true)
                    && !excludedDirs.map {excludedDir -> isParent(excludedDir, it)}.contains(true)
                    && !excludeAnywhere.map {excludeText -> excludeText in it.relativeTo(File(".")).toString()}.contains(true)
        }
        source.from.addAll(additionalSources)
        compilerArgs.add("-std=c++20")
        compilerArgs.add("-isystem")
        compilerArgs.add("C:\\Program Files (x86)\\Windows Kits\\10\\Include\\10.0.22621.0\\winrt")
    }

    tasks.withType(LinkSharedLibrary::class) {
        toolChain.set(toolChains.getByName("clang"))
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
