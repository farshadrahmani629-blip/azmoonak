// app/src/main/java/com/examapp/ui/results/ResultChart.kt
package com.examapp.ui.results

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.examapp.R
import kotlin.math.max
import kotlin.math.min

class ResultChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var chartData: List<Pair<String, Float>> = emptyList()
    private val pointRadius = 12f
    private val padding = 50f
    private val textSize = 32f

    // رنگ‌ها
    private val lineColor = Color.parseColor("#6200EE")
    private val pointColor = Color.parseColor("#FF4081")
    private val gridColor = Color.parseColor("#E0E0E0")
    private val textColor = Color.parseColor("#757575")
    private val fillColor = Color.parseColor("#1A6200EE")

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = this@ResultChart.textSize
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = fillColor
    }

    fun setData(data: List<Pair<String, Float>>) {
        chartData = data
        invalidate() // باعث می‌شود view دوباره draw شود
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (chartData.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        drawGrid(canvas)
        drawChart(canvas)
        drawLabels(canvas)
    }

    private fun drawEmptyState(canvas: Canvas) {
        val message = "داده‌ای برای نمایش وجود ندارد"
        val x = width / 2f
        val y = height / 2f

        textPaint.color = Color.GRAY
        canvas.drawText(message, x, y, textPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        paint.color = gridColor
        paint.strokeWidth = 1f

        // خطوط عمودی شبکه
        val verticalSpacing = (width - 2 * padding) / 6
        for (i in 0..6) {
            val x = padding + i * verticalSpacing
            canvas.drawLine(x, padding, x, height - padding, paint)
        }

        // خطوط افقی شبکه
        val horizontalSpacing = (height - 2 * padding) / 5
        for (i in 0..5) {
            val y = padding + i * horizontalSpacing
            canvas.drawLine(padding, y, width - padding, y, paint)
        }

        // محورها
        paint.color = Color.BLACK
        paint.strokeWidth = 2f
        canvas.drawLine(padding, height - padding, width - padding, height - padding, paint) // محور X
        canvas.drawLine(padding, padding, padding, height - padding, paint) // محور Y
    }

    private fun drawChart(canvas: Canvas) {
        if (chartData.size < 2) return

        val maxScore = chartData.maxOfOrNull { it.second } ?: 100f
        val minScore = chartData.minOfOrNull { it.second } ?: 0f
        val scoreRange = max(maxScore - minScore, 10f) // حداقل range = 10

        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        val xStep = chartWidth / (chartData.size - 1)

        // ایجاد path برای خط نمودار
        val linePath = Path()
        val fillPath = Path()

        // نقاط نمودار
        val points = mutableListOf<PointF>()

        for ((index, dataPoint) in chartData.withIndex()) {
            val x = padding + index * xStep
            val normalizedScore = (dataPoint.second - minScore) / scoreRange
            val y = height - padding - (normalizedScore * chartHeight)

            points.add(PointF(x, y))

            if (index == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, height - padding)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // بستن path برای fill
        if (points.isNotEmpty()) {
            fillPath.lineTo(points.last().x, height - padding)
            fillPath.close()
        }

        // رسم area زیر نمودار
        canvas.drawPath(fillPath, fillPaint)

        // رسم خط نمودار
        paint.color = lineColor
        paint.strokeWidth = 4f
        paint.style = Paint.Style.STROKE
        canvas.drawPath(linePath, paint)

        // رسم نقاط
        pointPaint.color = pointColor
        for (point in points) {
            canvas.drawCircle(point.x, point.y, pointRadius, pointPaint)

            // حلقه دور نقطه
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 2f
            canvas.drawCircle(point.x, point.y, pointRadius, paint)
        }

        // رسم مقادیر روی نقاط
        textPaint.color = Color.BLACK
        textPaint.textSize = 28f
        for ((index, point) in points.withIndex()) {
            val score = chartData[index].second
            val scoreText = String.format("%.0f", score)
            canvas.drawText(scoreText, point.x, point.y - pointRadius - 10, textPaint)
        }
    }

    private fun drawLabels(canvas: Canvas) {
        if (chartData.isEmpty()) return

        textPaint.color = textColor
        textPaint.textSize = 30f

        val chartWidth = width - 2 * padding
        val xStep = chartWidth / (chartData.size - 1)

        // برچسب‌های محور X
        for ((index, dataPoint) in chartData.withIndex()) {
            val x = padding + index * xStep
            val label = dataPoint.first

            // چرخش متن برای برچسب‌های فارسی
            canvas.save()
            canvas.translate(x, height - padding + 40)
            canvas.rotate(-45f)
            canvas.drawText(label, 0f, 0f, textPaint)
            canvas.restore()
        }

        // برچسب‌های محور Y
        val maxScore = chartData.maxOfOrNull { it.second } ?: 100f
        val minScore = chartData.minOfOrNull { it.second } ?: 0f
        val scoreRange = max(maxScore - minScore, 10f)

        val chartHeight = height - 2 * padding
        val yStep = chartHeight / 5

        for (i in 0..5) {
            val y = height - padding - i * yStep
            val scoreValue = minScore + (i * scoreRange / 5)
            val scoreText = String.format("%.0f", scoreValue)

            // رسم مقدار
            canvas.drawText(scoreText, padding - 25, y + 10, textPaint)

            // رسم واحد (درصد)
            if (i == 0) {
                canvas.drawText("%", padding - 25, y - 15, textPaint.apply { textSize = 24f })
            }
        }

        // عنوان محورها
        textPaint.textSize = 36f
        textPaint.textAlign = Paint.Align.CENTER

        // عنوان محور X (در پایین)
        canvas.drawText("تاریخ آزمون", width / 2f, height - 10f, textPaint)

        // عنوان محور Y (چرخش ۹۰ درجه)
        canvas.save()
        canvas.rotate(-90f)
        canvas.drawText("نمره (درصد)", -height / 2f, padding - 80f, textPaint)
        canvas.restore()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = 600
        val desiredHeight = 400

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> min(desiredWidth, widthSize)
            else -> desiredWidth
        }

        val height = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> min(desiredHeight, heightSize)
            else -> desiredHeight
        }

        setMeasuredDimension(width, height)
    }
}