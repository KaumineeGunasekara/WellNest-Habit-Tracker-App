package com.example.wellnestapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnestapp.model.HabitGoal

class HabitsAdapter(
    private val habits: MutableList<HabitGoal>
) : RecyclerView.Adapter<HabitsAdapter.HabitViewHolder>() {

    inner class HabitViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardInner: View = view.findViewById(R.id.habit_card_inner)
        val title: TextView = view.findViewById(R.id.habit_title)
        val goal: TextView = view.findViewById(R.id.habit_goal)
        val amountUnit: TextView = view.findViewById(R.id.habit_amount_unit)
        val doneIcon: ImageView = view.findViewById(R.id.habit_done_icon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HabitViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_habit_goal, parent, false)
        return HabitViewHolder(view)
    }

    override fun onBindViewHolder(holder: HabitViewHolder, position: Int) {
        val habit = habits[position]
        holder.title.text = habit.title
        holder.goal.text = habit.goal
        holder.amountUnit.text = "${habit.amount} ${habit.unit}"

        // Set border color dynamically
        val drawable = holder.cardInner.background as? GradientDrawable
        drawable?.setStroke(3, Color.parseColor(habit.colorHex))

        // Show tick or box
        holder.doneIcon.setImageResource(if (habit.done) R.drawable.tick else R.drawable.box)

        // Toggle done status on icon click, save result
        holder.doneIcon.setOnClickListener {
            val updatedHabit = habit.copy(done = !habit.done)
            habits[position] = updatedHabit

            // Save the update in storage
            val context = holder.itemView.context
            val allHabits: List<HabitGoal> = HabitStorage.getHabits(context)
            val mutableHabits: MutableList<HabitGoal> = allHabits.toMutableList()
            val index = mutableHabits.indexOfFirst { it.createdTime == habit.createdTime }
            if (index != -1) {
                mutableHabits[index] = updatedHabit
                HabitStorage.saveHabits(context, mutableHabits)
            }


            notifyItemChanged(position)
        }

        // Edit habit on title click
        holder.title.setOnClickListener {
            val activity = holder.itemView.context as? HomePage
            activity?.replaceFragment(NewHabitFragment.forEdit(habit))
        }
    }

    override fun getItemCount() = habits.size
}
