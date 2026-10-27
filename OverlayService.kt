package com.elixir.overlay

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import kotlin.math.abs

class OverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private var panel: View? = null
    private var params: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (panel == null) showPanel()
        return START_STICKY
    }

    private fun showPanel() {
        if (panel != null) return

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.argb(238, 18, 18, 24))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = "ELIXIR  •  CR"
            textSize = 14f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, dp(10), 0)
        }
        header.addView(title, LinearLayout.LayoutParams(0, -2, 1f))

        val minimize = TextView(this).apply {
            text = "—"
            textSize = 22f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(10), 0, dp(10), 0)
            setOnClickListener { toggleCompact(root, this) }
        }
        header.addView(minimize, LinearLayout.LayoutParams(-2, dp(34)))
        val close = TextView(this).apply {
            text = "×"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setPadding(dp(8), 0, dp(4), 0)
            setOnClickListener { stopSelf() }
        }
        header.addView(close, LinearLayout.LayoutParams(-2, dp(34)))
        root.addView(header)

        val divider = View(this).apply { setBackgroundColor(Color.rgb(70, 70, 82)) }
        root.addView(divider, LinearLayout.LayoutParams(-1, dp(1)).apply {
            topMargin = dp(5)
            bottomMargin = dp(8)
        })

        val status = TextView(this).apply {
            text = "ПАНЕЛЬ АКТИВНА"
            textSize = 11f
            letterSpacing = 0.08f
            setTextColor(Color.rgb(165, 235, 190))
        }
        root.addView(status)

        val info = TextView(this).apply {
            text = "Карты: ожидание распознавания\\nЭликсир: —\\nПодсказка: демонстрационный режим"
            textSize = 12f
            setTextColor(Color.rgb(220, 220, 226))
            setLineSpacing(dp(3).toFloat(), 1f)
            setPadding(0, dp(7), 0, dp(5))
        }
        root.addView(info)

        val action = Button(this).apply {
            text = "Скрыть панель"
            textSize = 11f
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(48, 48, 62))
            setOnClickListener { root.visibility = View.GONE; showBubble() }
        }
        root.addView(action, LinearLayout.LayoutParams(-1, dp(38)))

        val lp = WindowManager.LayoutParams(
            dp(250),
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12)
            y = dp(110)
        }
        params = lp
        panel = root
        try {
            windowManager.addView(root, lp)
            attachDrag(root, lp)
        } catch (_: Exception) {
            panel = null
            stopSelf()
        }
    }

    private var bubble: View? = null

    private fun showBubble() {
        if (bubble != null) return
        val b = TextView(this).apply {
            text = "E"
            textSize = 20f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.rgb(48, 48, 62))
            setOnClickListener {
                panel?.visibility = View.VISIBLE
                removeBubble()
            }
        }
        val lp = WindowManager.LayoutParams(
            dp(52), dp(52),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(12); y = dp(170)
        }
        bubble = b
        try {
            windowManager.addView(b, lp)
            attachDrag(b, lp)
        } catch (_: Exception) {
            bubble = null
        }
    }

    private fun toggleCompact(root: LinearLayout, button: TextView) {
        val info = if (root.childCount > 2) root.getChildAt(2) else null
        if (info?.visibility == View.GONE) {
            for (i in 2 until root.childCount) root.getChildAt(i).visibility = View.VISIBLE
            button.text = "—"
        } else {
            for (i in 2 until root.childCount) root.getChildAt(i).visibility = View.GONE
            button.text = "+"
        }
    }

    private fun attachDrag(view: View, lp: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        view.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x; startY = lp.y
                    touchX = event.rawX; touchY = event.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - touchX).toInt()
                    val dy = (event.rawY - touchY).toInt()
                    if (abs(dx) > dp(3) || abs(dy) > dp(3)) {
                        lp.x = (startX - dx).coerceAtLeast(0)
                        lp.y = (startY + dy).coerceAtLeast(0)
                        try { windowManager.updateViewLayout(view, lp) } catch (_: Exception) {}
                        true
                    } else false
                }
                else -> false
            }
        }
    }

    private fun removeBubble() {
        bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        bubble = null
    }

    override fun onDestroy() {
        panel?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        panel = null
        removeBubble()
        isRunning = false
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        @Volatile var isRunning: Boolean = false
            private set
    }
}
