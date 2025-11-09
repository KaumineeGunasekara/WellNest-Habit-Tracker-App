package com.example.wellnestapp

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

class HomePage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // Show HomeFragment by default (on first launch)
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment(), addToBackStack = false)
        }

        // Bottom navbar navigation
        findViewById<ImageView>(R.id.imageView5).setOnClickListener {
            // Home icon - clear the stack and show HomeFragment only
            replaceFragment(HomeFragment(), addToBackStack = false)
        }
        findViewById<ImageView>(R.id.imageView6).setOnClickListener { replaceFragment(CalendarFragment()) }
        findViewById<ImageView>(R.id.imageView8).setOnClickListener { replaceFragment(ReportFragment()) }
        findViewById<ImageView>(R.id.imageView9).setOnClickListener { replaceFragment(TimerFragment()) }
    }

    // Helper function to replace displayed fragment
    fun replaceFragment(fragment: Fragment, addToBackStack: Boolean = true) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        } else {
            // Clear the whole backstack for home
            supportFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }
        transaction.commit()
    }
}
