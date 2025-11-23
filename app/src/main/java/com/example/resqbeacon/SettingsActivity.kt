package com.example.resqbeacon

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat // Changed import

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Updated to use the standard SwitchCompat
        val switchFall = findViewById<SwitchCompat>(R.id.switchFallDetection)
        val sharedPref = getSharedPreferences("ResQSettings", Context.MODE_PRIVATE)

        val isFallEnabled = sharedPref.getBoolean("FALL_DETECTION_ENABLED", true)
        switchFall.isChecked = isFallEnabled

        switchFall.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean("FALL_DETECTION_ENABLED", isChecked).apply()
        }
    }
}