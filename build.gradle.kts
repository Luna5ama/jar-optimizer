group = "dev.luna5ama"
version = "1.2.2"

plugins {
    `kotlin-dsl`
    `maven-publish`
    id("com.gradle.plugin-publish") version "1.3.1"
}

gradlePlugin {
    website.set("https://github.com/Luna5ama/jar-optimizer")
    vcsUrl.set("https://github.com/Luna5ama/jar-optimizer")
    plugins {
        create("jar-optimizer") {
            id = "dev.luna5ama.jar-optimizer"
            implementationClass = "dev.luna5ama.jaroptimizer.JarOptimizerPlugin"
            displayName = "Jar Optimizer"
            description = "Simple jar file optimizing tool"
            tags.addAll("java", "jar", "jvm", "build")
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
    library("it.unimi.dsi:fastutil:8.5.13")
    library("org.apache.bcel:bcel:6.8.1")
    library(kotlin("stdlib-jdk8"))
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
    withSourcesJar()
}

tasks {
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
