package dev.luna.jaroptimizer

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import it.unimi.dsi.fastutil.objects.ObjectSets
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import java.io.File

@Suppress("LeakingThis")
abstract class OptimizeJarTask : Jar() {
    @get:Input
    abstract val jarTask : Property<Jar>

    @get:Input
    abstract val keeps: SetProperty<String>

    @get:InputFile
    internal abstract val jarFile: RegularFileProperty

    init {
        jarFile.set(jarTask.flatMap { it.archiveFile })

        destinationDirectory.set(jarTask.flatMap { it.destinationDirectory })
        archiveBaseName.set(jarTask.flatMap { it.archiveBaseName })
        archiveAppendix.set(jarTask.flatMap { it.archiveAppendix })
        archiveVersion.set(jarTask.flatMap { it.archiveVersion })
        archiveClassifier.set(jarTask.flatMap { jar -> jar.archiveClassifier.map { "$it-optimized" } })
        archiveExtension.set("jar")
    }

    override fun createCopyAction(): CopyAction {
        return RemapJarAction(jarFile.get().asFile, archiveFile.get().asFile, keeps.get())
    }

    class RemapJarAction(private val inputFile: File, private val outputFile: File, private val keeps: Set<String>) : CopyAction {
        override fun execute(stream: CopyActionProcessingStream): WorkResult {
            JarOptimizer().optimize(inputFile, outputFile, ObjectOpenHashSet(keeps))
            return WorkResults.didWork(true)
        }
    }
}