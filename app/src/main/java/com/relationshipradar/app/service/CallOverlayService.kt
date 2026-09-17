package com.relationshipradar.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Tether In-Call Floating HUD Overlay.
 * Appears over native phone call dialer to provide instant memory jogger / cheat sheet:
 * - Contact Name & Relationship
 * - Days since last connection
 * - Key talking points & notes
 */
class CallOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private val autoDismissRunnable = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        val action = intent?.action
        if (action == ACTION_STOP) {
            removeOverlay()
            stopSelf()
            return START_NOT_STICKY
        }

        val name = intent?.getStringExtra(EXTRA_NAME) ?: "Tracked Contact"
        val lastEffort = intent?.getStringExtra(EXTRA_LAST_EFFORT) ?: "No recent calls logged"
        val talkingPoints = intent?.getStringExtra(EXTRA_TALKING_POINTS)
        val notes = intent?.getStringExtra(EXTRA_NOTES)

        showOverlay(name, lastEffort, talkingPoints, notes)
        return START_NOT_STICKY
    }

    private fun showOverlay(
        name: String,
        lastEffort: String,
        talkingPoints: String?,
        notes: String?
    ) {
        removeOverlay()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return

        val dp = { value: Float ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, resources.displayMetrics).toInt()
        }

        // Outer Root Frame
        val root = FrameLayout(this).apply {
            setPadding(dp(16f), dp(8f), dp(16f), dp(8f))
        }

        // Inner Card with Machined Obsidian Liquid Glass Gradient
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18f), dp(16f), dp(18f), dp(16f))

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(20f).toFloat()
                colors = intArrayOf(
                    Color.argb(240, 15, 23, 42),   // Obsidian dark slate top
                    Color.argb(248, 2, 6, 23)      // Deep midnight bottom
                )
                setStroke(dp(1.2f), Color.argb(80, 255, 255, 255))
            }
            background = bg
            elevation = dp(12f).toFloat()
        }

        // Top Row: Brand pill + Close Button
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val badgeText = TextView(this).apply {
            text = "TETHER · CHEAT SHEET"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(Color.parseColor("#38BDF8")) // Electric cyan/sky
            typeface = Typeface.DEFAULT_BOLD
            letterSpacing = 0.15f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val closeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#94A3B8"))
            layoutParams = LinearLayout.LayoutParams(dp(22f), dp(22f))
            setOnClickListener {
                removeOverlay()
                stopSelf()
            }
        }

        topRow.addView(badgeText)
        topRow.addView(closeBtn)
        card.addView(topRow)

        // Contact Name
        val nameView = TextView(this).apply {
            text = name
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6f), 0, 0)
        }
        card.addView(nameView)

        // Last Effort Status
        val statusView = TextView(this).apply {
            text = lastEffort
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding(0, dp(2f), 0, dp(8f))
        }
        card.addView(statusView)

        // Talking Points / Notes Section
        val content = talkingPoints?.takeIf { it.isNotBlank() } ?: notes?.takeIf { it.isNotBlank() }
        if (content != null) {
            val divider = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1f)).apply {
                    setMargins(0, dp(4f), 0, dp(8f))
                }
                setBackgroundColor(Color.argb(40, 255, 255, 255))
            }
            card.addView(divider)

            val notesHeader = TextView(this).apply {
                text = "THINGS TO BRING UP:"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextColor(Color.parseColor("#F59E0B")) // Kinetic Amber
                typeface = Typeface.DEFAULT_BOLD
                letterSpacing = 0.1f
            }
            card.addView(notesHeader)

            val notesBody = TextView(this).apply {
                text = content
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTextColor(Color.parseColor("#E2E8F0"))
                maxLines = 4
                setPadding(0, dp(2f), 0, 0)
            }
            card.addView(notesBody)
        }

        root.addView(card)
        overlayView = root

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(48f) // comfortable clearance below Android status bar & call notch
        }

        try {
            windowManager?.addView(root, layoutParams)
            // Auto dismiss after 20 seconds so it never overstays
            handler.removeCallbacks(autoDismissRunnable)
            handler.postDelayed(autoDismissRunnable, 20_000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        handler.removeCallbacks(autoDismissRunnable)
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                // Ignore if already removed
            }
            overlayView = null
        }
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "com.relationshipradar.app.action.SHOW_OVERLAY"
        const val ACTION_STOP = "com.relationshipradar.app.action.STOP_OVERLAY"

        const val EXTRA_NAME = "extra_name"
        const val EXTRA_LAST_EFFORT = "extra_last_effort"
        const val EXTRA_TALKING_POINTS = "extra_talking_points"
        const val EXTRA_NOTES = "extra_notes"

        fun show(
            context: Context,
            name: String,
            lastEffort: String,
            talkingPoints: String?,
            notes: String?
        ) {
            if (!Settings.canDrawOverlays(context)) return
            val intent = Intent(context, CallOverlayService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_NAME, name)
                putExtra(EXTRA_LAST_EFFORT, lastEffort)
                putExtra(EXTRA_TALKING_POINTS, talkingPoints)
                putExtra(EXTRA_NOTES, notes)
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CallOverlayService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
