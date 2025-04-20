package ru.noxly.baumforms.service

import kotlinx.coroutines.flow.Flow
import ru.noxly.baumforms.db.dao.StudentDao
import ru.noxly.baumforms.db.entity.StudentEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentService @Inject constructor(
    private val studentDao: StudentDao
) {
    fun getStudentsBySession(sessionId: Int): Flow<List<StudentEntity>> =
        studentDao.getStudentsBySession(sessionId)

    suspend fun addStudent(student: StudentEntity): Long =
        studentDao.insert(student)

    suspend fun deleteStudent(student: StudentEntity) =
        studentDao.delete(student)

    suspend fun deleteAllFromSession(sessionId: Int) =
        studentDao.deleteAllForSession(sessionId)

    suspend fun deleteStudentsBySession(sessionId: Int) {
        studentDao.deleteAllForSession(sessionId)
    }

    suspend fun getStudentById(id: Int): StudentEntity? =
        studentDao.getById(id)

    suspend fun updateStudent(student: StudentEntity) {
        studentDao.insert(student)
    }

}