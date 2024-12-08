package com.example.gps_image_notes

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface NoteDao {

    @Insert
    fun insertAll(vararg notes: Note)

    @Insert
    fun insert(note: Note)

    @Delete
    fun delete(note: Note)

    @Update
    fun update(note: Note)

    @Query("SELECT * FROM notes")
    fun getAllNotes(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteByID(noteId: Int): Note
}