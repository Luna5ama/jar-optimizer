package dev.luna.jaroptimizer

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.jvm.tasks.Jar
import java.util.*
import javax.inject.Inject

@Suppress("unused")
abstract class JarOptimizerExtension {
    @get:Inject
    internal abstract val project: Project

    fun optimize(jarTask: String, vararg keeps: String) {
        project.tasks.create("optimize${jarTask.capitalize()}", OptimizeJarTask::class.java) {
            it.jarTask.set(project.tasks.named(jarTask, Jar::class.java))
            it.keeps.addAll(*keeps)
        }
    }

    fun optimize(jarTask: Provider<out Jar>, vararg keeps: String) {
        project.tasks.create("optimize${jarTask.get().name.capitalize()}", OptimizeJarTask::class.java) {
            it.jarTask.set(jarTask)
            it.keeps.addAll(*keeps)
        }
    }

    fun optimize(jarTask: Jar, vararg keeps: String) {
        project.tasks.create("optimize${jarTask.name.capitalize()}", OptimizeJarTask::class.java) {
            it.jarTask.set(jarTask)
            it.keeps.addAll(*keeps)
        }
    }

    private fun String.capitalize(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}