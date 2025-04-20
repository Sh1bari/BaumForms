package ru.noxly.baumforms.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ru.noxly.baumforms.db.dao.StudentDao
import ru.noxly.baumforms.db.dao.TestSessionDao
import ru.noxly.baumforms.db.entity.StudentEntity
import ru.noxly.baumforms.db.entity.TestQuestionConverter
import ru.noxly.baumforms.db.entity.TestSessionEntity
import ru.noxly.baumforms.db.entity.TestSessionStatusConverter

@Database(entities = [TestSessionEntity::class, StudentEntity::class], version = 2)
@TypeConverters(TestSessionStatusConverter::class, TestQuestionConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun testSessionDao(): TestSessionDao

    abstract fun studentDao(): StudentDao
}