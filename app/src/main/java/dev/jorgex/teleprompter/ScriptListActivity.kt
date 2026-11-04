package dev.jorgex.teleprompter

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast

import dev.jorgex.teleprompter.overlay.PromptService

class ScriptListActivity : Activity() {

    private lateinit var store: ScriptStore
    private lateinit var listView: ListView
    private lateinit var runningBar: View
    private var scripts: List<Script> = emptyList()
    private val adapter = ScriptAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        store = ScriptStore(filesDir)

        listView = findViewById(R.id.list)
        listView.adapter = adapter
        listView.emptyView = findViewById<View>(R.id.empty)
        listView.setOnItemClickListener { _, _, position, _ -> openEditor(scripts[position].fileName) }
        listView.setOnItemLongClickListener { _, _, position, _ ->
            showActions(scripts[position])
            true
        }

        findViewById<Button>(R.id.btnNew).setOnClickListener { openEditor(null) }
        findViewById<Button>(R.id.btnImport).setOnClickListener { pickTxt() }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        runningBar = findViewById(R.id.runningBar)
        findViewById<Button>(R.id.btnStopPrompt).setOnClickListener {
            PromptService.stop(this)
            runningBar.visibility = View.GONE
        }

        askNotificationPermissionIfNeeded()
        seedDemoScriptIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        runningBar.visibility = if (PromptService.isRunning) View.VISIBLE else View.GONE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_PICK_TXT || resultCode != RESULT_OK) return
        val uri = data?.data ?: return
        importTxt(uri)
    }

    private fun refresh() {
        scripts = store.list()
        adapter.notifyDataSetChanged()
    }

    private fun openEditor(fileName: String?) {
        startActivity(
            Intent(this, EditorActivity::class.java).putExtra(EditorActivity.EXTRA_FILE, fileName)
        )
    }

    private fun showActions(script: Script) {
        val options = arrayOf("Edit", "Start teleprompter", "Rename", "Delete")
        AlertDialog.Builder(this)
            .setTitle(script.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openEditor(script.fileName)
                    1 -> startPrompt(script)
                    2 -> renameScript(script)
                    3 -> confirmDelete(script)
                }
            }
            .show()
    }

    private fun startPrompt(script: Script) {
        PromptLauncher.start(this, store.read(script.fileName))
    }

    private fun renameScript(script: Script) {
        val input = EditText(this).apply {
            setText(script.title)
            setSelection(text.length)
            setSingleLine()
        }
        val container = FrameLayout(this).apply {
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(
                input,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                )
            )
        }
        AlertDialog.Builder(this)
            .setTitle("Rename script")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                store.rename(script.fileName, input.text.toString())
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(script: Script) {
        AlertDialog.Builder(this)
            .setTitle("Delete script")
            .setMessage("Delete \"${script.title}\"? This can't be undone.")
            .setPositiveButton("Delete") { _, _ ->
                store.delete(script.fileName)
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pickTxt() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/plain"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("text/plain", "text/*", "application/octet-stream"))
        }
        startActivityForResult(intent, REQUEST_PICK_TXT)
    }

    private fun importTxt(uri: Uri) {
        val text = try {
            contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        } catch (exception: Exception) {
            null
        }
        if (text == null) {
            Toast.makeText(this, "Could not read the file.", Toast.LENGTH_LONG).show()
            return
        }
        val name = displayName(uri)?.substringBeforeLast('.') ?: "Imported script"
        val created = store.create(name, text)
        refresh()
        Toast.makeText(this, "Imported: ${created.title}", Toast.LENGTH_SHORT).show()
    }

    private fun displayName(uri: Uri): String? =
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (exception: Exception) {
            null
        }

    private fun seedDemoScriptIfNeeded() {
        val settingsStore = SettingsStore(this)
        if (settingsStore.demoSeeded) return
        store.create(getString(R.string.demo_script_title), getString(R.string.demo_script))
        settingsStore.demoSeeded = true
    }

    private fun askNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inner class ScriptAdapter : BaseAdapter() {
        override fun getCount(): Int = scripts.size
        override fun getItem(position: Int): Any = scripts[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.row_script, parent, false)
            val script = scripts[position]
            view.findViewById<TextView>(R.id.rowTitle).text = script.title
            view.findViewById<TextView>(R.id.rowMeta).text = "${script.characters} characters"
            return view
        }
    }

    private companion object {
        const val REQUEST_PICK_TXT = 10
        const val REQUEST_NOTIFICATIONS = 11
    }
}
