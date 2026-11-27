package com.example.mynotes

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class NotesDatabaseTest {
    @Test
    fun createsEditsAndDeletesPersistentNotes() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "notes-test-${UUID.randomUUID()}.db"
        try {
            val original = NotesDatabase(context, name).use {
                it.save(null, "First note", "Original text")
            }
            NotesDatabase(context, name).use {
                assertEquals(listOf(original), it.notes())
                val edited = it.save(original.id, "Updated", "Zażółć gęślą")
                assertEquals(original.id, edited.id)
                assertEquals(listOf(edited), it.notes())
            }
            NotesDatabase(context, name).use {
                assertEquals("Zażółć gęślą", it.notes().single().body)
                it.delete(original.id)
            }
            NotesDatabase(context, name).use {
                assertTrue(it.notes().isEmpty())
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
