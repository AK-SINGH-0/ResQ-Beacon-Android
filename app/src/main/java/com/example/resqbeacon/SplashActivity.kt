package com.example.resqbeacon

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Run Animation
        val slideAnimation = AnimationUtils.loadAnimation(this, R.anim.splash_animation)
        findViewById<View>(R.id.logoContainer).startAnimation(slideAnimation)

        // Wait only 0.8 seconds (800ms) for instant feel
        lifecycleScope.launch {
            delay(800)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}