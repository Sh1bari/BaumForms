package ru.noxly.baumforms.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.noxly.baumforms.db.AppDatabase
import ru.noxly.baumforms.db.dao.StudentDao
import ru.noxly.baumforms.db.dao.TestSessionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = Room.databaseBuilder(
        context,
        AppDatabase::class.java,
        "baumforms.db"
    ).build()

    @Provides
    fun provideTestSessionDao(db: AppDatabase): TestSessionDao = db.testSessionDao()

    @Provides
    fun provideStudentDao(db: AppDatabase): StudentDao = db.studentDao()
}