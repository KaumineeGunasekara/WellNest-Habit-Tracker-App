package com.example.wellnestapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import java.util.*
import android.graphics.Color


class ReportFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val spinner = view.findViewById<Spinner>(R.id.report_filter_spinner)
        val progressBar = view.findViewById<ProgressBar>(R.id.report_progress_bar)
        val progressText = view.findViewById<TextView>(R.id.report_progress_text)
        val summaryLabel = view.findViewById<TextView>(R.id.report_summary_label)
        val breakdownContainer = view.findViewById<LinearLayout>(R.id.report_breakdown_container)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, v: View?, pos: Int, id: Long) {
                updateReport(
                    spinner.selectedItemPosition,
                    progressBar, progressText, summaryLabel, breakdownContainer
                )
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        updateReport(
            spinner.selectedItemPosition,
            progressBar, progressText, summaryLabel, breakdownContainer
        )
    }

    private fun updateReport(
        filterType: Int, // 0 = Day, 1 = Week, 2 = Month
        progressBar: ProgressBar,
        progressText: TextView,
        summaryLabel: TextView,
        breakdownContainer: LinearLayout
    ) {
        val allHabits = HabitStorage.getHabits(requireContext())
        val now = Calendar.getInstance()

        // Delete habits older than a month (autodelete policy)
        val firstDayOfThisMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Remove habits created before start of this month
        val cleanHabits = allHabits.filter { it.createdTime >= firstDayOfThisMonth.timeInMillis }
        if (cleanHabits.size != allHabits.size) HabitStorage.saveHabits(requireContext(), cleanHabits)

        val displayHabits = cleanHabits

        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val startToday = today.timeInMillis
        today.set(Calendar.HOUR_OF_DAY, 23)
        today.set(Calendar.MINUTE, 59)
        today.set(Calendar.SECOND, 59)
        today.set(Calendar.MILLISECOND, 999)
        val endToday = today.timeInMillis

        val periods = mutableListOf<ReportPeriod>() // Will fill depending on filter

        if (filterType == 0) { // Day
            // Report for today; breakdown for each day in current week
            val weekCal = Calendar.getInstance()
            val todayOfWeek = weekCal.get(Calendar.DAY_OF_WEEK)
            val offset = if (todayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - todayOfWeek
            weekCal.add(Calendar.DAY_OF_MONTH, offset)
            for (i in 0..6) {
                val start = weekCal.clone() as Calendar
                start.set(Calendar.HOUR_OF_DAY, 0)
                start.set(Calendar.MINUTE, 0)
                start.set(Calendar.SECOND, 0)
                start.set(Calendar.MILLISECOND, 0)
                val end = start.clone() as Calendar
                end.set(Calendar.HOUR_OF_DAY, 23)
                end.set(Calendar.MINUTE, 59)
                end.set(Calendar.SECOND, 59)
                end.set(Calendar.MILLISECOND, 999)
                val habits = displayHabits.filter { it.createdTime in start.timeInMillis..end.timeInMillis }
                periods.add(
                    ReportPeriod(
                        android.text.format.DateFormat.format("EEE, MMM dd", start).toString(),
                        habits.count { it.done },
                        habits.size
                    )
                )
                weekCal.add(Calendar.DAY_OF_MONTH, 1)
            }
            summaryLabel.text = "Today's Progress"
        }
        else if (filterType == 1) { // Week
            // Current week report; breakdown by week for current month
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            for (week in 0..4) {
                val weekStart = cal.clone() as Calendar
                weekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                weekStart.add(Calendar.WEEK_OF_MONTH, week)
                val weekEnd = weekStart.clone() as Calendar
                weekEnd.add(Calendar.DAY_OF_MONTH, 6)

                val habitsThisWeek = displayHabits.filter {
                    it.createdTime in weekStart.timeInMillis..weekEnd.timeInMillis
                }
                // Only add if there are habits this week
                if (habitsThisWeek.isNotEmpty()) {
                    periods.add(
                        ReportPeriod(
                            "Week ${week + 1}",
                            habitsThisWeek.count { it.done },
                            habitsThisWeek.size
                        )
                    )
                }
            }
            summaryLabel.text = "This Week's Progress"
        }
        else { // Month
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val monthTitle = android.text.format.DateFormat.format("MMMM yyyy", cal).toString()
            val habitsThisMonth = displayHabits.filter { it.createdTime >= cal.timeInMillis }
            periods.add(
                ReportPeriod(
                    monthTitle,
                    habitsThisMonth.count { it.done },
                    habitsThisMonth.size
                )
            )
            summaryLabel.text = "This Month's Progress"
        }

        // Overall summary progress bar
        val doneSum = periods.sumOf { it.done }
        val totalSum = periods.sumOf { it.total }
        val percent = if (totalSum > 0) (doneSum * 100 / totalSum) else 0
        progressBar.max = 100
        progressBar.progress = percent
        progressText.text = "$doneSum/$totalSum completed"

        // Breakdown display
        breakdownContainer.removeAllViews()
        // Only show past-periods (today=all, week=week-by-week, month=just current month one)
        for (rep in periods) {
            val label = rep.label
            val row = TextView(requireContext())
            row.text = "${label}: ${rep.done}/${rep.total} done"
            row.textSize = 15f
            row.setPadding(10, 16, 10, 16)
            row.setTextColor(if (rep.done == rep.total && rep.total != 0) Color.parseColor("#27496D") else Color.DKGRAY)
            breakdownContainer.addView(row)
        }
    }

    // Helper data class for report periods display
    data class ReportPeriod(val label: String, val done: Int, val total: Int)
}
