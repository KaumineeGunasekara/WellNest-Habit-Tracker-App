package com.example.wellnestapp

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HomeFragment : Fragment(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var lastShakeTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        setTopDateLabel(view)
        setCurrentWeekDates(view)

        view.findViewById<Button>(R.id.add_habit_button).setOnClickListener {
            (activity as? HomePage)?.replaceFragment(NewHabitFragment())
        }

        // Load ALL habits from storage (for reporting)
        val allHabits = HabitStorage.getHabits(requireContext())

        // Show ONLY habits created today
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = todayCal.timeInMillis

        todayCal.set(Calendar.HOUR_OF_DAY, 23)
        todayCal.set(Calendar.MINUTE, 59)
        todayCal.set(Calendar.SECOND, 59)
        todayCal.set(Calendar.MILLISECOND, 999)
        val endOfDay = todayCal.timeInMillis

        val habitList = allHabits.filter {
            it.createdTime in startOfDay..endOfDay
        }.toMutableList()

        val recycler = view.findViewById<RecyclerView>(R.id.habits_recycler)
        val adapter = HabitsAdapter(habitList)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        // Swipe-to-delete support for today only!
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val habit = habitList[position]
                HabitStorage.deleteHabit(requireContext(), habit)
                habitList.removeAt(position)
                adapter.notifyItemRemoved(position)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recycler)

        // --- Show today's mood ---
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val moodEmoji = MoodStorage.getMood(requireContext(), todayKey)

        val moodTextView = view.findViewById<TextView>(R.id.today_mood_text)
        moodTextView.visibility = View.VISIBLE
        if (moodEmoji != null) {
            moodTextView.text = "Today's Mood: $moodEmoji"
        } else {
            moodTextView.text = "Today's Mood: Not set"
        }

        return view
    }

    // Sensor logic
    override fun onResume() {
        super.onResume()
        sensorManager = requireContext().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager?.registerListener(
            this,
            sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null && event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]; val y = event.values[1]; val z = event.values[2]
            val gForce = Math.sqrt((x * x + y * y + z * z).toDouble())
            if (gForce > 13) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTime > 1000) {
                    lastShakeTime = now
                    showAddMoodDialog()
                }
            }
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {} // Not needed

    // Mood picker popup dialog
    fun showAddMoodDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_mood, null)
        val builder = android.app.AlertDialog.Builder(requireContext())
            .setTitle("How was your day?")
            .setView(dialogView)
        val alertDialog = builder.create()

        val emojiIds = arrayOf(R.id.emoji_happy, R.id.emoji_ok, R.id.emoji_sad)
        val emojiValues = arrayOf("😊", "😐", "😢")
        emojiIds.forEachIndexed { idx, id ->
            dialogView.findViewById<View>(id).setOnClickListener {
                saveMoodForToday(emojiValues[idx])
                alertDialog.dismiss()
                updateTodayMoodUI()
            }
        }
        alertDialog.show()
    }

    // Save selected mood for today
    fun saveMoodForToday(emoji: String) {
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        MoodStorage.saveMood(requireContext(), todayKey, emoji)
    }

    // Call this to refresh the mood emoji on screen after adding
    fun updateTodayMoodUI() {
        val view = view ?: return
        val todayKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val moodEmoji = MoodStorage.getMood(requireContext(), todayKey)
        val moodTextView = view.findViewById<TextView>(R.id.today_mood_text)
        moodTextView.visibility = View.VISIBLE
        if (moodEmoji != null) {
            moodTextView.text = "Today's Mood: $moodEmoji"
        } else {
            moodTextView.text = "Today's Mood: Not set"
        }
    }

    private fun setTopDateLabel(view: View) {
        val dateLabel = view.findViewById<TextView>(R.id.top_date_label)
        val today = Calendar.getInstance()
        val todayStr = android.text.format.DateFormat.format("EEEE, MMM dd", today)
        dateLabel.text = todayStr
    }

    private fun setCurrentWeekDates(view: View) {
        val dateViews = listOf(
            R.id.date_mon, R.id.date_tue, R.id.date_wed,
            R.id.date_thu, R.id.date_fri, R.id.date_sat, R.id.date_sun
        )
        val weekCal = Calendar.getInstance()
        val todayCal = Calendar.getInstance()
        val todayOfWeek = weekCal.get(Calendar.DAY_OF_WEEK)
        val offset = if (todayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - todayOfWeek
        weekCal.add(Calendar.DAY_OF_MONTH, offset)

        for (id in dateViews) {
            val dateView = view.findViewById<TextView>(id)
            dateView.text = weekCal.get(Calendar.DAY_OF_MONTH).toString()

            val isToday = weekCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    weekCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)

            dateView.setTextColor(if (isToday) Color.WHITE else Color.parseColor("#444444"))
            if (isToday) dateView.setBackgroundResource(R.drawable.day_border)
            else dateView.background = null

            weekCal.add(Calendar.DAY_OF_MONTH, 1)
        }
    }
}

// Mood storage logic
object MoodStorage {
    private const val PREF_NAME = "mood_pref"
    private const val KEY = "mood_map"
    fun saveMood(context: Context, dateKey: String, emoji: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "{}")
        val type = object : TypeToken<MutableMap<String, String>>() {}.type
        val map = Gson().fromJson<MutableMap<String, String>>(json ?: "{}", type)
        map[dateKey] = emoji
        prefs.edit().putString(KEY, Gson().toJson(map)).apply()
    }
    fun getMood(context: Context, dateKey: String): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, "{}")
        val type = object : TypeToken<Map<String, String>>() {}.type
        val map = Gson().fromJson<Map<String, String>>(json ?: "{}", type)
        return map[dateKey]
    }
}
