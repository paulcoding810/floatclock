package com.paulcoding.floatclock

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
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
import android.view.WindowManager
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FloatClockService : Service() {
  private lateinit var windowManager: WindowManager
  private lateinit var clockView: TextView
  private lateinit var params: WindowManager.LayoutParams
  private val format = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
  private val choreographer = Choreographer.getInstance()
  private val frameCallback = object : Choreographer.FrameCallback {
    override fun doFrame(frameTimeNanos: Long) {
      clockView.text = format.format(Date())
      choreographer.postFrameCallback(this)
    }
  }

  override fun onCreate() {
    super.onCreate()
    showForegroundNotification()
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    clockView = TextView(this).apply {
      typeface = Typeface.MONOSPACE
      textSize = 18f
      setTextColor(Color.WHITE)
      setBackgroundColor(0x99000000.toInt())
      setPadding(24, 12, 24, 12)
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
  }

  override fun onDestroy() {
    choreographer.removeFrameCallback(frameCallback)
    windowManager.removeView(clockView)
    super.onDestroy()
  }

  override fun onBind(intent: Intent?): IBinder? = null

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
    return builder
      .setContentTitle("Float Clock running")
      .setSmallIcon(android.R.drawable.ic_menu_recent_history)
      .setOngoing(true)
      .build()
  }

  private fun showForegroundNotification() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      startForeground(1, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
    } else {
      startForeground(1, buildNotification())
    }
  }
}
