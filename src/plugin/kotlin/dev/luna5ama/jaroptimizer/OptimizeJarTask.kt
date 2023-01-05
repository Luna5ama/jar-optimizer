package dev.luna5ama.jaroptimizer

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.jvm.tasks.Jar
import java.io.File

abstract class OptimizeJarTask : Jar() {
    @get:Input
    abstract val keeps: SetProperty<String>

    @get:InputFile
    abstract val jarFile: RegularFileProperty

    fun setup(jarTask: Provider<out Jar>) {
        jarFile.set(jarTask.flatMap { it.archiveFile })

        destinationDirectory.set(jarTask.flatMap { it.destinationDirectory })
        archiveBaseName.set(jarTask.flatMap { it.archiveBaseName })
        archiveAppendix.set(jarTask.flatMap { it.archiveAppendix })
        archiveVersion.set(jarTask.flatMap { it.archiveVersion })
        archiveClassifier.set(jarTask.flatMap { jar -> jar.archiveClassifier.map { if (it.isEmpty()) "optimized" else "$it-optimized" } })
        archiveExtension.set("jar")
    }

    fun setup(jarTask: Jar) {
        jarFile.set(jarTask.archiveFile)

        destinationDirectory.set(jarTask.destinationDirectory)
        archiveBaseName.set(jarTask.archiveBaseName)
        archiveAppendix.set(jarTask.archiveAppendix)
        archiveVersion.set(jarTask.archiveVersion)
        archiveClassifier.set(jarTask.archiveClassifier.map { "$it-optimized" })
        archiveExtension.set("jar")
    }

    override fun createCopyAction(): CopyAction {
        return RemapJarAction(jarFile.get().asFile, archiveFile.get().asFile, keeps.get())
    }

    class RemapJarAction(private val inputFile: File, private val outputFile: File, private val keeps: Set<String>) :
        CopyAction {
        override fun execute(stream: CopyActionProcessingStream): WorkResult {
            JarOptimizer().optimize(inputFile, outputFile, ObjectOpenHashSet(keeps))
            return WorkResults.didWork(true)
        }
    }
}