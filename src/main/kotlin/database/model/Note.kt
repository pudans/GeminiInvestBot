package ru.pudans.investrobot.database.model

import org.ktorm.entity.Entity
import org.ktorm.schema.Table
import org.ktorm.schema.date
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import java.time.LocalDate

interface Note : Entity<Note> {
    companion object : Entity.Factory<Note>()

    val id: Int
    val figi: String
    val date: LocalDate
    val content: String
}

object Notes : Table<Note>("t_notes") {
    val id = int("id").primaryKey().bindTo { it.id }
    val figi = varchar("figi").bindTo { it.figi }
    val date = date("date").bindTo { it.date }
    val content = varchar("content").bindTo { it.content }
}