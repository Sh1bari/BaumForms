package ru.noxly.baumforms.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.db.entity.TestSessionStatus

@Dao
interface TestSessionDao {

    @Query("SELECT * FROM test_sessions ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TestSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: TestSessionEntity): Long

    @Delete
    suspend fun delete(session: TestSessionEntity)

    @Query("SELECT * FROM test_sessions WHERE id = :id")
    fun getById(id: Int): Flow<TestSessionEntity?>

    @Query("SELECT * FROM test_sessions WHERE id = :id")
    suspend fun getByIdSuspend(id: Int): TestSessionEntity?

    @Query("UPDATE test_sessions SET status = :status WHERE id = :sessionId")
    suspend fun updateStatus(sessionId: Int, status: TestSessionStatus)

    @Query("SELECT * FROM test_sessions WHERE status = :status")
    suspend fun getSessionsByStatus(status: TestSessionStatus): List<TestSessionEntity>
}