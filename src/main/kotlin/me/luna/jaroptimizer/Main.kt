package me.luna.jaroptimizer

import org.apache.bcel.Const
import org.apache.bcel.classfile.*
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.streams.toList

fun main(args: Array<String>) {
    var input: String? = null
    var output: String? = null
    val keeps = mutableListOf<String>()

    var i = 0
    while (i < args.size) {
        when (args[i]) {
            "-i", "-in", "-input" -> input = args[++i]
            "-o", "-out", "-output" -> output = args[++i]
            "-k", "-keep" -> keeps += args[++i].replace('.', '/')
        }
        i++
    }

    if (input == null) {
        println("Input file not specified")
        return
    }

    if (output == null) {
        println("Output file not specified")
        return
    }

    if (keeps.isEmpty()) {
        println("No classes to keep specified")
        return
    }

    val cachedFiles = unpack(File(input))

    val classes = cachedFiles
        .filter { it.value != null }
        .filter { it.key.endsWith(".class") }
        .map { ClassFile(it.key, it.key.substring(0, it.key.length - 6), it.value!!) }

    val dependencyMap = readAllClassDependencies(classes)

    val set = processDependencies(classes, dependencyMap, keeps)
    classes.forEach { classFile ->
        if (!set.contains(classFile.classPath)) {
            cachedFiles.remove(classFile.path)
        }
    }

    repack(cachedFiles, File(output))
}

private fun unpack(input: File): MutableMap<String, ByteArray?> {
    val files = mutableMapOf<String, ByteArray?>()

    ZipInputStream(input.inputStream()).use { stream ->
        while (true) {
            val entry = stream.nextEntry ?: break
            files[entry.name] = if (entry.isDirectory) null else stream.readBytes()
        }
    }

    return files
}

private fun repack(files: MutableMap<String, ByteArray?>, output: File) {
    if (output.exists()) {
        output.delete()
    }

    ZipOutputStream(FileOutputStream(output)).use { stream ->
        files.remove("META-INF/MANIFEST.MF")?.let {
            stream.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
            stream.write(it)
            stream.closeEntry()
        }
        files.forEach { (path, bytes) ->
            stream.putNextEntry(ZipEntry(path))
            if (bytes != null) {
                stream.write(bytes)
            }
            stream.closeEntry()
        }
    }
}


private fun readAllClassDependencies(
    classes: List<ClassFile>
): Map<String, MutableSet<String>> {
    return classes.parallelStream()
        .map { classFile ->
            classFile.bytes.inputStream().use {
                classFile.classPath to readClassDependencies(it)
            }
        }
        .toList()
        .toMap()
}

private fun readClassDependencies(stream: InputStream): MutableSet<String> {
    val clazz = ClassParser(stream, "").parse()
    val constPool = clazz.constantPool
    val set = mutableSetOf<String>()

    constPool.accept(DescendingVisitor(clazz, object : EmptyVisitor() {
        override fun visitConstantClass(cC: ConstantClass) {
            set.add((cC.getConstantValue(constPool) as String))
        }

        override fun visitConstantNameAndType(cNaT: ConstantNameAndType) {
            processSignature(cNaT.getSignature(constPool))
        }

        override fun visitConstantMethodType(cMt: ConstantMethodType) {
            processSignature(constPool.constantToString(cMt.descriptorIndex, Const.CONSTANT_Utf8))
        }

        private fun processSignature(desc: String) {
            var i = 0
            while (i < desc.length) {
                if (desc[i] == 'L') {
                    val indexOf = desc.indexOf(';', i)
                    set.add(desc.substring(++i, indexOf))
                    i = indexOf
                }
                i++
            }
        }
    }))

    return set
}

private fun processDependencies(
    classes: List<ClassFile>,
    map: Map<String, Set<String>>,
    keeps: List<String>
): Set<String> {
    val set = mutableSetOf<String>()
    val queue = ArrayDeque<String>()

    if (keeps.isNotEmpty()) {
        classes.forEach { classFile ->
            if (keeps.any { classFile.classPath.startsWith(it) }) {
                set.add(classFile.classPath)
                queue.add(classFile.classPath)
            }
        }
    }

    while (queue.isNotEmpty()) {
        val path = queue.removeFirst()
        map[path]?.forEach {
            if (set.add(it)) {
                queue.add(it)
            }
        }
    }

    return set
}

private data class ClassFile(val path: String, val classPath: String, val bytes: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ClassFile) return false

        if (path != other.path) return false
        if (classPath != other.classPath) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + classPath.hashCode()
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}