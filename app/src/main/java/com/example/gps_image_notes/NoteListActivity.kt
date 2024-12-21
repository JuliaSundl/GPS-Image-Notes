package com.example.gps_image_notes

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room

class NoteListActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var adapter: NoteAdapter
    private lateinit var db: NoteDatabase
    private lateinit var noteDao: NoteDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_list)

        // Set up Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Initialize ListView
        listView = findViewById(R.id.listView)

        // Initialize Database
        db = Room.databaseBuilder(
            applicationContext,
            NoteDatabase::class.java, "notes"
        ).allowMainThreadQueries().build()
        noteDao = db.noteDao()

        adapter = NoteAdapter(this, noteDao.getAllNotes())
        listView.setAdapter(adapter)

        // Set ClickListener for the ListView
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Get the clicked note
            val clickedNote = adapter.getItem(position)

            // Open NoteEditActivity
            val intent = Intent(this, NoteEditActivity::class.java).apply {
                putExtra("id", clickedNote.id)
            }
            startActivity(intent)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_list, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.add) {
            // Open NoteEditActivity
            val intent = Intent(this, NoteEditActivity::class.java)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        // Reload notes when returning to this activity
        adapter.notes = noteDao.getAllNotes()
        adapter.notifyDataSetChanged()
    }
}