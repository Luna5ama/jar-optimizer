package me.luna.jaroptimizer

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class OptimizeJarTask : DefaultTask() {
    private val entries = project.extensions.getByType(JarOptimizerPluginExtension::class.java).entries

    init {
        group = "build"
        entries.forEach {
            dependsOn(it.task)
        }
    }

    @Suppress("unused")
    @get:InputFiles
    val inputJars: FileCollection
        get() = project.objects.fileCollection().apply {
            entries.forEach { entry ->
                from(getJarFiles(entry.task.outputs.files))
            }
        }

    private fun getJarFiles(files: FileCollection): List<File> {
        val list = mutableListOf<File>()
        files.forEach { file ->
            if (file.isDirectory) {
                file.listFiles()!!.filterTo(list) { it.name.endsWith(".jar") }
            } else if (file.name.endsWith(".jar")) {
                list.add(file)
            }
        }
        return list
    }

    @TaskAction
    fun optimize() {
        entries.asSequence().flatMap { entry ->
            getJarFiles(entry.task.outputs.files).map { it to entry.keeps }
        }.forEach { (jarFile, keeps) ->
            JarOptimizer().optimize(jarFile, File(jarFile.parentFile, "optimized/${jarFile.name}"), keeps)
        }
    }
}