plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "me.luna"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.bcel:bcel:6.5.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    jar {
        manifest {
            attributes(
                "Main-Class" to "me.luna.jaroptimizer.MainKt"
            )
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        from(
            configurations.runtimeClasspath.get().map {
                if (it.isDirectory) it else zipTree(it)
            }
        )
    }
}