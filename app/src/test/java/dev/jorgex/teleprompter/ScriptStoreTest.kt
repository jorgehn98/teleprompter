package dev.jorgex.teleprompter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScriptStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun store() = ScriptStore(temp.root)

    @Test
    fun `create stores a txt file named after the title`() {
        val store = store()
        val script = store.create("My script", "Hello world")

        assertEquals("My script", script.title)
        assertEquals("My script.txt", script.fileName)
        assertEquals("Hello world", store.read(script.fileName))
        assertEquals(1, store.list().size)
    }

    @Test
    fun `duplicate titles do not overwrite each other`() {
        val store = store()
        val first = store.create("Script", "one")
        val second = store.create("Script", "two")

        assertEquals("Script.txt", first.fileName)
        assertEquals("Script (2).txt", second.fileName)
        assertEquals("one", store.read(first.fileName))
        assertEquals("two", store.read(second.fileName))
    }

    @Test
    fun `forbidden characters in a title are sanitized`() {
        val store = store()
        val script = store.create("a/b\\c:d\ne", "text")

        assertTrue(script.fileName.endsWith(".txt"))
        assertTrue(script.fileName.none { it in "\\/:*?\"<>|\n" })
        assertEquals("text", store.read(script.fileName))
    }

    @Test
    fun `a blank title falls back to Script`() {
        val store = store()
        assertEquals("Script", store.create("   ", "text").title)
    }

    @Test
    fun `rename moves the content to the new name`() {
        val store = store()
        val script = store.create("Old", "content")

        val renamed = store.rename(script.fileName, "New")

        assertEquals("New.txt", renamed)
        assertEquals("content", store.read(renamed))
        assertNull(store.find(script.fileName))
    }

    @Test
    fun `renaming to an existing title avoids the collision`() {
        val store = store()
        store.create("Taken", "a")
        val other = store.create("Other", "b")

        val renamed = store.rename(other.fileName, "Taken")

        assertEquals("Taken (2).txt", renamed)
        assertEquals("b", store.read(renamed))
    }

    @Test
    fun `renaming to the same title does nothing`() {
        val store = store()
        val script = store.create("Same", "x")

        assertEquals(script.fileName, store.rename(script.fileName, "Same"))
        assertNotNull(store.find(script.fileName))
    }

    @Test
    fun `save overwrites the content`() {
        val store = store()
        val script = store.create("Script", "old")

        store.save(script.fileName, "new")

        assertEquals("new", store.read(script.fileName))
    }

    @Test
    fun `delete removes the script from the list`() {
        val store = store()
        val script = store.create("Script", "text")

        store.delete(script.fileName)

        assertTrue(store.list().isEmpty())
        assertNull(store.find(script.fileName))
    }

    @Test
    fun `a missing script reads as empty`() {
        assertEquals("", store().read("missing.txt"))
    }

    @Test
    fun `the list only includes txt files`() {
        val store = store()
        store.create("Script", "text")
        File(temp.root, "scripts/notes.md").writeText("not a script")

        val titles = store.list().map { it.title }

        assertEquals(listOf("Script"), titles)
    }
}
