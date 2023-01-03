package dev.luna.jaroptimizer

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class JarOptimizerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("jarOptimizer", JarOptimizerExtension::class.java)
    }
}