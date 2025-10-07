package ru.pudans.investrobot.database

import org.ktorm.database.Database
import org.ktorm.dsl.delete
import org.ktorm.dsl.deleteAll
import org.ktorm.dsl.eq
import org.ktorm.dsl.insert
import org.ktorm.entity.filter
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList
import ru.pudans.investrobot.database.model.Note
import ru.pudans.investrobot.database.model.Notes
import java.time.LocalDate

class NotesRepository {

    private val database = Database.connect(
        url = "jdbc:mysql://localhost:3306/ktorm",
        user = "root",
        password = "***"
    )

    private val Database.notes get() = sequenceOf(Notes)

    fun getAllNotes(): List<Note> {
        return database.notes.toList()
    }

    fun getNotes(figi: String): List<Note> {
        return database.notes.filter { it.figi eq figi }.toList()
    }

    fun insertNote(figi: String, content: String) {
        database.insert(Notes) {
            set(it.figi, figi)
            set(it.date, LocalDate.now())
            set(it.content, content)
        }
    }

    fun delete(figi: String) {
        database.delete(Notes) { it.figi eq figi }
    }

    fun deleteAll() {
        database.deleteAll(Notes)
    }
}