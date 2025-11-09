package com.example.wellnestapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment

class TimerFragment : Fragment() {

    private lateinit var timerText: TextView
    private var seconds: Int = 0
    private var running: Boolean = false
    private var wasRunning: Boolean = false
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_timer, container, false)
        timerText = view.findViewById(R.id.timerText)
        val startButton = view.findViewById<Button>(R.id.startButton)
        val stopButton = view.findViewById<Button>(R.id.stopButton)
        val resetButton = view.findViewById<Button>(R.id.resetButton)

        startButton.setOnClickListener { running = true }
        stopButton.setOnClickListener { running = false }
        resetButton.setOnClickListener {
            running = false
            seconds = 0
            updateTimerText()
        }

        runTimer()
        return view
    }

    private fun runTimer() {
        handler.post(object : Runnable {
            override fun run() {
                if (running) seconds++
                updateTimerText()
                handler.postDelayed(this, 1000)
            }
        })
    }

    private fun updateTimerText() {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        timerText.text = String.format("%02d:%02d:%02d", hrs, mins, secs)
    }

    override fun onPause() {
        super.onPause()
        wasRunning = running
        running = false
    }

    override fun onResume() {
        super.onResume()
        if (wasRunning) running = true
    }
}
