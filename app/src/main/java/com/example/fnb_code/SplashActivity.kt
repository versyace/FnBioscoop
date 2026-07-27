package com.fnbioscoop

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.fnbioscoop.R

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imgLogo    = findViewById<View>(R.id.imgLogo)
        val tvTagline  = findViewById<TextView>(R.id.tvTagline)
        val layoutDots = findViewById<View>(R.id.layoutDots)

        imgLogo.scaleX = 0.5f
        imgLogo.scaleY = 0.5f

        val set = AnimatorSet()
        set.playTogether(
            ObjectAnimator.ofFloat(imgLogo, "alpha", 0f, 1f).apply { duration = 600 },
            ObjectAnimator.ofFloat(imgLogo, "scaleX", 0.5f, 1f).apply { duration = 600 },
            ObjectAnimator.ofFloat(imgLogo, "scaleY", 0.5f, 1f).apply { duration = 600 },
            ObjectAnimator.ofFloat(tvTagline, "alpha", 0f, 1f).apply { duration = 400; startDelay = 700 },
            ObjectAnimator.ofFloat(layoutDots, "alpha", 0f, 1f).apply { duration = 400; startDelay = 900 }
        )
        set.interpolator = AccelerateDecelerateInterpolator()
        set.start()

        animateDots()

        Handler(Looper.getMainLooper()).postDelayed({
            val next = when {
                !UserPrefs.isLoggedIn(this) -> Intent(this, LoginActivity::class.java)
                UserPrefs.isAdmin(this)     -> Intent(this, AdminDashboardActivity::class.java)
                else                        -> Intent(this, MenuActivity::class.java)
            }
            startActivity(next)
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }, 2800)
    }

    private fun animateDots() {
        val dot1 = findViewById<View>(R.id.dot1)
        val dot2 = findViewById<View>(R.id.dot2)
        val dot3 = findViewById<View>(R.id.dot3)
        val handler = Handler(Looper.getMainLooper())
        var step = 0
        val runnable = object : Runnable {
            override fun run() {
                when (step % 3) {
                    0 -> { dot1.alpha = 1f; dot2.alpha = 0.4f; dot3.alpha = 0.4f }
                    1 -> { dot1.alpha = 0.4f; dot2.alpha = 1f; dot3.alpha = 0.4f }
                    2 -> { dot1.alpha = 0.4f; dot2.alpha = 0.4f; dot3.alpha = 1f }
                }
                step++
                handler.postDelayed(this, 400)
            }
        }
        handler.postDelayed(runnable, 1000)
    }
}
