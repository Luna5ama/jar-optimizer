plugins {
    kotlin("jvm") version "1.7.10"
    application
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins {
        create("jarOptimizer") {
            id = "me.luna.jaroptimizer"
            implementationClass = "me.luna.jaroptimizer.JarOptimizerPlugin"
        }
    }
}
group = "me.luna"
version = "1.1"

repositories {
    mavenCentral()
}

val library: Configuration by configurations.creating
val pluginSourceSet = sourceSets.create("plugin").apply {
    java.srcDir("src/plugin/kotlin")
    resources.srcDir("src/plugin/resources")
    compileClasspath += sourceSets.main.get().compileClasspath + sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().runtimeClasspath + sourceSets.main.get().output
}

dependencies {
    library("org.apache.bcel:bcel:6.5.0")
    library(kotlin("stdlib-jdk8"))
    implementation(library)
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    jar {
        from(pluginSourceSet.output)
    }

    val standaloneJar by register<Jar>("standaloneJar") {
        group = "build"

        manifest {
            attributes(
                "Main-Class" to "me.luna.jaroptimizer.JarOptimizer",
            )
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        archiveBaseName.set("JarOptimizer")
        archiveClassifier.set("Standalone")

        from(sourceSets.main.get().output)
        from(
            library.map {
                if (it.isDirectory) it else zipTree(it)
            }
        )
    }

    artifacts {
        archives(standaloneJar)
    }
}

publishing {
    repositories {
        maven("https://maven.pkg.github.com/Luna5ama/JarOptimizer") {
            val githubProperty = runCatching {
                org.jetbrains.kotlin.konan.properties.loadProperties("${projectDir.absolutePath}/github.properties")
            }.getOrNull()

            credentials {
                username = githubProperty?.getProperty("username") ?: System.getenv("USERNAME")
                password = githubProperty?.getProperty("token") ?: System.getenv("TOKEN")
            }
        }
    }
}