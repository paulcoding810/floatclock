package com.paulcoding.floatclock

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View

class ClockView(context: Context) : View(context) {
  private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    typeface = Typeface.MONOSPACE
    color = Color.WHITE
    textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f, resources.displayMetrics)
  }
  private val chars = CharArray(12).also { it[2] = ':'; it[5] = ':'; it[8] = '.' }
  private val cell = "0123456789:.".maxOf { paint.measureText(it.toString()) }
  private val padX = 24f
  private val padY = 12f
  private val baseline = padY - paint.fontMetrics.ascent
  private val heightPx = padY * 2 + paint.fontMetrics.descent - paint.fontMetrics.ascent

  fun setTime(msOfDay: Int) {
    val msIntoHour = msOfDay % 3_600_000
    putDigits(0, msOfDay / 3_600_000, 2)
    putDigits(3, msIntoHour / 60_000, 2)
    putDigits(6, msIntoHour / 1_000 % 60, 2)
    putDigits(9, msIntoHour % 1_000, 3)
    invalidate()
  }

  private fun putDigits(offset: Int, value: Int, count: Int) {
    var v = value
    for (i in count - 1 downTo 0) {
      chars[offset + i] = '0' + v % 10
      v /= 10
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension((padX * 2 + cell * chars.size).toInt(), heightPx.toInt())
  }

  override fun onDraw(canvas: Canvas) {
    for (i in chars.indices) {
      canvas.drawText(chars, i, 1, padX + i * cell, baseline, paint)
    }
  }
}
