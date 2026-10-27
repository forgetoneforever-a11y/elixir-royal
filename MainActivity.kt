package com.elixir.overlay

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var startButton: Button
    private lateinit var stopButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 10, 14)
        window.navigationBarColor = Color.rgb(10, 10, 14)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(10, 10, 14))
            isFillViewport = true
        }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(34), dp(22), dp(28))
        }
        scroll.addView(page, ScrollView.LayoutParams(-1, -1))

        val spacer = View(this)
        page.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        val logo = TextView(this).apply {
            text = "ELIXIR OVERLAY"
            textSize = 29f
            letterSpacing = 0.08f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }
        page.addView(logo, matchWrap())

        val subtitle = TextView(this).apply {
            text = "Помощник для Clash Royale"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(205, 205, 212))
            setPadding(0, dp(8), 0, dp(26))
        }
        page.addView(subtitle, matchWrap())

        statusText = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(205, 205, 212))
            setPadding(0, 0, 0, dp(18))
        }
        page.addView(statusText, matchWrap())

        startButton = makeButton("ЗАПУСТИТЬ ОВЕРЛЕЙ")
        startButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                startOverlay()
            }
        }
        page.addView(startButton, matchWidth())

        stopButton = makeButton("ОСТАНОВИТЬ ОВЕРЛЕЙ")
        stopButton.setOnClickListener {
            stopService(Intent(this, OverlayService::class.java))
            refreshStatus()
        }
        page.addView(stopButton, matchWidth().apply { topMargin = dp(10) })

        val note = TextView(this).apply {
            text = "Важно: распознавание карт по экрану пока не подключено. Сейчас доступна плавающая панель с демонстрационными данными."
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(145, 145, 154))
            setPadding(dp(8), dp(18), dp(8), 0)
        }
        page.addView(note, matchWrap())

        val bottom = View(this)
        page.addView(bottom, LinearLayout.LayoutParams(1, 0, 1f))
        setContentView(scroll)
        refreshStatus()
    }

    private fun startOverlay() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startService(intent) else startService(intent)
        statusText.text = "Статус: запуск..."
    }

    private fun refreshStatus() {
        val hasPermission = Settings.canDrawOverlays(this)
        val running = OverlayService.isRunning
        statusText.text = when {
            running -> "Статус: оверлей запущен"
            !hasPermission -> "Статус: нужно разрешение на оверлей"
            else -> "Статус: оверлей не запущен"
        }
        if (::startButton.isInitialized) {
            startButton.text = if (hasPermission) "ЗАПУСТИТЬ ОВЕРЛЕЙ" else "РАЗРЕШИТЬ ОТОБРАЖЕНИЕ ПОВЕРХ ОКОН"
        }
    }

    private fun makeButton(label: String) = Button(this).apply {
        text = label
        textSize = 13f
        isAllCaps = false
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.rgb(43, 43, 54))
        minHeight = dp(52)
        setPadding(dp(12), dp(8), dp(12), dp(8))
    }

    private fun matchWrap() = LinearLayout.LayoutParams(-1, ViewGroup.LayoutParams.WRAP_CONTENT)
    private fun matchWidth() = LinearLayout.LayoutParams(-1, dp(54))
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
