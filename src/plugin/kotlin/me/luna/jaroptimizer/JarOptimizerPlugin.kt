package me.luna.jaroptimizer

import org.gradle.api.Plugin
import org.gradle.api.Project

@Suppress("unused")
class JarOptimizerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.extensions.create("jarOptimizer", JarOptimizerPluginExtension::class.java)
        target.tasks.register("optimizeJars", OptimizeJarTask::class.java)
    }
}