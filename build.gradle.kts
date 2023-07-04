import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dev.luna5ama"
version = "1.2-SNAPSHOT"

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    id("dev.fastmc.maven-repo").version("1.0.0")
}

gradlePlugin {
    plugins {
        create("jar-optimizer") {
            id = "dev.luna5ama.jar-optimizer"
            displayName = "Jar Optimizer"
            description = "Simple jar file optimizing tool"
            implementationClass = "dev.luna5ama.jaroptimizer.JarOptimizerPlugin"
        }
    }
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
    withSourcesJar()
}

tasks {
    withType(KotlinCompile::class.java) {
        kotlinOptions.jvmTarget = "1.8"
    }

    jar {
        from(pluginSourceSet.output)
    }

    named<Jar>("sourcesJar") {
        from(pluginSourceSet.allSource)
    }

    val standaloneJar by register<Jar>("standaloneJar") {
        group = "build"

        manifest {
            attributes(
                "Main-Class" to "dev.luna5ama.jaroptimizer.JarOptimizer",
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