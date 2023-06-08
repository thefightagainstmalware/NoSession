plugins {
    java
    id("net.minecraftforge.gradle.forge")
    id("org.spongepowered.mixin")
    kotlin("jvm")
}

version = "1.2.0"
group = "gq.malwarefight.nosession"

val kotlinVersion = "1.8.21"
val shade: Configuration by configurations.creating
configurations {
    val compile by creating {
        extendsFrom(shade)
    }
    implementation.get().extendsFrom(compile)
}

minecraft {
    version = "1.8.9-11.15.1.2318-1.8.9"
    runDir = "run"
    mappings = "stable_22" //mappings for 1.8.9
    makeObfSourceJar = false //disable creation of sources jar
}


repositories {
    maven("https://repo.spongepowered.org/maven/")
    maven("https://maven.minecraftforge.net/")
    mavenCentral()
}

dependencies {
    compileOnly("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9:universal")
    shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        exclude(module = "launchwrapper")
        exclude(module = "guava")
        exclude(module = "gson")
        exclude(module = "commons-io")
        exclude(module = "log4j-core")
    }
    shade(project(":nosession_libc", "lib"))
    compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", project.minecraft.version)
    filesMatching(listOf("mcmod.info")) {
        expand(inputs.properties)
    }
}

sourceSets.main {
    this.ext["refMap"] = "mixins.nosession.refmap.json"
}

tasks.withType(Jar::class) {
    from(shade.map { if(it.isDirectory) it else zipTree(it) })
    manifest {
        attributes(
            "ForceLoadAsMod" to true,
            "ModSide" to "CLIENT",
            "FMLCorePluginContainsFMLMod" to true,
            "MixinConfigs" to "mixins.nosession.json",
            "FMLCorePlugin" to "gq.malwarefight.nosession.NoSessionLoadingPlugin"
        )
    }
    exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA", "dummyThing")
    archiveBaseName.set("NoSession")
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

