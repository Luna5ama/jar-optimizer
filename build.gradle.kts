import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.utils.extendsFrom

group = "dev.luna"
version = "1.2-SNAPSHOT"

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("com.gradle.plugin-publish").version("1.0.0")
    id("dev.fastmc.maven-repo").version("1.0.0")
}

gradlePlugin {
    plugins {
        create("jarOptimizer") {
            id = "dev.luna.jaroptimizer"
            displayName = "Jar Optimizer"
            description = "Simple jar file optimizing tool"
            implementationClass = "dev.luna.jaroptimizer.JarOptimizerPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/Luna5ama/JarOptimizer"
    vcsUrl = "https://github.com/Luna5ama/JarOptimizer"
    tags = listOf("java", "jar", "optimization")
}

repositories {
    mavenCentral()
}

val library: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val pluginSourceSet = sourceSets.create("plugin").apply {
    java.srcDir("src/plugin/kotlin")
    resources.srcDir("src/plugin/resources")
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

dependencies {
    library("it.unimi.dsi:fastutil:8.5.11")
    library("org.apache.bcel:bcel:6.6.0")
    library(kotlin("stdlib-jdk8"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

tasks {
    withType(KotlinCompile::class.java) {
        kotlinOptions.jvmTarget = "1.8"
    }

    jar {
        from(pluginSourceSet.output)
    }

    val standaloneJar by register<Jar>("standaloneJar") {
        group = "build"

        manifest {
            attributes(
                "Main-Class" to "dev.luna.jaroptimizer.JarOptimizer",
            )
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        archiveClassifier.set("standalone")

        from(sourceSets.main.get().output)
        from(library.elements.map { set -> set.map { it.asFile }.map { if (it.isDirectory) it else zipTree(it) } })
    }

    artifacts {
        archives(standaloneJar)
    }
}