package dev.jorgex.teleprompter

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText

class EditorActivity : Activity() {

    private lateinit var store: ScriptStore
    private lateinit var titleInput: EditText
    private lateinit var contentInput: EditText
    private var fileName: String? = null
    private var dirty = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        store = ScriptStore(filesDir)

        titleInput = findViewById(R.id.titleInput)
        contentInput = findViewById(R.id.contentInput)
        fileName = intent.getStringExtra(EXTRA_FILE)

        fileName?.let { name ->
            titleInput.setText(store.find(name)?.title ?: name.removeSuffix(".txt"))
            contentInput.setText(store.read(name))
            contentInput.setSelection(contentInput.text.length)
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                dirty = true
            }
        }
        titleInput.addTextChangedListener(watcher)
        contentInput.addTextChangedListener(watcher)
        // Loading the script does not count as a change.
        dirty = false

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSave).setOnClickListener {
            if (save()) finish()
        }
        findViewById<Button>(R.id.btnStart).setOnClickListener {
            if (save()) PromptLauncher.start(this, contentInput.text.toString())
        }
    }

    override fun onPause() {
        super.onPause()
        // Leaving the screen saves: there is no "discard" button and losing the script would be worse.
        if (dirty) save()
    }

    private fun save(): Boolean {
        val content = contentInput.text.toString()
        val title = titleInput.text.toString().trim()
        if (title.isEmpty() && content.isBlank()) return false

        val existing = fileName
        if (existing == null) {
            val created = store.create(title.ifBlank { defaultTitle(content) }, content)
            fileName = created.fileName
            titleInput.setText(created.title)
        } else {
            store.save(existing, content)
            val currentTitle = store.find(existing)?.title
            if (title.isNotEmpty() && title != currentTitle) {
                fileName = store.rename(existing, title)
            }
        }
        dirty = false
        return true
    }

    private fun defaultTitle(content: String): String =
        content.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(40) ?: "Script"

    companion object {
        const val EXTRA_FILE = "file"
    }
}
