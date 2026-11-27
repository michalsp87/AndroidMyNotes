package com.example.mynotes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteException

internal data class Note(
    val id: Long,
    val title: String,
    val body: String,
    val updatedAt: Long,
)

internal class NotesDatabase(context: Context, name: String = "notes.db") :
    SQLiteOpenHelper(context, name, null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE notes (
                id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Missing notes database migration from $oldVersion to $newVersion")
    }

    fun notes(): List<Note> = readableDatabase.query(
        "notes", arrayOf("id", "title", "body", "updated_at"),
        null, null, null, null, "updated_at DESC, id DESC",
    ).use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                add(Note(cursor.getLong(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3)))
            }
        }
    }

    fun save(id: Long?, title: String, body: String): Note {
        val timestamp = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("title", title)
            put("body", body)
            put("updated_at", timestamp)
        }
        val db = writableDatabase
        val savedId = if (id == null) {
            db.insertOrThrow("notes", null, values)
        } else {
            if (db.update("notes", values, "id = ?", arrayOf(id.toString())) != 1) {
                throw SQLiteException("Note no longer exists")
            }
            id
        }
        return Note(savedId, title, body, timestamp)
    }

    fun delete(id: Long) {
        writableDatabase.delete("notes", "id = ?", arrayOf(id.toString()))
    }
    companion object {
        private var instance: NotesDatabase? = null

        @Synchronized
        fun getInstance(context: Context): NotesDatabase =
            instance ?: NotesDatabase(context.applicationContext).also { instance = it }
    }
}
