package ru.noxly.baumforms.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.noxly.baumforms.model.TestQuestion

@Entity(tableName = "test_sessions")
data class TestSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val filePath: String? = null,
    val status: TestSessionStatus = TestSessionStatus.CREATED,
    val questionsJson: String? = null,
    val manualGradesJson: String? = null
)

enum class TestSessionStatus {
    CREATED,
    STARTED,
    FINISHED
}

class TestSessionStatusConverter {

    @TypeConverter
    fun fromStatus(status: TestSessionStatus): String = status.name

    @TypeConverter
    fun toStatus(value: String): TestSessionStatus =
        TestSessionStatus.valueOf(value)
}

class TestQuestionConverter {

    private val gson = Gson()

    @TypeConverter
    fun fromList(value: List<TestQuestion>?): String? {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toList(json: String?): List<TestQuestion>? {
        if (json.isNullOrBlank()) return null
        val type = object : TypeToken<List<TestQuestion>>() {}.type
        return gson.fromJson(json, type)
    }
}