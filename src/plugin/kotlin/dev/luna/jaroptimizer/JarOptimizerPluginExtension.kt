package dev.luna.jaroptimizer

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

abstract class JarOptimizerPluginExtension {
    private val entries0 = mutableMapOf<Task, Entry>()

    val entries: List<Entry> get() = entries0.values.toList()

    fun add(jarTask: TaskProvider<out Task>, vararg keeps: String) {
        add(jarTask.get(), *keeps)
    }

    fun add(jarTask: Task, vararg keeps: String) {
        entries0[jarTask] = (Entry(jarTask, keeps.toList()))
    }

    data class Entry(val task: Task, val keeps: List<String>)
}