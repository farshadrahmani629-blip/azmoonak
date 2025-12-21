// app/src/main/java/com/examapp/data/models/FeedbackTemplate.kt
package com.examapp.data.models

import androidx.room.Embedded
import com.google.gson.annotations.SerializedName

data class FeedbackTemplate(
    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("emoji")
    val emoji: String? = null,

    @SerializedName("category")
    val category: FeedbackCategory = FeedbackCategory.GENERAL,

    @SerializedName("points_range")
    val pointsRange: IntRange = 0..100
) {
    enum class FeedbackCategory {
        @SerializedName("general") GENERAL,
        @SerializedName("encouragement") ENCOURAGEMENT,
        @SerializedName("improvement") IMPROVEMENT,
        @SerializedName("strength") STRENGTH,
        @SerializedName("weakness") WEAKNESS
    }

    fun getPersianCategory(): String {
        return when (category) {
            FeedbackCategory.GENERAL -> "عمومی"
            FeedbackCategory.ENCOURAGEMENT -> "تشویق"
            FeedbackCategory.IMPROVEMENT -> "نیاز به بهبود"
            FeedbackCategory.STRENGTH -> "نقاط قوت"
            FeedbackCategory.WEAKNESS -> "نقاط ضعف"
        }
    }

    fun matchesScore(score: Int): Boolean {
        return score in pointsRange
    }
}

// Extension برای لیست FeedbackTemplate
fun List<FeedbackTemplate>.filterByScore(score: Int): List<FeedbackTemplate> {
    return this.filter { it.matchesScore(score) }
}

fun List<FeedbackTemplate>.filterByCategory(category: FeedbackTemplate.FeedbackCategory): List<FeedbackTemplate> {
    return this.filter { it.category == category }
}

fun List<FeedbackTemplate>.getRandomForScore(score: Int): FeedbackTemplate? {
    val matchingTemplates = this.filterByScore(score)
    return if (matchingTemplates.isNotEmpty()) {
        matchingTemplates.random()
    } else {
        // اگر هیچ تمپلیت مناسب نبود، نزدیک‌ترین را برگردان
        this.minByOrNull { template ->
            val distanceToStart = Math.abs(score - template.pointsRange.first)
            val distanceToEnd = Math.abs(score - template.pointsRange.last)
            minOf(distanceToStart, distanceToEnd)
        }
    }
}

// لیست پیش‌فرض تمپلیت‌ها
object DefaultFeedbackTemplates {
    val templates = listOf(
        FeedbackTemplate(
            title = "عالی بود!",
            description = "تو واقعا این مبحث رو کامل یاد گرفتی. ادامه بده!",
            emoji = "🎉",
            category = FeedbackTemplate.FeedbackCategory.ENCOURAGEMENT,
            pointsRange = 90..100
        ),
        FeedbackTemplate(
            title = "خیلی خوب",
            description = "عملکرد خوبی داشتی، فقط چند نکته کوچیک باقی مونده.",
            emoji = "👍",
            category = FeedbackTemplate.FeedbackCategory.STRENGTH,
            pointsRange = 75..89
        ),
        FeedbackTemplate(
            title = "قابل قبول",
            description = "نیاز به تمرین بیشتر داری. دوباره درس رو مرور کن.",
            emoji = "📚",
            category = FeedbackTemplate.FeedbackCategory.IMPROVEMENT,
            pointsRange = 50..74
        ),
        FeedbackTemplate(
            title = "نیاز به تلاش بیشتر",
            description = "این مبحث رو خوب متوجه نشدی. بهتره از اول درس رو بخونی.",
            emoji = "💪",
            category = FeedbackTemplate.FeedbackCategory.WEAKNESS,
            pointsRange = 0..49
        )
    )
}