package ru.noxly.baumforms.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.noxly.baumforms.db.entity.StudentEntity

@Dao
interface StudentDao {

    @Query("SELECT * FROM students WHERE sessionId = :sessionId")
    fun getStudentsBySession(sessionId: Int): Flow<List<StudentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: StudentEntity): Long

    @Delete
    suspend fun delete(student: StudentEntity)

    @Query("DELETE FROM students WHERE sessionId = :sessionId")
    suspend fun deleteAllForSession(sessionId: Int)

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): StudentEntity?

}