package com.example.wellnestapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.TextView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.example.wellnestapp.model.HabitGoal
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment() {

    private lateinit var monthYearText: TextView
    private lateinit var completedLabel: TextView
    private lateinit var completedList: LinearLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val layout = inflater.inflate(R.layout.fragment_calendar, container, false)
        val calendarView = layout.findViewById<CalendarView>(R.id.calendarView)
        monthYearText = layout.findViewById(R.id.monthYearLabel)
        completedLabel = layout.findViewById(R.id.completed_habits_label)
        completedList = layout.findViewById(R.id.completed_habits_list)

        updateMonthYear(calendarView.date)
        showCompletedHabitsForDate(calendarView.date)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            updateMonthYear(cal.timeInMillis)
            showCompletedHabitsForDate(cal.timeInMillis)
        }

        return layout
    }

    private fun updateMonthYear(dateMillis: Long) {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthYearText.text = sdf.format(Date(dateMillis))
    }

    private fun showCompletedHabitsForDate(dateMillis: Long) {
        val startCal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = startCal.clone() as Calendar
        endCal.set(Calendar.HOUR_OF_DAY, 23)
        endCal.set(Calendar.MINUTE, 59)
        endCal.set(Calendar.SECOND, 59)
        endCal.set(Calendar.MILLISECOND, 999)

        val habits = HabitStorage.getHabits(requireContext())
        val completedNames = habits.filter { habit: HabitGoal ->
            habit.done && habit.createdTime in startCal.timeInMillis..endCal.timeInMillis
        }.map { it.title }

        completedList.removeAllViews()
        if (completedNames.isEmpty()) {
            completedLabel.visibility = View.GONE
        } else {
            completedLabel.visibility = View.VISIBLE
            completedNames.forEach { name ->
                val nameView = TextView(requireContext()).apply {
                    text = name
                    textSize = 15f
                    setPadding(6, 6, 6, 6)
                }
                completedList.addView(nameView)
            }
        }
    }
}
