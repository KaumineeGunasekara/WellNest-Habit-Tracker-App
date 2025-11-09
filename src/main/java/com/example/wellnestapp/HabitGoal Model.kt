package com.example.wellnestapp.model

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
data class HabitGoal(
    val title: String,
    val goal: String,
    val amount: String,
    val unit: String,
    val colorHex: String,
    val reminderTimeMillis: Long,
    val createdTime: Long = System.currentTimeMillis(),
    val done: Boolean = false
) : Parcelable
