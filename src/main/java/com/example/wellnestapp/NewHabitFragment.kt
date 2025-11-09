package com.example.wellnestapp

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import com.example.wellnestapp.model.HabitGoal
import java.util.*

class NewHabitFragment : Fragment() {
    private var selectedColor: String = "#388E3C"
    private var reminderTimeMillis: Long = 0L
    private var editingHabit: HabitGoal? = null

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingHabit = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arguments?.getParcelable("habit", HabitGoal::class.java)
        } else {
            arguments?.getParcelable("habit")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_habit, container, false)

        val habitName = view.findViewById<EditText>(R.id.input_habit)
        val habitGoal = view.findViewById<EditText>(R.id.input_goal)
        val habitAmount = view.findViewById<EditText>(R.id.input_target_value)
        val habitUnit = view.findViewById<Spinner>(R.id.input_target_unit)
        val reminderTime = view.findViewById<EditText>(R.id.input_time)
        val timeIcon = view.findViewById<ImageView>(R.id.time_picker_icon)
        val confirm = view.findViewById<ImageView>(R.id.confirm)
        val cancel = view.findViewById<ImageView>(R.id.cancel)

        val colors = mapOf(
            R.id.color_blue to "#1976D2",
            R.id.color_green to "#388E3C",
            R.id.color_pink to "#E91E63",
            R.id.color_yellow to "#FFC107",
            R.id.color_orange to "#FF5722",
            R.id.color_purple to "#9C27B0"
        )

        fun selectColor(viewId: Int, hex: String) {
            selectedColor = hex
            colors.forEach { (vId, colorHex) ->
                val v = view.findViewById<View>(vId)
                ViewCompat.setElevation(v, if (vId == viewId) 7f else 0f)
                v.background = null
                v.setBackgroundColor(Color.parseColor(colorHex))
                if (vId == viewId) {
                    v.background = resources.getDrawable(R.drawable.selected_color_border, null)
                }
            }
        }
        colors.forEach { (vId, hex) ->
            view.findViewById<View>(vId).setOnClickListener { selectColor(vId, hex) }
        }
        selectColor(R.id.color_blue, "#1976D2")

        val pickTimeListener = View.OnClickListener {
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val minute = cal.get(Calendar.MINUTE)
            TimePickerDialog(requireContext(), { _, h, m ->
                reminderTime.setText(String.format("%02d:%02d", h, m))
                val today = Calendar.getInstance()
                today.set(Calendar.HOUR_OF_DAY, h)
                today.set(Calendar.MINUTE, m)
                today.set(Calendar.SECOND, 0)
                today.set(Calendar.MILLISECOND, 0)
                reminderTimeMillis = today.timeInMillis
            }, hour, minute, true).show()
        }
        reminderTime.setOnClickListener(pickTimeListener)
        timeIcon.setOnClickListener(pickTimeListener)

        // --- If editing, pre-fill fields with data ---
        editingHabit?.let { habit ->
            habitName.setText(habit.title)
            habitGoal.setText(habit.goal)
            habitAmount.setText(habit.amount)
            // Set spinner selection by value:
            val unitAdapter = habitUnit.adapter
            for (i in 0 until unitAdapter.count) {
                if (unitAdapter.getItem(i).toString() == habit.unit) {
                    habitUnit.setSelection(i)
                    break
                }
            }
            val selectedColorId = colors.filterValues { it == habit.colorHex }.keys.firstOrNull() ?: R.id.color_blue
            selectColor(selectedColorId, habit.colorHex)
            val cal = Calendar.getInstance()
            cal.timeInMillis = habit.reminderTimeMillis
            reminderTime.setText(String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)))
            reminderTimeMillis = habit.reminderTimeMillis
        }

        confirm.setOnClickListener {
            val nameStr = habitName.text.toString().trim()
            val goalStr = habitGoal.text.toString().trim()
            val amountStr = habitAmount.text.toString().trim()
            val unitStr = habitUnit.selectedItem?.toString() ?: ""

            if (nameStr.isEmpty() || goalStr.isEmpty() || amountStr.isEmpty() || unitStr.isEmpty() || reminderTimeMillis == 0L) {
                Toast.makeText(requireContext(), "Please fill all details and select a reminder time", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val habit = HabitGoal(
                title = nameStr,
                goal = goalStr,
                amount = amountStr,
                unit = unitStr,
                colorHex = selectedColor,
                reminderTimeMillis = reminderTimeMillis,
                createdTime = editingHabit?.createdTime ?: System.currentTimeMillis(),
                done = editingHabit?.done ?: false
            )

            val allHabits: List<HabitGoal> = HabitStorage.getHabits(requireContext())
            val mutableHabits: MutableList<HabitGoal> = allHabits.toMutableList()
            if (editingHabit != null) {
                val index = mutableHabits.indexOfFirst { it.createdTime == editingHabit!!.createdTime }
                if (index != -1) {
                    mutableHabits[index] = habit
                }
            } else {
                mutableHabits.add(habit)
            }
            HabitStorage.saveHabits(requireContext(), mutableHabits)

            // --- Schedule notification for the habit ---
            scheduleHabitReminder(requireContext(), habit.createdTime.toInt(), habit.reminderTimeMillis, habit.title)

            Toast.makeText(requireContext(), if (editingHabit != null) "Habit updated" else "Habit saved", Toast.LENGTH_SHORT).show()
            requireActivity().supportFragmentManager.popBackStack()
        }

        cancel.setOnClickListener { requireActivity().supportFragmentManager.popBackStack() }
        return view
    }

    companion object {
        fun forEdit(habit: HabitGoal): NewHabitFragment {
            val frag = NewHabitFragment()
            val bundle = Bundle()
            bundle.putParcelable("habit", habit)
            frag.arguments = bundle
            return frag
        }
    }

    // --- ADD THIS Below: Schedules notification with AlarmManager ---
    private fun scheduleHabitReminder(context: Context, habitId: Int, reminderTimeMillis: Long, habitName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("habitId", habitId)
            putExtra("habitName", habitName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId, // Unique request code per habit
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            reminderTimeMillis,
            pendingIntent
        )
    }
}
