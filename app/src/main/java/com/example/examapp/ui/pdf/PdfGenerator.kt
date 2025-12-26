package com.examapp.ui.exam.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.examapp.R
import com.examapp.data.models.Exam
import com.examapp.data.models.ExamResult
import com.examapp.data.models.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class PdfGenerator @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val PAGE_WIDTH = 595 // A4 width in points (72 DPI)
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 50
        private const val LINE_HEIGHT = 20
        private const val TITLE_FONT_SIZE = 22f
        private const val HEADER_FONT_SIZE = 18f
        private const val NORMAL_FONT_SIZE = 14f
        private const val SMALL_FONT_SIZE = 12f
        private const val VERY_SMALL_FONT_SIZE = 10f

        // Persian/Arabic digits
        private val PERSIAN_DIGITS = arrayOf("۰", "۱", "۲", "۳", "۴", "۵", "۶", "۷", "۸", "۹")

        // Colors
        private val COLOR_PRIMARY = Color.parseColor("#2C3E50")
        private val COLOR_SUCCESS = Color.parseColor("#27AE60")
        private val COLOR_WARNING = Color.parseColor("#F39C12")
        private val COLOR_DANGER = Color.parseColor("#E74C3C")
        private val COLOR_INFO = Color.parseColor("#3498DB")
    }

    /**
     * تولید PDF آزمون خالی (برای چاپ)
     */
    suspend fun generateExamPdf(
        exam: Exam,
        studentName: String,
        grade: Int,
        teacherName: String,
        schoolName: String = "مدرسه نمونه"
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        var pageNumber = 1

        // صفحه اول - جلد و اطلاعات
        val coverPage = createCoverPage(document, exam, studentName, grade, teacherName, schoolName)
        document.finishPage(coverPage)

        // صفحه راهنما
        val instructionsPage = createInstructionsPage(document, exam)
        document.finishPage(instructionsPage)

        // صفحات سوالات
        val questionsPages = createQuestionsPages(document, exam)
        questionsPages.forEach { document.finishPage(it) }

        // صفحه پاسخنامه (اگر سوال تستی دارد)
        if (exam.questions.any { it.type == "mcq" }) {
            val answerSheetPage = createAnswerSheetPage(document, exam)
            document.finishPage(answerSheetPage)
        }

        // صفحه تشریحی (اگر سوال تشریحی دارد)
        if (exam.questions.any { it.type == "short_answer" || it.type == "essay" }) {
            val descriptivePage = createDescriptivePage(document, exam)
            document.finishPage(descriptivePage)
        }

        // ذخیره فایل
        return@withContext saveDocument(document, "exam_${exam.id}_${System.currentTimeMillis()}")
    }

    /**
     * تولید PDF پاسخنامه با نتایج
     */
    suspend fun generateResultPdf(
        exam: Exam,
        result: ExamResult,
        studentName: String,
        teacherName: String,
        schoolName: String = "مدرسه نمونه"
    ): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()

        // صفحه اول - نتایج کلی
        val resultsPage = createResultsSummaryPage(document, exam, result, studentName, teacherName, schoolName)
        document.finishPage(resultsPage)

        // صفحه تحلیل نتیجه
        val analysisPage = createAnalysisPage(document, result)
        document.finishPage(analysisPage)

        // صفحه پاسخ‌های صحیح
        val answersPage = createCorrectAnswersPage(document, exam, result)
        document.finishPage(answersPage)

        // صفحه نکات آموزشی
        val tipsPage = createStudyTipsPage(document, exam, result)
        document.finishPage(tipsPage)

        // ذخیره فایل
        return@withContext saveDocument(document, "result_${exam.id}_${studentName}_${System.currentTimeMillis()}")
    }

    /**
     * صفحه جلد آزمون
     */
    private fun createCoverPage(
        document: PdfDocument,
        exam: Exam,
        studentName: String,
        grade: Int,
        teacherName: String,
        schoolName: String
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN.toFloat()

        // لوگو یا عنوان مدرسه
        drawCenteredText(
            canvas = canvas,
            text = schoolName,
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE,
            isBold = true,
            color = COLOR_PRIMARY
        )
        y += LINE_HEIGHT * 3

        // خط جداکننده
        drawHorizontalLine(canvas, y, PAGE_WIDTH - MARGIN * 2)
        y += LINE_HEIGHT * 2

        // عنوان آزمون
        drawCenteredText(
            canvas = canvas,
            text = "آزمون ${exam.subject}",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE + 4,
            isBold = true
        )
        y += LINE_HEIGHT * 2

        // اطلاعات آزمون در کادر
        y = drawInfoBox(canvas, y, listOf(
            "📝 عنوان: ${exam.title}",
            "👤 دانش‌آموز: $studentName",
            "🎯 پایه: ${convertToPersianDigits(grade)}",
            "🏫 معلم: $teacherName",
            "📚 درس: ${exam.subject}",
            "⏱ زمان: ${convertToPersianDigits(exam.duration)} دقیقه",
            "📊 سطح: ${exam.difficulty}",
            "📅 تاریخ: ${getPersianDate()}"
        ))

        y += LINE_HEIGHT * 3

        // راهنمای نمره‌دهی
        drawCenteredText(
            canvas = canvas,
            text = "راهنمای نمره‌دهی",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = HEADER_FONT_SIZE,
            isBold = true,
            color = COLOR_INFO
        )
        y += LINE_HEIGHT * 1.5f

        val scoringGuide = listOf(
            "• هر سوال تستی: ${convertToPersianDigits(3)} نمره",
            "• هر سوال تشریحی کوتاه: ${convertToPersianDigits(4)} نمره",
            "• هر سوال تشریحی بلند: ${convertToPersianDigits(5)} نمره",
            "• نمره منفی: ${convertToPersianDigits(1)}- برای هر پاسخ غلط",
            "• نمره کل: ${convertToPersianDigits(100)}"
        )

        scoringGuide.forEach { guide ->
            y = drawText(
                canvas = canvas,
                text = guide,
                x = (PAGE_WIDTH / 2 - 100).toFloat(),
                y = y,
                fontSize = NORMAL_FONT_SIZE
            )
            y += LINE_HEIGHT
        }

        // فضای امضا در پایین صفحه
        y = PAGE_HEIGHT - MARGIN - 100
        drawHorizontalLine(canvas, y, 200f)
        y += 20

        drawCenteredText(
            canvas = canvas,
            text = "امضای مسئول",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = SMALL_FONT_SIZE,
            color = Color.GRAY
        )

        return page
    }

    /**
     * صفحه راهنمای آزمون
     */
    private fun createInstructionsPage(
        document: PdfDocument,
        exam: Exam
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN.toFloat()

        // عنوان
        drawCenteredText(
            canvas = canvas,
            text = "راهنمای شرکت در آزمون",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE,
            isBold = true,
            color = COLOR_PRIMARY
        )
        y += LINE_HEIGHT * 2

        // کادر راهنمایی
        val instructions = listOf(
            "📌 قبل از شروع آزمون:",
            "  ۱. نام و نام خانوادگی خود را در جای مشخص شده بنویسید.",
            "  ۲. زمان آزمون را کنترل کنید.",
            "  ۳. تمام وسایل مورد نیاز (خودکار، مداد، پاک‌کن) را آماده کنید.",
            "",
            "📌 هنگام پاسخ‌گویی:",
            "  ۴. سوالات را با دقت کامل بخوانید.",
            "  ۵. برای سوالات تستی فقط یک گزینه صحیح است.",
            "  ۶. در صورت عدم اطمینان، پاسخ ندهید (نمره منفی دارد).",
            "  ۷. ابتدا به سوالات آسان پاسخ دهید.",
            "",
            "📌 پس از آزمون:",
            "  ۸. پاسخ‌ها را مرور کنید.",
            "  ۹. پاسخنامه را تمیز و خوانا تحویل دهید.",
            "  ۱۰. زمان تحویل را رعایت کنید."
        )

        y = drawTextBox(canvas, y, instructions, backgroundColor = Color.parseColor("#F8F9FA"))

        y += LINE_HEIGHT * 2

        // توزیع سوالات
        val mcqCount = exam.questions.count { it.type == "mcq" }
        val shortAnswerCount = exam.questions.count { it.type == "short_answer" }
        val essayCount = exam.questions.count { it.type == "essay" }

        drawCenteredText(
            canvas = canvas,
            text = "توزیع سوالات",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = HEADER_FONT_SIZE,
            isBold = true
        )
        y += LINE_HEIGHT * 1.5f

        val distribution = listOf(
            "• سوالات تستی: ${convertToPersianDigits(mcqCount)} سوال (${convertToPersianDigits(mcqCount * 3)} نمره)",
            "• سوالات کوتاه پاسخ: ${convertToPersianDigits(shortAnswerCount)} سوال (${convertToPersianDigits(shortAnswerCount * 4)} نمره)",
            "• سوالات تشریحی: ${convertToPersianDigits(essayCount)} سوال (${convertToPersianDigits(essayCount * 5)} نمره)",
            "• مجموع: ${convertToPersianDigits(exam.questions.size)} سوال (${convertToPersianDigits(100)} نمره)"
        )

        distribution.forEach { item ->
            y = drawText(
                canvas = canvas,
                text = item,
                x = (PAGE_WIDTH / 2 - 120).toFloat(),
                y = y,
                fontSize = NORMAL_FONT_SIZE
            )
            y += LINE_HEIGHT
        }

        // نکته مهم
        y += LINE_HEIGHT
        drawTextBox(
            canvas = canvas,
            y = y,
            lines = listOf(
                "⚠️ نکته مهم:",
                "پاسخ‌های خود را با خودکار آبی یا مشکی بنویسید.",
                "از خودکار قرمز استفاده نکنید.",
                "خط خوردگی و لاک غلط گیر مجاز نیست."
            ),
            backgroundColor = Color.parseColor("#FFF3CD"),
            borderColor = Color.parseColor("#FFEEBA")
        )

        return page
    }

    /**
     * صفحات سوالات
     */
    private fun createQuestionsPages(
        document: PdfDocument,
        exam: Exam
    ): List<PdfDocument.Page> {
        val pages = mutableListOf<PdfDocument.Page>()
        var currentPage: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = MARGIN.toFloat()
        var questionNumber = 1

        exam.questions.forEach { question ->
            // اگر صفحه نداریم یا صفحه پر شده، صفحه جدید ایجاد کن
            if (currentPage == null || y > PAGE_HEIGHT - MARGIN - 150) {
                currentPage?.let { pages.add(it) }

                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                currentPage = document.startPage(pageInfo)
                canvas = currentPage!!.canvas
                y = MARGIN.toFloat()

                // هدر صفحه (شماره صفحه و عنوان)
                drawPageHeader(canvas!!, "سوالات آزمون ${exam.subject}")
            }

            y = drawQuestion(canvas!!, question, questionNumber, y)
            y += LINE_HEIGHT * 1.5f
            questionNumber++
        }

        // آخرین صفحه را اضافه کن
        currentPage?.let { pages.add(it) }

        return pages
    }

    /**
     * صفحه پاسخنامه تستی
     */
    private fun createAnswerSheetPage(
        document: PdfDocument,
        exam: Exam
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN.toFloat()

        // عنوان
        drawCenteredText(
            canvas = canvas,
            text = "پاسخنامه تستی",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE,
            isBold = true,
            color = COLOR_INFO
        )
        y += LINE_HEIGHT * 2

        // توضیحات
        drawText(
            canvas = canvas,
            text = "لطفاً گزینه صحیح هر سوال را در خانه مربوطه علامت بزنید:",
            x = MARGIN.toFloat(),
            y = y,
            fontSize = NORMAL_FONT_SIZE
        )
        y += LINE_HEIGHT * 1.5f

        // جدول پاسخنامه
        val mcqQuestions = exam.questions.filter { it.type == "mcq" }
        val columns = 4
        val rows = (mcqQuestions.size + columns - 1) / columns
        val cellWidth = 100
        val cellHeight = 40

        for (row in 0 until rows) {
            for (col in 0 until columns) {
                val index = row * columns + col
                if (index < mcqQuestions.size) {
                    val xPos = MARGIN + col * (cellWidth + 20)
                    val yPos = y + row * (cellHeight + 10)

                    // کادر سوال
                    drawRect(
                        canvas = canvas,
                        x = xPos.toFloat(),
                        y = yPos.toFloat(),
                        width = cellWidth.toFloat(),
                        height = cellHeight.toFloat(),
                        borderColor = Color.GRAY
                    )

                    // شماره سوال
                    drawCenteredText(
                        canvas = canvas,
                        text = convertToPersianDigits(index + 1),
                        x = xPos + cellWidth / 2f,
                        y = yPos + 15,
                        fontSize = NORMAL_FONT_SIZE,
                        isBold = true
                    )

                    // خانه‌های گزینه‌ها
                    val optionWidth = 15
                    val options = listOf("الف", "ب", "ج", "د")

                    options.forEachIndexed { optIndex, option ->
                        val optX = xPos + 20 + optIndex * (optionWidth + 10)
                        val optY = yPos + 25

                        // دایره گزینه
                        canvas.drawCircle(
                            optX + optionWidth / 2f,
                            optY + optionWidth / 2f,
                            optionWidth / 2f,
                            Paint().apply {
                                color = Color.WHITE
                                style = Paint.Style.STROKE
                                strokeWidth = 1f
                            }
                        )

                        // حرف گزینه
                        drawCenteredText(
                            canvas = canvas,
                            text = option,
                            x = optX + optionWidth / 2f,
                            y = optY + optionWidth / 2f - 4,
                            fontSize = 8f
                        )
                    }
                }
            }
        }

        y += rows * (cellHeight + 10) + LINE_HEIGHT * 2

        // راهنمای علامت‌گذاری
        drawTextBox(
            canvas = canvas,
            y = y,
            lines = listOf(
                "راهنمای علامت‌گذاری:",
                "• دایره مربوط به گزینه صحیح را کاملاً پر کنید.",
                "• از علامت‌های دیگر مانند ضربدر یا تیک استفاده نکنید.",
                "• در صورت تغییر پاسخ، گزینه قبلی را کاملاً پاک کنید."
            ),
            backgroundColor = Color.parseColor("#E8F4FD")
        )

        return page
    }

    /**
     * صفحه سوالات تشریحی
     */
    private fun createDescriptivePage(
        document: PdfDocument,
        exam: Exam
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN.toFloat()

        // عنوان
        drawCenteredText(
            canvas = canvas,
            text = "سوالات تشریحی",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE,
            isBold = true,
            color = COLOR_INFO
        )
        y += LINE_HEIGHT * 2

        val descriptiveQuestions = exam.questions.filter { it.type == "short_answer" || it.type == "essay" }
        var questionNum = 1

        descriptiveQuestions.forEach { question ->
            // شماره و متن سوال
            drawText(
                canvas = canvas,
                text = "${convertToPersianDigits(questionNum)}. ${question.text}",
                x = MARGIN.toFloat(),
                y = y,
                fontSize = NORMAL_FONT_SIZE,
                isBold = true
            )
            y += LINE_HEIGHT * 1.5f

            // نمره سوال
            drawText(
                canvas = canvas,
                text = "نمره: ${convertToPersianDigits(if (question.type == "essay") 5 else 4)}",
                x = PAGE_WIDTH - MARGIN - 100.toFloat(),
                y = y - LINE_HEIGHT,
                fontSize = SMALL_FONT_SIZE,
                color = Color.GRAY
            )

            // فضای پاسخ
            val answerLines = if (question.type == "essay") 10 else 5
            repeat(answerLines) {
                drawHorizontalLine(canvas, y, PAGE_WIDTH - MARGIN * 2, isDashed = true)
                y += LINE_HEIGHT
            }

            y += LINE_HEIGHT
            questionNum++
        }

        return page
    }

    /**
     * صفحه نتایج کلی
     */
    private fun createResultsSummaryPage(
        document: PdfDocument,
        exam: Exam,
        result: ExamResult,
        studentName: String,
        teacherName: String,
        schoolName: String
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN.toFloat()

        // عنوان
        drawCenteredText(
            canvas = canvas,
            text = "کارنامه آزمون",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE + 4,
            isBold = true,
            color = COLOR_PRIMARY
        )
        y += LINE_HEIGHT * 2

        drawCenteredText(
            canvas = canvas,
            text = exam.title,
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE,
            color = COLOR_INFO
        )
        y += LINE_HEIGHT * 3

        // اطلاعات دانش‌آموز در کادر
        y = drawInfoBox(canvas, y, listOf(
            "🏫 مدرسه: $schoolName",
            "👤 دانش‌آموز: $studentName",
            "🎯 پایه: ${convertToPersianDigits(exam.grade)}",
            "📚 درس: ${exam.subject}",
            "🏫 معلم: $teacherName",
            "📅 تاریخ آزمون: ${getPersianDate()}"
        ), backgroundColor = Color.parseColor("#E8F4FD"))

        y += LINE_HEIGHT * 2

        // کارت نتایج
        val scoreCardY = y
        val cardWidth = PAGE_WIDTH - MARGIN * 2
        val cardHeight = 150

        // پس‌زمینه کارت
        drawRect(
            canvas = canvas,
            x = MARGIN.toFloat(),
            y = scoreCardY,
            width = cardWidth.toFloat(),
            height = cardHeight.toFloat(),
            backgroundColor = when {
                result.score >= 90 -> Color.parseColor("#D5EDDA")
                result.score >= 70 -> Color.parseColor("#D1ECF1")
                result.score >= 50 -> Color.parseColor("#FFF3CD")
                else -> Color.parseColor("#F8D7DA")
            },
            borderColor = when {
                result.score >= 90 -> Color.parseColor("#C3E6CB")
                result.score >= 70 -> Color.parseColor("#BEE5EB")
                result.score >= 50 -> Color.parseColor("#FFEAA8")
                else -> Color.parseColor("#F5C6CB")
            }
        )

        // نمره اصلی (بزرگ)
        drawCenteredText(
            canvas = canvas,
            text = String.format("%.1f", result.score),
            x = PAGE_WIDTH / 2f,
            y = scoreCardY + 60,
            fontSize = 48f,
            isBold = true,
            color = when {
                result.score >= 90 -> COLOR_SUCCESS
                result.score >= 70 -> COLOR_INFO
                result.score >= 50 -> COLOR_WARNING
                else -> COLOR_DANGER
            }
        )

        // برچسب نمره
        drawCenteredText(
            canvas = canvas,
            text = "از ۱۰۰",
            x = PAGE_WIDTH / 2f,
            y = scoreCardY + 90,
            fontSize = SMALL_FONT_SIZE,
            color = Color.GRAY
        )

        // آمارهای کناری
        val stats = listOf(
            "✅ صحیح: ${convertToPersianDigits(result.correctAnswers)}",
            "❌ غلط: ${convertToPersianDigits(result.wrongAnswers)}",
            "⏱ زمان: ${formatTime(result.timeSpent ?: 0)}",
            "📊 رتبه: ${getGradeText(result.score)}"
        )

        stats.forEachIndexed { index, stat ->
            val xPos = if (index < 2) MARGIN + 30 else PAGE_WIDTH - MARGIN - 130
            val yPos = scoreCardY + 120 + (index % 2) * 20

            drawText(
                canvas = canvas,
                text = stat,
                x = xPos.toFloat(),
                y = yPos.toFloat(),
                fontSize = SMALL_FONT_SIZE
            )
        }

        y = scoreCardY + cardHeight + LINE_HEIGHT * 2

        // نمودار میله‌ای ساده
        drawCenteredText(
            canvas = canvas,
            text = "نمودار عملکرد",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = HEADER_FONT_SIZE,
            isBold = true
        )
        y += LINE_HEIGHT * 1.5f

        // رسم نمودار
        y = drawSimpleBarChart(
            canvas = canvas,
            y = y,
            correct = result.correctAnswers,
            wrong = result.wrongAnswers,
            total = result.totalQuestions
        )

        y += LINE_HEIGHT * 2

        // بازخورد سریع
        val feedback = when {
            result.score >= 90 -> "عالی! 🎉 عملکرد بسیار خوبی داشتید."
            result.score >= 75 -> "خوب! 👍 نقاط قوت خوبی دارید."
            result.score >= 60 -> "قابل قبول. 💪 نیاز به تمرین بیشتر."
            result.score >= 50 -> "نیاز به تلاش. 📚 درس را مرور کنید."
            else -> "نیاز به توجه ویژه. 🔄 با معلم مشورت کنید."
        }

        drawTextBox(
            canvas = canvas,
            y = y,
            lines = listOf("📝 بازخورد: $feedback"),
            backgroundColor = when {
                result.score >= 90 -> Color.parseColor("#D5EDDA")
                result.score >= 75 -> Color.parseColor("#D1ECF1")
                result.score >= 60 -> Color.parseColor("#FFF3CD")
                else -> Color.parseColor("#F8D7DA")
            }
        )

        return page
    }

    /**
     * صفحه تحلیل نتایج
     */
    private fun createAnalysisPage(
        document: PdfDocument,
        result: ExamResult
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN.toFloat()

        // عنوان
        drawCenteredText(
            canvas = canvas,
            text = "تحلیل نتایج",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE,
            isBold = true,
            color = COLOR_INFO
        )
        y += LINE_HEIGHT * 2

        // بازخورد توصیفی کامل
        val detailedFeedback = generateDetailedFeedback(result)
        y = drawTextBox(canvas, y, detailedFeedback, backgroundColor = Color.parseColor("#F8F9FA"))

        y += LINE_HEIGHT * 2

        // نقاط قوت و ضعف
        drawCenteredText(
            canvas = canvas,
            text = "نقاط قوت و ضعف",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = HEADER_FONT_SIZE,
            isBold = true
        )
        y += LINE_HEIGHT * 1.5f

        val strengths = when {
            result.score >= 80 -> listOf(
                "• سرعت پاسخ‌گویی مناسب",
                "• دقت در خواندن سوالات",
                "• مدیریت زمان عالی",
                "• تمرکز بالا"
            )
            result.score >= 60 -> listOf(
                "• پایه علمی قابل قبول",
                "• توانایی حل مسائل ساده",
                "• تلاش و پشتکار"
            )
            else -> listOf(
                "• حضور در آزمون",
                "• تلاش برای پاسخ‌گویی"
            )
        }

        val weaknesses = when {
            result.score < 50 -> listOf(
                "• نیاز به مرور مطالب پایه",
                "• دقت در خواندن سوالات",
                "• مدیریت زمان",
                "• تمرین بیشتر"
            )
            result.score < 70 -> listOf(
                "• نیاز به تمرین بیشتر",
                "• افزایش دقت",
                "• مرور نکات کلیدی"
            )
            else -> listOf(
                "• مرور سوالات غلط",
                "• تمرین سوالات چالشی"
            )
        }

        // دو ستون نقاط قوت و ضعف
        val colWidth = (PAGE_WIDTH - MARGIN * 3) / 2

        // ستون نقاط قوت
        drawRect(
            canvas = canvas,
            x = MARGIN.toFloat(),
            y = y,
            width = colWidth.toFloat(),
            height = 120f,
            backgroundColor = Color.parseColor("#D5EDDA"),
            borderColor = Color.parseColor("#C3E6CB")
        )

        drawCenteredText(
            canvas = canvas,
            text = "✅ نقاط قوت",
            x = MARGIN + colWidth / 2f,
            y = y + 20,
            fontSize = NORMAL_FONT_SIZE,
            isBold = true,
            color = COLOR_SUCCESS
        )

        var tempY = y + 45
        strengths.forEach { strength ->
            drawText(
                canvas = canvas,
                text = strength,
                x = MARGIN + 20.toFloat(),
                y = tempY,
                fontSize = SMALL_FONT_SIZE
            )
            tempY += LINE_HEIGHT
        }

        // ستون نقاط ضعف
        drawRect(
            canvas = canvas,
            x = (MARGIN * 2 + colWidth).toFloat(),
            y = y,
            width = colWidth.toFloat(),
            height = 120f,
            backgroundColor = Color.parseColor("#F8D7DA"),
            borderColor = Color.parseColor("#F5C6CB")
        )

        drawCenteredText(
            canvas = canvas,
            text = "❌ نیاز به بهبود",
            x = MARGIN * 2 + colWidth + colWidth / 2f,
            y = y + 20,
            fontSize = NORMAL_FONT_SIZE,
            isBold = true,
            color = COLOR_DANGER
        )

        tempY = y + 45
        weaknesses.forEach { weakness ->
            drawText(
                canvas = canvas,
                text = weakness,
                x = MARGIN * 2 + colWidth + 20.toFloat(),
                y = tempY,
                fontSize = SMALL_FONT_SIZE
            )
            tempY += LINE_HEIGHT
        }

        y += 130

        // توصیه‌های بهبود
        y += LINE_HEIGHT
        drawCenteredText(
            canvas = canvas,
            text = "توصیه‌های بهبود",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = HEADER_FONT_SIZE,
            isBold = true
        )
        y += LINE_HEIGHT * 1.5f

        val recommendations = when {
            result.score >= 90 -> listOf(
                "• روی سوالات چالشی تمرکز کنید",
                "• سرعت خود را افزایش دهید",
                "• برای آزمون‌های پیشرفته آماده شوید"
            )
            result.score >= 70 -> listOf(
                "• نقاط ضعف را شناسایی و تمرین کنید",
                "• تست‌زنی زمان‌دار تمرین کنید",
                "• مرور هفتگی داشته باشید"
            )
            result.score >= 50 -> listOf(
                "• مطالب پایه را مرور کنید",
                "• روزانه ۱ ساعت مطالعه هدفمند",
                "• با معلم در مورد نقاط ضعف مشورت کنید"
            )
            else -> listOf(
                "• از ابتدا مطالب را مطالعه کنید",
                "• روزانه ۲ ساعت مطالعه",
                "• تمرین با نمونه سوالات ساده",
                "• جلسات رفع اشکال با معلم"
            )
        }

        recommendations.forEach { recommendation ->
            y = drawText(
                canvas = canvas,
                text = "• $recommendation",
                x = MARGIN + 20.toFloat(),
                y = y,
                fontSize = NORMAL_FONT_SIZE
            )
            y += LINE_HEIGHT
        }

        return page
    }

    /**
     * صفحه پاسخ‌های صحیح
     */
    private fun createCorrectAnswersPage(
        document: PdfDocument,
        exam: Exam,
        result: ExamResult
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN.toFloat()

        // عنوان
        drawCenteredText(
            canvas = canvas,
            text = "پاسخ‌های صحیح",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE,
            isBold = true,
            color = COLOR_INFO
        )
        y += LINE_HEIGHT * 2

        drawText(
            canvas = canvas,
            text = "مقایسه پاسخ‌های شما با پاسخ‌های صحیح:",
            x = MARGIN.toFloat(),
            y = y,
            fontSize = NORMAL_FONT_SIZE
        )
        y += LINE_HEIGHT * 1.5f

        // جدول پاسخ‌ها
        val headers = listOf("شماره", "نوع", "پاسخ شما", "پاسخ صحیح", "وضعیت")
        val colWidths = listOf(40, 60, 120, 120, 60)

        // هدر جدول
        drawTableHeader(canvas, y, headers, colWidths)
        y += 30

        exam.questions.forEachIndexed { index, question ->
            val studentAnswer = result.detailedResults?.find { it.questionId == question.id }
            val isCorrect = studentAnswer?.isCorrect == true

            // رنگ ردیف
            val rowColor = when {
                isCorrect -> Color.parseColor("#D5EDDA")
                studentAnswer == null -> Color.parseColor("#F8F9FA")
                else -> Color.parseColor("#F8D7DA")
            }

            // محتوای ردیف
            val rowData = listOf(
                convertToPersianDigits(index + 1),
                getQuestionTypeText(question.type),
                studentAnswer?.studentAnswer ?: "پاسخ داده نشد",
                question.correctAnswer ?: "-",
                if (isCorrect) "✅" else "❌"
            )

            y = drawTableRow(canvas, y, rowData, colWidths, rowColor)
            y += 2
        }

        y += LINE_HEIGHT

        // خلاصه
        drawTextBox(
            canvas = canvas,
            y = y,
            lines = listOf(
                "📊 خلاصه:",
                "• سوالات صحیح: ${convertToPersianDigits(result.correctAnswers)}",
                "• سوالات غلط: ${convertToPersianDigits(result.wrongAnswers)}",
                "• سوالات بی‌پاسخ: ${convertToPersianDigits(result.totalQuestions - result.correctAnswers - result.wrongAnswers)}",
                "• دقت: ${String.format("%.1f", (result.correctAnswers.toFloat() / result.totalQuestions) * 100)}%"
            ),
            backgroundColor = Color.parseColor("#E8F4FD")
        )

        return page
    }

    /**
     * صفحه نکات آموزشی
     */
    private fun createStudyTipsPage(
        document: PdfDocument,
        exam: Exam,
        result: ExamResult
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        var y = MARGIN.toFloat()

        // عنوان
        drawCenteredText(
            canvas = canvas,
            text = "نکات آموزشی برای بهبود",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = TITLE_FONT_SIZE,
            isBold = true,
            color = COLOR_PRIMARY
        )
        y += LINE_HEIGHT * 2

        // برنامه مطالعه پیشنهادی
        drawCenteredText(
            canvas = canvas,
            text = "📅 برنامه مطالعه هفتگی پیشنهادی",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = HEADER_FONT_SIZE,
            isBold = true
        )
        y += LINE_HEIGHT * 1.5f

        val studyPlan = when {
            result.score >= 80 -> listOf(
                "شنبه: مرور فصل‌های ۱ و ۲ (۱ ساعت)",
                "یکشنبه: حل نمونه سوالات (۱.۵ ساعت)",
                "دوشنبه: مطالعه فصل جدید (۱ ساعت)",
                "سه‌شنبه: تمرین تست‌زنی زمان‌دار (۱ ساعت)",
                "چهارشنبه: مرور نکات کلیدی (۰.۵ ساعت)",
                "پنج‌شنبه: استراحت",
                "جمعه: آزمون آزمایشی (۲ ساعت)"
            )
            result.score >= 60 -> listOf(
                "شنبه: مطالعه فصل‌های ضعیف (۱.۵ ساعت)",
                "یکشنبه: حل تمرینات کتاب (۱ ساعت)",
                "دوشنبه: مرور نکات مهم (۱ ساعت)",
                "سه‌شنبه: تست‌زنی (۱ ساعت)",
                "چهارشنبه: رفع اشکال (۱ ساعت)",
                "پنج‌شنبه: مرور کلی (۰.۵ ساعت)",
                "جمعه: آزمون آزمایشی (۱.۵ ساعت)"
            )
            else -> listOf(
                "شنبه: مطالعه از ابتدا (۲ ساعت)",
                "یکشنبه: حل مثال‌های ساده (۱.۵ ساعت)",
                "دوشنبه: تمرین با راهنمایی (۲ ساعت)",
                "سه‌شنبه: مرور مطالبی که خوانده‌اید (۱ ساعت)",
                "چهارشنبه: جلسه رفع اشکال (۱ ساعت)",
                "پنج‌شنبه: تکرار تمرینات (۱.۵ ساعت)",
                "جمعه: آزمون کوتاه (۱ ساعت)"
            )
        }

        studyPlan.forEach { dayPlan ->
            y = drawText(
                canvas = canvas,
                text = dayPlan,
                x = MARGIN + 30.toFloat(),
                y = y,
                fontSize = NORMAL_FONT_SIZE
            )
            y += LINE_HEIGHT
        }

        y += LINE_HEIGHT

        // منابع پیشنهادی
        drawCenteredText(
            canvas = canvas,
            text = "📚 منابع مطالعاتی پیشنهادی",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = HEADER_FONT_SIZE,
            isBold = true
        )
        y += LINE_HEIGHT * 1.5f

        val resources = listOf(
            "• کتاب درسی (منبع اصلی)",
            "• کتاب کار دانش‌آموز",
            "• نمونه سوالات امتحانی سال‌های قبل",
            "• فلش کارت‌های آموزشی",
            "• اپلیکیشن‌های کمک آموزشی"
        )

        resources.forEach { resource ->
            y = drawText(
                canvas = canvas,
                text = resource,
                x = MARGIN + 30.toFloat(),
                y = y,
                fontSize = NORMAL_FONT_SIZE
            )
            y += LINE_HEIGHT
        }

        y += LINE_HEIGHT

        // تکنیک‌های مطالعه
        drawCenteredText(
            canvas = canvas,
            text = "🎯 تکنیک‌های مطالعه موثر",
            x = PAGE_WIDTH / 2f,
            y = y,
            fontSize = HEADER_FONT_SIZE,
            isBold = true
        )
        y += LINE_HEIGHT * 1.5f

        val techniques = listOf(
            "• مطالعه فعال: حین مطالعه یادداشت برداری کنید",
            "• تکرار با فاصله: مطالب را در بازه‌های زمانی مرور کنید",
            "• تست‌زنی زمان‌دار: سرعت و دقت خود را افزایش دهید",
            "• خلاصه‌نویسی: نکات کلیدی را خلاصه کنید",
            "• آموزش به دیگران: بهترین روش برای تثبیت مطالب"
        )

        techniques.forEach { technique ->
            y = drawText(
                canvas = canvas,
                text = technique,
                x = MARGIN + 30.toFloat(),
                y = y,
                fontSize = NORMAL_FONT_SIZE
            )
            y += LINE_HEIGHT
        }

        // نکته پایانی
        y += LINE_HEIGHT
        drawTextBox(
            canvas = canvas,
            y = y,
            lines = listOf(
                "💡 نکته پایانی:",
                "پیشرفت نیاز به زمان و تلاش مستمر دارد.",
                "هر روز کمی بهتر از دیروز باشید.",
                "موفقیت شما آرزوی ماست! 🌟"
            ),
            backgroundColor = Color.parseColor("#FFF3CD")
        )

        return page
    }

    // ==================== توابع کمکی ====================

    private fun drawQuestion(canvas: Canvas, question: Question, number: Int, y: Float): Float {
        var currentY = y

        // شماره و متن سوال
        val questionText = "${convertToPersianDigits(number)}. ${question.text}"
        currentY = drawWrappedText(
            canvas = canvas,
            text = questionText,
            x = MARGIN.toFloat(),
            y = currentY,
            width = PAGE_WIDTH - MARGIN * 2,
            fontSize = NORMAL_FONT_SIZE,
            isBold = true
        )

        currentY += LINE_HEIGHT

        // گزینه‌ها (اگر تستی باشد)
        if (question.type == "mcq") {
            question.options.forEachIndexed { index, option ->
                val optionText = "${('الف' + index)}. $option"
                currentY = drawWrappedText(
                    canvas = canvas,
                    text = optionText,
                    x = MARGIN + 30.toFloat(),
                    y = currentY,
                    width = PAGE_WIDTH - MARGIN * 2 - 30,
                    fontSize = NORMAL_FONT_SIZE
                )
                currentY += LINE_HEIGHT
            }
        }

        // نمره سوال
        drawText(
            canvas = canvas,
            text = "نمره: ${convertToPersianDigits(getQuestionScore(question.type))}",
            x = PAGE_WIDTH - MARGIN - 50.toFloat(),
            y = y + 5,
            fontSize = SMALL_FONT_SIZE,
            color = Color.GRAY
        )

        return currentY
    }

    private fun drawPageHeader(canvas: Canvas, title: String) {
        drawCenteredText(
            canvas = canvas,
            text = title,
            x = PAGE_WIDTH / 2f,
            y = MARGIN.toFloat(),
            fontSize = HEADER_FONT_SIZE,
            isBold = true
        )

        drawHorizontalLine(canvas, MARGIN + 30f, PAGE_WIDTH - MARGIN * 2)
    }

    private fun drawSimpleBarChart(canvas: Canvas, y: Float, correct: Int, wrong: Int, total: Int): Float {
        var currentY = y
        val chartWidth = PAGE_WIDTH - MARGIN * 2
        val barHeight = 20f

        // محاسبه درصدها
        val correctPercent = correct.toFloat() / total * 100
        val wrongPercent = wrong.toFloat() / total * 100
        val unansweredPercent = (total - correct - wrong).toFloat() / total * 100

        // رسم نمودار
        var xPos = MARGIN.toFloat()

        // بخش صحیح
        val correctWidth = chartWidth * correctPercent / 100
        drawRect(
            canvas = canvas,
            x = xPos,
            y = currentY,
            width = correctWidth,
            height = barHeight,
            backgroundColor = COLOR_SUCCESS
        )
        xPos += correctWidth

        // بخش غلط
        val wrongWidth = chartWidth * wrongPercent / 100
        drawRect(
            canvas = canvas,
            x = xPos,
            y = currentY,
            width = wrongWidth,
            height = barHeight,
            backgroundColor = COLOR_DANGER
        )
        xPos += wrongWidth

        // بخش بی‌پاسخ
        val unansweredWidth = chartWidth * unansweredPercent / 100
        drawRect(
            canvas = canvas,
            x = xPos,
            y = currentY,
            width = unansweredWidth,
            height = barHeight,
            backgroundColor = Color.LTGRAY
        )

        currentY += barHeight + 10

        // راهنمای رنگ‌ها
        val legendItems = listOf(
            Pair("صحیح (${convertToPersianDigits(correct)})", COLOR_SUCCESS),
            Pair("غلط (${convertToPersianDigits(wrong)})", COLOR_DANGER),
            Pair("بی‌پاسخ (${convertToPersianDigits(total - correct - wrong)})", Color.GRAY)
        )

        val legendWidth = 100
        var legendX = (PAGE_WIDTH - legendItems.size * legendWidth) / 2

        legendItems.forEach { (text, color) ->
            // مربع رنگ
            drawRect(
                canvas = canvas,
                x = legendX.toFloat(),
                y = currentY,
                width = 15f,
                height = 15f,
                backgroundColor = color
            )

            // متن
            drawText(
                canvas = canvas,
                text = text,
                x = legendX + 20.toFloat(),
                y = currentY + 12,
                fontSize = VERY_SMALL_FONT_SIZE
            )

            legendX += legendWidth
        }

        return currentY + 30
    }

    private fun drawTableHeader(canvas: Canvas, y: Float, headers: List<String>, colWidths: List<Int>) {
        var xPos = MARGIN

        // پس‌زمینه هدر
        drawRect(
            canvas = canvas,
            x = MARGIN.toFloat(),
            y = y,
            width = colWidths.sum().toFloat(),
            height = 25f,
            backgroundColor = COLOR_PRIMARY
        )

        // متن هدرها
        headers.forEachIndexed { index, header ->
            drawCenteredText(
                canvas = canvas,
                text = header,
                x = xPos + colWidths[index] / 2f,
                y = y + 18,
                fontSize = SMALL_FONT_SIZE,
                isBold = true,
                color = Color.WHITE
            )
            xPos += colWidths[index]
        }
    }

    private fun drawTableRow(
        canvas: Canvas,
        y: Float,
        rowData: List<String>,
        colWidths: List<Int>,
        backgroundColor: Int = Color.WHITE
    ): Float {
        var xPos = MARGIN

        // پس‌زمینه ردیف
        drawRect(
            canvas = canvas,
            x = MARGIN.toFloat(),
            y = y,
            width = colWidths.sum().toFloat(),
            height = 20f,
            backgroundColor = backgroundColor,
            borderColor = Color.LTGRAY
        )

        // محتوای ردیف
        rowData.forEachIndexed { index, cell ->
            // برای ستون‌های متن بلند، متن را کوتاه کن
            val displayText = if (cell.length > 20 && index >= 2) "${cell.take(20)}..." else cell

            drawCenteredText(
                canvas = canvas,
                text = displayText,
                x = xPos + colWidths[index] / 2f,
                y = y + 15,
                fontSize = VERY_SMALL_FONT_SIZE,
                color = if (index == 4) {
                    when (cell) {
                        "✅" -> COLOR_SUCCESS
                        "❌" -> COLOR_DANGER
                        else -> Color.BLACK
                    }
                } else {
                    Color.BLACK
                }
            )
            xPos += colWidths[index]
        }

        return y + 20
    }

    private fun drawInfoBox(canvas: Canvas, y: Float, lines: List<String>, backgroundColor: Int = Color.parseColor("#F8F9FA")): Float {
        var currentY = y

        // محاسبه ارتفاع مورد نیاز
        val lineHeight = LINE_HEIGHT * 0.8f
        val padding = 20
        val boxHeight = lines.size * lineHeight + padding * 2

        // رسم کادر
        drawRect(
            canvas = canvas,
            x = MARGIN.toFloat(),
            y = currentY,
            width = (PAGE_WIDTH - MARGIN * 2).toFloat(),
            height = boxHeight,
            backgroundColor = backgroundColor,
            borderColor = Color.LTGRAY
        )

        currentY += padding.toFloat()

        // متن‌ها
        lines.forEach { line ->
            drawText(
                canvas = canvas,
                text = line,
                x = MARGIN + 20.toFloat(),
                y = currentY,
                fontSize = NORMAL_FONT_SIZE
            )
            currentY += lineHeight
        }

        return y + boxHeight
    }

    private fun drawTextBox(
        canvas: Canvas,
        y: Float,
        lines: List<String>,
        backgroundColor: Int = Color.WHITE,
        borderColor: Int = Color.LTGRAY
    ): Float {
        var currentY = y

        // محاسبه ارتفاع مورد نیاز
        val maxLineWidth = PAGE_WIDTH - MARGIN * 2 - 40
        var totalHeight = 0

        lines.forEach { line ->
            val paint = TextPaint().apply {
                textSize = NORMAL_FONT_SIZE
                typeface = Typeface.DEFAULT
            }

            val layout = StaticLayout.Builder.obtain(line, 0, line.length, paint, maxLineWidth)
                .build()

            totalHeight += layout.height + 5
        }

        val boxHeight = totalHeight + 30

        // رسم کادر
        drawRect(
            canvas = canvas,
            x = MARGIN.toFloat(),
            y = currentY,
            width = (PAGE_WIDTH - MARGIN * 2).toFloat(),
            height = boxHeight.toFloat(),
            backgroundColor = backgroundColor,
            borderColor = borderColor
        )

        currentY += 20

        // متن‌ها
        lines.forEach { line ->
            currentY = drawWrappedText(
                canvas = canvas,
                text = line,
                x = MARGIN + 20.toFloat(),
                y = currentY,
                width = maxLineWidth,
                fontSize = NORMAL_FONT_SIZE
            )
            currentY += 5
        }

        return y + boxHeight
    }

    // ==================== توابع رسم پایه ====================

    private fun drawText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        fontSize: Float = NORMAL_FONT_SIZE,
        isBold: Boolean = false,
        color: Int = Color.BLACK
    ): Float {
        val paint = Paint().apply {
            this.color = color
            this.textSize = fontSize
            this.typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.isAntiAlias = true
            this.textAlign = Paint.Align.LEFT
        }

        canvas.drawText(text, x, y, paint)
        return y + fontSize
    }

    private fun drawCenteredText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        fontSize: Float = NORMAL_FONT_SIZE,
        isBold: Boolean = false,
        color: Int = Color.BLACK
    ): Float {
        val paint = Paint().apply {
            this.color = color
            this.textSize = fontSize
            this.typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.isAntiAlias = true
            this.textAlign = Paint.Align.CENTER
        }

        canvas.drawText(text, x, y, paint)
        return y + fontSize
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        width: Int,
        fontSize: Float = NORMAL_FONT_SIZE,
        isBold: Boolean = false,
        color: Int = Color.BLACK
    ): Float {
        val paint = TextPaint().apply {
            this.color = color
            this.textSize = fontSize
            this.typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            this.isAntiAlias = true
        }

        val staticLayout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)
            .build()

        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()

        return y + staticLayout.height
    }

    private fun drawRect(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        backgroundColor: Int = Color.WHITE,
        borderColor: Int? = null
    ) {
        // پس‌زمینه
        canvas.drawRect(x, y, x + width, y + height, Paint().apply {
            color = backgroundColor
            style = Paint.Style.FILL
        })

        // حاشیه (اگر خواسته شده)
        borderColor?.let {
            canvas.drawRect(x, y, x + width, y + height, Paint().apply {
                color = it
                style = Paint.Style.STROKE
                strokeWidth = 1f
            })
        }
    }

    private fun drawHorizontalLine(
        canvas: Canvas,
        y: Float,
        length: Float = (PAGE_WIDTH - MARGIN * 2).toFloat(),
        isDashed: Boolean = false
    ) {
        val paint = Paint().apply {
            color = Color.GRAY
            strokeWidth = 1f
            if (isDashed) {
                pathEffect = android.graphics.DashPathEffect(floatArrayOf(5f, 5f), 0f)
            }
        }

        canvas.drawLine(MARGIN.toFloat(), y, MARGIN + length, y, paint)
    }

    // ==================== توابع کمکی متنی ====================

    private fun convertToPersianDigits(number: Int): String {
        return number.toString().map {
            PERSIAN_DIGITS[it.toString().toInt()]
        }.joinToString("")
    }

    private fun getPersianDate(): String {
        val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale("fa", "IR"))
        return dateFormat.format(Date())
    }

    private fun getQuestionTypeText(type: String): String {
        return when (type) {
            "mcq" -> "تستی"
            "short_answer" -> "کوتاه"
            "essay" -> "تشریحی"
            "fill_blank" -> "جای خالی"
            else -> "سایر"
        }
    }

    private fun getQuestionScore(type: String): Int {
        return when (type) {
            "mcq" -> 3
            "short_answer" -> 4
            "essay" -> 5
            "fill_blank" -> 2
            else -> 1
        }
    }

    private fun getGradeText(score: Float): String {
        return when {
            score >= 90 -> "عالی"
            score >= 80 -> "خیلی خوب"
            score >= 70 -> "خوب"
            score >= 60 -> "قابل قبول"
            score >= 50 -> "نیاز به تلاش"
            else -> "نیاز به توجه ویژه"
        }
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return "${convertToPersianDigits(minutes.toInt())}:${convertToPersianDigits(remainingSeconds.toInt())}"
    }

    private fun generateDetailedFeedback(result: ExamResult): List<String> {
        val feedback = mutableListOf<String>()

        feedback.add("📊 تحلیل عملکرد شما در این آزمون:")
        feedback.add("")

        when {
            result.score >= 90 -> {
                feedback.add("🌟 عملکرد درخشان!")
                feedback.add("شما تسلط کاملی بر مطالب دارید.")
                feedback.add("سرعت و دقت شما در پاسخ‌گویی عالی است.")
                feedback.add("می‌توانید روی سوالات چالشی تمرکز کنید.")
            }
            result.score >= 80 -> {
                feedback.add("👍 عملکرد بسیار خوب")
                feedback.add("درک عمیقی از مطالب دارید.")
                feedback.add("نقاط قوت شما قابل توجه است.")
                feedback.add("با تمرین بیشتر می‌توانید به سطح عالی برسید.")
            }
            result.score >= 70 -> {
                feedback.add("💪 عملکرد خوب")
                feedback.add("پایه علمی شما قابل قبول است.")
                feedback.add("نیاز به تمرین بیشتر در برخی مباحث دارید.")
                feedback.add("با برنامه‌ریزی مناسب پیشرفت خواهید کرد.")
            }
            result.score >= 60 -> {
                feedback.add("📚 نیاز به تمرین")
                feedback.add("درک کلی از مطالب دارید.")
                feedback.add("نیاز به مرور و تمرین بیشتر دارید.")
                feedback.add("روزانه حداقل ۱ ساعت مطالعه هدفمند داشته باشید.")
            }
            result.score >= 50 -> {
                feedback.add("🔍 نیاز به توجه")
                feedback.add("پایه شما نیاز به تقویت دارد.")
                feedback.add("مطالب را از ابتدا مرور کنید.")
                feedback.add("با معلم خود در مورد نقاط ضعف مشورت کنید.")
            }
            else -> {
                feedback.add("🔄 نیاز به بازنگری اساسی")
                feedback.add("پایه علمی شما ضعیف است.")
                feedback.add("نیاز به مطالعه از ابتدا دارید.")
                feedback.add("روزانه ۲ ساعت مطالعه با برنامه‌ریزی دقیق داشته باشید.")
            }
        }

        feedback.add("")
        feedback.add("✅ نقاط قوت: ${result.correctAnswers} سوال صحیح")
        feedback.add("❌ نقاط ضعف: ${result.wrongAnswers} سوال غلط")
        if (result.totalQuestions - result.correctAnswers - result.wrongAnswers > 0) {
            feedback.add("⏰ نیاز به مدیریت زمان: ${result.totalQuestions - result.correctAnswers - result.wrongAnswers} سوال بی‌پاسخ")
        }

        return feedback
    }

    private fun saveDocument(document: PdfDocument, baseName: String): File {
        val fileName = "${baseName}_${System.currentTimeMillis()}.pdf"
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val examsDir = File(downloadsDir, "ExamApp")

        if (!examsDir.exists()) {
            examsDir.mkdirs()
        }

        val file = File(examsDir, fileName)

        FileOutputStream(file).use { fos ->
            document.writeTo(fos)
        }

        document.close()
        return file
    }
}