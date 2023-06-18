plugins {
    `kotlin-dsl`
    java
}

repositories {
    mavenCentral()
    maven("https://maven.minecraftforge.net")
    maven("https://jitpack.io") {
        mavenContent {
            includeGroupByRegex("(com|io)\\.github\\..+")
        }
    }
    mavenLocal()
}

val kotlinVersion = "1.8.21"

dependencies {
    implementation("com.github.thefightagainstmalware:ForgeGradle:5a1fcb9")
    implementation("com.github.thefightagainstmalware:MixinGradle:92e66fe")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

dependencyLocking {
    lockAllConfigurations()
}