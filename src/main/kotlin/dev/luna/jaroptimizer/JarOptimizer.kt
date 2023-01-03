package dev.luna.jaroptimizer

import it.unimi.dsi.fastutil.objects.*
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
    fun optimize(input: File, output: File, keeps: ObjectSet<String>) {
        require(input.exists()) { "Input file does not exist" }

        val cachedFiles = unpack(input)

        val classes = ObjectArrayList<ClassFile>()
        cachedFiles.asSequence()
            .filter { it.value != null }
            .filter { it.key.endsWith(".class") }
            .map { ClassFile(it.key, it.key.substring(0, it.key.length - 6), it.value!!) }
            .toCollection(classes)

        val dependencyMap = readAllClassDependencies(classes)

        val formattedKeeps = ObjectArrayList<String>()
        keeps.mapTo(formattedKeeps) { it.replace('.', '/') }

        val set = propagateDependencies(classes, dependencyMap, formattedKeeps)
        classes.forEach { classFile ->
            if (!set.contains(classFile.classPath)) {
                cachedFiles.remove(classFile.path)
            }
        }

        repack(cachedFiles, output)
    }

    private fun unpack(input: File): Object2ObjectMap<String, ByteArray?> {
        val files = Object2ObjectOpenHashMap<String, ByteArray?>()

        ZipInputStream(input.inputStream().buffered(1024 * 1024)).use { stream ->
            while (true) {
                val entry = stream.nextEntry ?: break
                files[entry.name] = if (entry.isDirectory) null else stream.readBytes()
            }
        }

        return files
    }

    private fun repack(files: Object2ObjectMap<String, ByteArray?>, output: File) {
        val dir = output.absoluteFile.parentFile
        if (!dir.exists()) {
            dir.mkdirs()
        } else if (output.exists()) {
            output.delete()
        }

        ZipOutputStream(output.outputStream().buffered(1024 * 1024)).use { stream ->
            stream.setLevel(Deflater.BEST_COMPRESSION)
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
        classes: ObjectList<ClassFile>
    ): Object2ObjectMap<String, ObjectSet<String>> {
        val result = Object2ObjectOpenHashMap<String, ObjectSet<String>>()
        classes.forEach { classFile ->
            DataInputStream(classFile.bytes.inputStream()).use {
                result[classFile.classPath] = readClassDependencies(it)
            }
        }
        return result
    }

    private fun readClassDependencies(stream: InputStream): ObjectSet<String> {
        val clazz = ClassParser(stream, "").parse()
        val constPool = clazz.constantPool
        val set = ObjectOpenHashSet<String>()

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

    private fun propagateDependencies(
        classes: ObjectList<ClassFile>,
        map: Object2ObjectMap<String, ObjectSet<String>>,
        keeps: ObjectList<String>
    ): ObjectSet<String> {
        val set = ObjectOpenHashSet<String>()
        val queue = ObjectArrayFIFOQueue<String>()

        if (keeps.isNotEmpty()) {
            classes.forEach { classFile ->
                if (keeps.any { classFile.classPath.startsWith(it) }) {
                    set.add(classFile.classPath)
                    queue.enqueue(classFile.classPath)
                }
            }
        }

        while (!queue.isEmpty) {
            val path = queue.dequeue()
            map[path]?.forEach {
                if (set.add(it)) {
                    queue.enqueue(it)
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
            val keeps = ObjectOpenHashSet<String>()

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