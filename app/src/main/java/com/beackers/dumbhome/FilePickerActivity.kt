package com.beackers.dumbhome

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class FilePickerActivity : AppCompatActivity() {
    private lateinit var pathText: TextView
    private lateinit var list: RecyclerView
    private lateinit var adapter: SimpleTextAdapter

    private var currentDir: File = Environment.getExternalStorageDirectory()
    private var rows: List<File> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_picker)

        pathText = findViewById(R.id.pathText)
        list = findViewById(R.id.fileList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = SimpleTextAdapter(emptyList()) { onRowSelected(it) }
        list.adapter = adapter

        renderDirectory(currentDir)
    }

    private fun renderDirectory(dir: File) {
        currentDir = dir
        pathText.text = "Path: ${dir.absolutePath}"
        val children = dir.listFiles()?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() }) ?: emptyList()
        rows = buildList {
            if (dir.parentFile != null) add(File(".."))
            addAll(children.filter { it.isDirectory || it.extension.lowercase() in setOf("jpg", "jpeg", "png", "webp") })
        }
        adapter.submit(rows.map {
            when {
                it.path == ".." -> ".."
                it.isDirectory -> "📁 ${it.name}"
                else -> "🖼 ${it.name}"
            }
        })
    }

    private fun onRowSelected(index: Int) {
        val item = rows[index]
        if (item.path == "..") {
            currentDir.parentFile?.let { renderDirectory(it) }
            return
        }
        if (item.isDirectory) {
            renderDirectory(item)
            return
        }

        setResult(Activity.RESULT_OK, Intent().setData(Uri.fromFile(item)))
        finish()
    }
}
