package dev.jorgex.teleprompter

import java.io.File

data class Script(
    val fileName: String,
    val title: String,
    val modified: Long,
    val characters: Int,
)

/**
 * Plain-text script library. One .txt file per script inside
 * `files/scripts`, and the file name is the title. No database:
 * what is on disk is exactly what the list shows.
 */
class ScriptStore(root: File) {

    private val dir = File(root, "scripts").apply { if (!exists()) mkdirs() }

    fun list(): List<Script> =
        (dir.listFiles { file -> file.isFile && file.name.endsWith(SUFFIX) } ?: emptyArray())
            .map(::toScript)
            .sortedByDescending { it.modified }

    fun find(fileName: String): Script? =
        File(dir, fileName).takeIf { it.isFile }?.let(::toScript)

    fun read(fileName: String): String =
        File(dir, fileName).takeIf { it.isFile }?.readText() ?: ""

    fun create(title: String, content: String): Script {
        val file = File(dir, uniqueName(title))
        file.writeText(content)
        return toScript(file)
    }

    fun save(fileName: String, content: String) {
        File(dir, fileName).writeText(content)
    }

    /** Returns the resulting file name, which changes if the title changes. */
    fun rename(fileName: String, newTitle: String): String {
        val file = File(dir, fileName)
        if (!file.isFile) return fileName
        val target = uniqueName(newTitle, ignore = fileName)
        if (target == fileName) return fileName
        return if (file.renameTo(File(dir, target))) target else fileName
    }

    fun delete(fileName: String) {
        File(dir, fileName).delete()
    }

    private fun toScript(file: File) = Script(
        fileName = file.name,
        title = file.name.removeSuffix(SUFFIX),
        modified = file.lastModified(),
        characters = file.length().toInt(),
    )

    private fun uniqueName(title: String, ignore: String? = null): String {
        val base = safeTitle(title)
        var candidate = "$base$SUFFIX"
        var copy = 2
        while (candidate != ignore && File(dir, candidate).exists()) {
            candidate = "$base ($copy)$SUFFIX"
            copy++
        }
        return candidate
    }

    private fun safeTitle(title: String): String =
        title.replace(Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.')
            .take(80)
            .trim()
            .ifBlank { "Script" }

    private companion object {
        const val SUFFIX = ".txt"
    }
}
