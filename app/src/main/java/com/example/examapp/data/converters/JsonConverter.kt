// app/src/main/java/com/examapp/data/converters/JsonConverter.kt
package com.examapp.data.converters

import androidx.room.TypeConverter
import com.examapp.core.models.FeedbackTemplate
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class JsonConverter {
    private val gson = Gson()

    // List<String>
    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringList(json: String?): List<String>? {
        return json?.let {
            val type: Type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    // List<Int>
    @TypeConverter
    fun fromIntList(list: List<Int>?): String? {
        return list?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toIntList(json: String?): List<Int>? {
        return json?.let {
            val type: Type = object : TypeToken<List<Int>>() {}.type
            gson.fromJson(it, type)
        }
    }

    // Map<String, String>
    @TypeConverter
    fun fromStringMap(map: Map<String, String>?): String? {
        return map?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toStringMap(json: String?): Map<String, String>? {
        return json?.let {
            val type: Type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(it, type)
        }
    }

    // Map<String, FeedbackTemplate>
    @TypeConverter
    fun fromFeedbackMap(map: Map<String, FeedbackTemplate>?): String? {
        return map?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toFeedbackMap(json: String?): Map<String, FeedbackTemplate>? {
        return json?.let {
            val type: Type = object : TypeToken<Map<String, FeedbackTemplate>>() {}.type
            gson.fromJson(it, type)
        }
    }

    // Converters for QuestionType, DifficultyLevel, BloomLevel enums
    @TypeConverter
    fun fromQuestionTypeList(list: List<com.examapp.data.models.QuestionType>?): String? {
        return list?.let { types ->
            gson.toJson(types.map { it.name })
        }
    }

    @TypeConverter
    fun toQuestionTypeList(json: String?): List<com.examapp.data.models.QuestionType>? {
        return json?.let {
            val type: Type = object : TypeToken<List<String>>() {}.type
            val names: List<String> = gson.fromJson(it, type)
            return names.mapNotNull { name ->
                try {
                    com.examapp.data.models.QuestionType.valueOf(name)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }

    @TypeConverter
    fun fromDifficultyLevel(value: com.examapp.data.models.DifficultyLevel): String {
        return value.name
    }

    @TypeConverter
    fun toDifficultyLevel(name: String): com.examapp.data.models.DifficultyLevel {
        return com.examapp.data.models.DifficultyLevel.valueOf(name)
    }

    @TypeConverter
    fun fromBloomLevel(value: com.examapp.data.models.BloomLevel): String {
        return value.name
    }

    @TypeConverter
    fun toBloomLevel(name: String): com.examapp.data.models.BloomLevel {
        return com.examapp.data.models.BloomLevel.valueOf(name)
    }

    // برای تبدیل تاریخ و زمان
    @TypeConverter
    fun fromLong(value: Long?): String? {
        return value?.toString()
    }

    @TypeConverter
    fun toLong(value: String?): Long? {
        return value?.toLongOrNull()
    }

    // برای تبدیل Boolean
    @TypeConverter
    fun fromBoolean(value: Boolean): Int {
        return if (value) 1 else 0
    }

    @TypeConverter
    fun toBoolean(value: Int): Boolean {
        return value == 1
    }
}