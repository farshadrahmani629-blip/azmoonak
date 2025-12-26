package com.examapp.data.model

import com.google.gson.annotations.SerializedName

/**
 * پاسخ دریافت لیست آزمون‌ها از سرور
 */
data class ExamsResponse(
    @SerializedName("exams")
    val exams: List<ExamRemote>
)

/**
 * مدل آزمون دریافتی از سرور
 */
data class ExamRemote(
    @SerializedName("id")
    val id: Int,

    @SerializedName("title")
    val title: String,

    @SerializedName("description")
    val description: String?,

    @SerializedName("duration_minutes")
    val durationMinutes: Int,

    @SerializedName("total_questions")
    val totalQuestions: Int,

    @SerializedName("price")
    val price: Double? = 0.0,

    @SerializedName("is_published")
    val isPublished: Boolean = true,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("questions")
    val questions: List<QuestionRemote> = emptyList()
)

/**
 * مدل سوال دریافتی از سرور
 */
data class QuestionRemote(
    @SerializedName("id")
    val id: Int,

    @SerializedName("exam_id")
    val examId: Int,

    @SerializedName("question_text")
    val questionText: String,

    @SerializedName("question_type")
    val questionType: String, // "multiple_choice", "true_false", "descriptive"

    @SerializedName("options")
    val options: List<OptionRemote> = emptyList(),

    @SerializedName("correct_answer")
    val correctAnswer: String? = null, // برای سوالات تستی

    @SerializedName("points")
    val points: Int = 1
)

/**
 * مدل گزینه دریافتی از سرور
 */
data class OptionRemote(
    @SerializedName("id")
    val id: Int,

    @SerializedName("option_text")
    val optionText: String,

    @SerializedName("letter")
    val letter: String // A, B, C, D
)

/**
 * مدل ارسال پاسخ‌های آزمون به سرور
 */
data class SubmitExamRequest(
    @SerializedName("exam_id")
    val examId: Int,

    @SerializedName("answers")
    val answers: List<UserAnswerRemote>
)

/**
 * مدل هر پاسخ کاربر
 */
data class UserAnswerRemote(
    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("selected_option")
    val selectedOption: String?, // برای سوالات تستی
    // برای سوالات تشریحی این فیلد خالی می‌شود

    @SerializedName("descriptive_answer")
    val descriptiveAnswer: String? = null // برای سوالات تشریحی
)

/**
 * پاسخ سرور به ارسال آزمون
 */
data class SubmitExamResponse(
    @SerializedName("score")
    val score: Int,

    @SerializedName("total_score")
    val totalScore: Int,

    @SerializedName("correct_answers")
    val correctAnswers: Int,

    @SerializedName("wrong_answers")
    val wrongAnswers: Int,

    @SerializedName("answers_details")
    val answersDetails: List<AnswerDetail>
)

/**
 * جزئیات هر پاسخ
 */
data class AnswerDetail(
    @SerializedName("question_id")
    val questionId: Int,

    @SerializedName("is_correct")
    val isCorrect: Boolean,

    @SerializedName("correct_answer")
    val correctAnswer: String?
)