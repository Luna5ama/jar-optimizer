package dev.luna5ama.jaroptimizer

import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import java.util.*
import javax.inject.Inject

@Suppress("unused")
abstract class JarOptimizerExtension {
    @get:Inject
    internal abstract val project: Project

    fun register(jarTaskName: String, keeps: Provider<out Iterable<String>>): TaskProvider<OptimizeJarTask> {
        return project.tasks.register("optimize${jarTaskName.capitalize()}", OptimizeJarTask::class.java) {
            this.setup(project.provider { project.tasks.named(jarTaskName, Jar::class.java) }.flatten())
            this.keeps.addAll(keeps)
        }
    }

    fun register(jarTaskName: String, vararg keeps: String): TaskProvider<OptimizeJarTask> {
        return project.tasks.register("optimize${jarTaskName.capitalize()}", OptimizeJarTask::class.java) {
            this.setup(project.provider { project.tasks.named(jarTaskName, Jar::class.java) }.flatten())
            this.keeps.addAll(*keeps)
        }
    }

    fun register(jarTask: NamedDomainObjectProvider<out Jar>, vararg keeps: String): TaskProvider<OptimizeJarTask> {
        return project.tasks.register("optimize${jarTask.name.capitalize()}", OptimizeJarTask::class.java) {
            this.setup(jarTask)
            this.keeps.addAll(*keeps)
        }
    }

    fun register(jarTask: NamedDomainObjectProvider<out Jar>, keeps: Provider<out Iterable<String>>): TaskProvider<OptimizeJarTask> {
        return project.tasks.register("optimize${jarTask.name.capitalize()}", OptimizeJarTask::class.java) {
            this.setup(jarTask)
            this.keeps.addAll(keeps)
        }
    }

    fun register(jarTask: Jar, vararg keeps: String): TaskProvider<OptimizeJarTask> {
        return project.tasks.register("optimize${jarTask.name.capitalize()}", OptimizeJarTask::class.java) {
            this.setup(jarTask)
            this.keeps.addAll(*keeps)
        }
    }

    fun register(jarTask: Jar, keeps: Provider<out Iterable<String>>): TaskProvider<OptimizeJarTask> {
        return project.tasks.register("optimize${jarTask.name.capitalize()}", OptimizeJarTask::class.java) {
            this.setup(jarTask)
            this.keeps.addAll(keeps)
        }
    }

    private fun <T> Provider<out Provider<T>>.flatten(): Provider<T> {
        return flatMap { it }
    }

    private fun String.capitalize(): String =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}