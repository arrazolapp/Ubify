package com.arrazolapp.gpstracker

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.view.animation.*
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private val SPLASH_DURATION = 5000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pantalla completa sin barra de estado
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        supportActionBar?.hide()

        setContentView(R.layout.activity_splash)

        val imgBee   = findViewById<ImageView>(R.id.splashBee)
        val txtTitle = findViewById<TextView>(R.id.splashTitle)
        val txtFrom  = findViewById<TextView>(R.id.splashFrom)
        val txtBrand = findViewById<TextView>(R.id.splashBrand)

        // 1. "Ubeefy" baja desde arriba con overshoot
        val titleAnim = AnimationSet(true).apply {
            addAnimation(TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, -1.8f, Animation.RELATIVE_TO_SELF, 0f
            ).apply { duration = 800; interpolator = OvershootInterpolator(1.5f) })
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 600 })
            fillAfter = true
        }
        txtTitle.startAnimation(titleAnim)

        // 2. Abejita sube desde abajo con rebote
        val beeEnter = AnimationSet(true).apply {
            addAnimation(TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 2.5f, Animation.RELATIVE_TO_SELF, 0f
            ).apply { duration = 800; interpolator = OvershootInterpolator(1.8f) })
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 500 })
            startOffset = 150
            fillAfter   = true
        }
        imgBee.startAnimation(beeEnter)

        // 3. Bounce continuo: la abejita flota
        Handler(Looper.getMainLooper()).postDelayed({
            val bounce = TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, -0.055f
            ).apply {
                duration     = 900
                repeatCount  = Animation.INFINITE
                repeatMode   = Animation.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                fillAfter    = true
            }
            imgBee.startAnimation(bounce)
        }, 1050L)

        // 4. "from" aparece desde abajo
        val fromAnim = AnimationSet(true).apply {
            addAnimation(TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f
            ).apply { duration = 600; interpolator = DecelerateInterpolator(2f) })
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 600 })
            startOffset = 900
            fillAfter   = true
        }
        txtFrom.startAnimation(fromAnim)

        // 5. "ArrazolApp" aparece desde abajo
        val brandAnim = AnimationSet(true).apply {
            addAnimation(TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0f, Animation.RELATIVE_TO_SELF, 0f,
                Animation.RELATIVE_TO_SELF, 1f, Animation.RELATIVE_TO_SELF, 0f
            ).apply { duration = 600; interpolator = DecelerateInterpolator(2f) })
            addAnimation(AlphaAnimation(0f, 1f).apply { duration = 600 })
            startOffset = 1050
            fillAfter   = true
        }
        txtBrand.startAnimation(brandAnim)

        // 6. Ir a MainActivity después de 5 segundos
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, SPLASH_DURATION)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Bloquear botón atrás durante el splash
    }
}
