package com.example.wellnestapp

import android.content.Context
import com.example.wellnestapp.model.HabitGoal
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object HabitStorage {
    private const val PREF_NAME = "habit_pref"
    private const val HABIT_KEY = "habit_list"

    /** Save a single habit */
    fun saveHabit(context: Context, habit: HabitGoal) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val list: MutableList<HabitGoal> = getHabits(context).toMutableList()
        list.add(habit)
        val json = Gson().toJson(list)
        prefs.edit().putString(HABIT_KEY, json).apply()
    }

    /** Save the full habit list (used in update/delete) */
    fun saveHabits(context: Context, habits: List<HabitGoal>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(habits)
        prefs.edit().putString(HABIT_KEY, json).apply()
    }

    /** Load all habits; returns empty list if none */
    fun getHabits(context: Context): List<HabitGoal> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(HABIT_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<HabitGoal>>() {}.type
            Gson().fromJson<List<HabitGoal>>(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }

    /** Delete by ID (createdTime, unique) */
    fun deleteHabit(context: Context, habit: HabitGoal) {
        val current: MutableList<HabitGoal> = getHabits(context).toMutableList()
        val removed = current.removeIf { it.createdTime == habit.createdTime }
        if (removed) {
            saveHabits(context, current)
        }
    }
}
