// app/src/main/java/com/examapp/data/database/Converters.kt
package com.examapp.data.database

import androidx.room.TypeConverter
import com.examapp.data.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

class Converters {
    private val gson = Gson()

    // ------------ تبدیل تاریخ و زمان ------------

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringToDate(value: String?): Date? {
        return value?.let {
            try {
                // فرض می‌کنیم فرمت: "yyyy-MM-dd HH:mm:ss"
                // برای فرمت‌های دیگر نیاز به SimpleDateFormat دارید
                Date(it) // این فقط برای مثال است
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun dateToString(date: Date?): String? {
        return date?.toString()
    }

    // ------------ تبدیل Boolean به Int ------------

    @TypeConverter
    fun intToBoolean(value: Int?): Boolean? {
        return value?.let { it == 1 }
    }

    @TypeConverter
    fun booleanToInt(value: Boolean?): Int? {
        return value?.let { if (it) 1 else 0 }
    }

    // ------------ تبدیل Enumها ------------

    @TypeConverter
    fun stringToQuestionType(value: String?): QuestionType? {
        return value?.let {
            try {
                QuestionType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun questionTypeToString(questionType: QuestionType?): String? {
        return questionType?.name
    }

    @TypeConverter
    fun stringToDifficultyLevel(value: String?): DifficultyLevel? {
        return value?.let {
            try {
                DifficultyLevel.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun difficultyLevelToString(difficulty: DifficultyLevel?): String? {
        return difficulty?.name
    }

    @TypeConverter
    fun stringToBloomLevel(value: String?): BloomLevel? {
        return value?.let {
            try {
                BloomLevel.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun bloomLevelToString(bloomLevel: BloomLevel?): String? {
        return bloomLevel?.name
    }

    @TypeConverter
    fun stringToExamStatus(value: String?): ExamStatus? {
        return value?.let {
            try {
                ExamStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    @TypeConverter
    fun examStatusToString(status: ExamStatus?): String? {
        return status?.name
    }

    // ------------ تبدیل لیست Enumها ------------

    @TypeConverter
    fun stringListToQuestionTypeList(value: String?): List<QuestionType>? {
        return value?.let {
            try {
                val stringList: List<String> = gson.fromJson(it, object : TypeToken<List<String>>() {}.type)
                stringList.mapNotNull { name ->
                    try {
                        QuestionType.valueOf(name)
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    @TypeConverter
    fun questionTypeListToString(list: List<QuestionType>?): String? {
        return list?.let {
            gson.toJson(it.map { type -> type.name })
        }
    }

    // ------------ تبدیل لیست‌های ساده ------------

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            try {
                gson.fromJson(it, object : TypeToken<List<String>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toIntList(value: String?): List<Int>? {
        return value?.let {
            try {
                gson.fromJson(it, object : TypeToken<List<Int>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ------------ تبدیل Mapهای ساده ------------

    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.let {
            try {
                gson.fromJson(it, object : TypeToken<Map<String, String>>() {}.type)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }

    // ------------ تبدیل اشیاء پیچیده ------------

    @TypeConverter
    fun fromQuestionList(value: List<Question>?): String? {
        return value?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toQuestionList(value: String?): List<Question>? {
        return value?.let {
            try {
                gson.fromJson(it, object : TypeToken<List<Question>>() {}.type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // ------------ تبدیل JSON عمومی ------------

    @TypeConverter
    fun fromJson(value: String?): Map<String, Any>? {
        return value?.let {
            try {
                gson.fromJson(it, object : TypeToken<Map<String, Any>>() {}.type)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun toJson(value: Map<String, Any>?): String? {
        return value?.let { gson.toJson(it) }
    }
}