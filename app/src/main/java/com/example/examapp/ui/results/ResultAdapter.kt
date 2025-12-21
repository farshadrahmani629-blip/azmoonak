// app/src/main/java/com/examapp/ui/results/ResultAdapter.kt
package com.examapp.ui.results

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.examapp.R
import com.examapp.data.models.Result
import java.text.SimpleDateFormat
import java.util.*

class ResultAdapter(
    private val onItemClick: (Result) -> Unit,
    private val onDeleteClick: (Result) -> Unit
) : ListAdapter<Result, ResultAdapter.ResultViewHolder>(ResultDiffCallback()) {

    class ResultViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvExamTitle: TextView = itemView.findViewById(R.id.tvExamTitle)
        val tvScore: TextView = itemView.findViewById(R.id.tvScore)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvCorrectAnswers: TextView = itemView.findViewById(R.id.tvCorrectAnswers)
        val btnDelete: View = itemView.findViewById(R.id.btnDelete)
        val rootView: View = itemView.findViewById(R.id.rootView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_result, parent, false)
        return ResultViewHolder(view)
    }

    override fun onBindViewHolder(holder: ResultViewHolder, position: Int) {
        val result = getItem(position)

        holder.tvExamTitle.text = result.examTitle ?: "آزمون بدون عنوان"
        holder.tvScore.text = String.format("%.1f%%", result.score)
        holder.tvCorrectAnswers.text = "${result.correctAnswers} از ${result.totalQuestions}"

        // فرمت تاریخ
        val dateFormat = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale("fa", "IR"))
        holder.tvDate.text = dateFormat.format(Date(result.date))

        // رنگ‌بندی بر اساس نمره
        val context = holder.itemView.context
        when {
            result.score >= 90 -> {
                holder.tvScore.setTextColor(ContextCompat.getColor(context, R.color.green))
                holder.rootView.setBackgroundResource(R.drawable.item_result_excellent)
            }
            result.score >= 75 -> {
                holder.tvScore.setTextColor(ContextCompat.getColor(context, R.color.blue))
                holder.rootView.setBackgroundResource(R.drawable.item_result_good)
            }
            result.score >= 50 -> {
                holder.tvScore.setTextColor(ContextCompat.getColor(context, R.color.orange))
                holder.rootView.setBackgroundResource(R.drawable.item_result_average)
            }
            else -> {
                holder.tvScore.setTextColor(ContextCompat.getColor(context, R.color.red))
                holder.rootView.setBackgroundResource(R.drawable.item_result_poor)
            }
        }

        // کلیک روی آیتم
        holder.rootView.setOnClickListener {
            onItemClick(result)
        }

        // کلیک روی دکمه حذف
        holder.btnDelete.setOnClickListener {
            onDeleteClick(result)
        }
    }
}

class ResultDiffCallback : DiffUtil.ItemCallback<Result>() {
    override fun areItemsTheSame(oldItem: Result, newItem: Result): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Result, newItem: Result): Boolean {
        return oldItem == newItem
    }
}