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
    implementation("com.github.thefightagainstmalware:ForgeGradle:2a95cb09a6885e391a7dda3c5c5655f7b6779c15")
    implementation("com.github.thefightagainstmalware:MixinGradle:92e66fe")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
}

dependencyLocking {
    lockAllConfigurations()
}
