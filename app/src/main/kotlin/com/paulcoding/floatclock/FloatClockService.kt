package com.paulcoding.floatclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import java.util.TimeZone

class FloatClockService : Service() {
  private lateinit var windowManager: WindowManager
  private lateinit var clockView: TextView
  private lateinit var params: WindowManager.LayoutParams
  private val digits = CharArray(12).also { it[2] = ':'; it[5] = ':'; it[8] = '.' }
  private var lastSecond = -1L
  private var offsetMs = 0
  private var highlighted = false
  private val choreographer = Choreographer.getInstance()
  private val frameCallback = object : Choreographer.FrameCallback {
    override fun doFrame(frameTimeNanos: Long) {
      render(System.currentTimeMillis())
      choreographer.postFrameCallback(this)
    }
  }

  override fun onCreate() {
    super.onCreate()
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    clockView = TextView(this).apply {
      typeface = Typeface.MONOSPACE
      textSize = 18f
      setTextColor(Color.WHITE)
      setBackgroundColor(NORMAL_COLOR)
      setPadding(24, 12, 24, 12)
      gravity = Gravity.CENTER
      width = paint.measureText("88:88:88.888").toInt() + paddingLeft + paddingRight
      setOnTouchListener(DragListener())
    }
    val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    else
      @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
    params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      type,
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = 100
      y = 200
    }
    windowManager.addView(clockView, params)
    choreographer.postFrameCallback(frameCallback)
    showForegroundNotification()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    when (intent?.action) {
      ACTION_TOGGLE -> {
        setClockVisible(clockView.visibility != View.VISIBLE)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
      }
      ACTION_STOP -> stopSelf()
    }
    return START_STICKY
  }

  override fun onDestroy() {
    choreographer.removeFrameCallback(frameCallback)
    windowManager.removeView(clockView)
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

  private fun setClockVisible(visible: Boolean) {
    clockView.visibility = if (visible) View.VISIBLE else View.GONE
    choreographer.removeFrameCallback(frameCallback)
    if (visible) choreographer.postFrameCallback(frameCallback)
  }

  private fun render(now: Long) {
    val second = now / 1000
    if (second != lastSecond) {
      lastSecond = second
      offsetMs = TimeZone.getDefault().getOffset(now)
    }
    val msOfDay = Math.floorMod(now + offsetMs, 86_400_000L).toInt()
    val msIntoHour = msOfDay % 3_600_000
    putDigits(0, msOfDay / 3_600_000, 2)
    putDigits(3, msIntoHour / 60_000, 2)
    putDigits(6, msIntoHour / 1_000 % 60, 2)
    putDigits(9, msIntoHour % 1_000, 3)
    clockView.setText(digits, 0, digits.size)

    val highlight = msIntoHour < HIGHLIGHT_DURATION_MS
    if (highlight != highlighted) {
      highlighted = highlight
      clockView.setBackgroundColor(if (highlight) HIGHLIGHT_COLOR else NORMAL_COLOR)
    }
  }

  private fun putDigits(offset: Int, value: Int, count: Int) {
    var v = value
    for (i in count - 1 downTo 0) {
      digits[offset + i] = '0' + v % 10
      v /= 10
    }
  }

  private fun servicePendingIntent(action: String): PendingIntent {
    val intent = Intent(this, FloatClockService::class.java).setAction(action)
    return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
  }

  private inner class DragListener : android.view.View.OnTouchListener {
    private var startX = 0
    private var startY = 0
    private var touchX = 0f
    private var touchY = 0f

    override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          startX = params.x
          startY = params.y
          touchX = event.rawX
          touchY = event.rawY
        }
        MotionEvent.ACTION_MOVE -> {
          params.x = startX + (event.rawX - touchX).toInt()
          params.y = startY + (event.rawY - touchY).toInt()
          windowManager.updateViewLayout(clockView, params)
        }
        MotionEvent.ACTION_UP -> v.performClick()
      }
      return true
    }
  }

  private fun buildNotification(): Notification {
    val channelId = "float_clock"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(channelId, "Float Clock", NotificationManager.IMPORTANCE_MIN)
      getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
      Notification.Builder(this, channelId)
    else
      @Suppress("DEPRECATION") Notification.Builder(this)
    val visible = clockView.visibility == View.VISIBLE
    val openApp = PendingIntent.getActivity(
      this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )
    return builder
      .setContentTitle(if (visible) "Float Clock shown" else "Float Clock hidden")
      .setSmallIcon(android.R.drawable.ic_menu_recent_history)
      .setContentIntent(openApp)
      .setOngoing(true)
      .addAction(action(if (visible) "Hide" else "Show", ACTION_TOGGLE))
      .addAction(action("Stop", ACTION_STOP))
      .build()
  }

  private fun action(title: String, action: String): Notification.Action =
    Notification.Action.Builder(null, title, servicePendingIntent(action)).build()

  private fun showForegroundNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(NOTIFICATION_ID, buildNotification())
    }
  }

  companion object {
    const val ACTION_TOGGLE = "com.paulcoding.floatclock.TOGGLE"
    const val ACTION_STOP = "com.paulcoding.floatclock.STOP"
    private const val NOTIFICATION_ID = 1
    private const val HIGHLIGHT_DURATION_MS = 3_000
    private const val NORMAL_COLOR = 0x99000000.toInt()
    private const val HIGHLIGHT_COLOR = 0xFFFF8C00.toInt()
  }
}
