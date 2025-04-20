package ru.noxly.baumforms.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val group: String,
    val mail: String?,
    val sessionId: Int, // foreign key to TestSessionEntity.id
    val answersJson: String? = null
)