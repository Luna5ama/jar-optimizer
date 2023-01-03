package dev.luna.jaroptimizer

import org.apache.bcel.Const
import org.apache.bcel.classfile.*
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class JarOptimizer {
    fun optimize(input: File, output: File, keeps: List<String>) {
        require(input.exists()) { "Input file does not exist" }

        val cachedFiles = unpack(input)

        val classes = cachedFiles.asSequence()
            .filter { it.value != null }
            .filter { it.key.endsWith(".class") }
            .map { ClassFile(it.key, it.key.substring(0, it.key.length - 6), it.value!!) }
            .toList()

        val dependencyMap = readAllClassDependencies(classes)

        val set = processDependencies(classes, dependencyMap, keeps.map { it.replace('.', '/') })
        classes.forEach { classFile ->
            if (!set.contains(classFile.classPath)) {
                cachedFiles.remove(classFile.path)
            }
        }

        repack(cachedFiles, output)
    }

    private fun unpack(input: File): MutableMap<String, ByteArray?> {
        val files = mutableMapOf<String, ByteArray?>()

        ZipInputStream(input.inputStream().buffered(1024 * 1024)).use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                files[entry.name] = if (entry.isDirectory) null else stream.readBytes()
            }
        }

        return files
    }

    private fun repack(files: MutableMap<String, ByteArray?>, output: File) {
        val dir = output.absoluteFile.parentFile
        if (!dir.exists()) {
            dir.mkdirs()
        } else if (output.exists()) {
            output.delete()
        }

        ZipOutputStream(output.outputStream().buffered(1024 * 1024)).use { stream ->
            stream.setLevel(Deflater.BEST_COMPRESSION)
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
        return classes.asSequence()
            .map { classFile ->
                DataInputStream(classFile.bytes.inputStream()).use {
                    classFile.classPath to readClassDependencies(it)
                }
            }
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
                        val prev = ++i
                        while (desc[i] != ';') {
                            i++
                        }
                        set.add(desc.substring(prev, i))
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

    private class ClassFile(val path: String, val classPath: String, val bytes: ByteArray)

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            var input: String? = null
            var output: String? = null
            val keeps = mutableListOf<String>()

            var i = 0
            while (i < args.size) {
                when (args[i++]) {
                    "-i", "-in", "-input" -> input = args[i++]
                    "-o", "-out", "-output" -> output = args[i++]
                    "-k", "-keep" -> keeps += args[i++]
                }
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

            JarOptimizer().optimize(File(input), File(output), keeps)
        }
    }
}