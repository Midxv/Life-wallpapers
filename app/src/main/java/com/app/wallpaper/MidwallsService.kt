package com.app.wallpaper

import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.preference.PreferenceManager
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.sqrt

class MidwallsService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return MidwallsEngine()
    }

    inner class MidwallsEngine : Engine(), SharedPreferences.OnSharedPreferenceChangeListener {

        private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        private var bgBitmap: Bitmap? = null

        private val paintBox = Paint().apply { color = Color.parseColor("#1A1A1A"); style = Paint.Style.STROKE; isAntiAlias = true }
        private val paintDotComplete = Paint().apply { color = Color.parseColor("#FF4444"); style = Paint.Style.FILL; isAntiAlias = true }
        private val paintDotPending = Paint().apply { color = Color.parseColor("#1FFFFFFF"); style = Paint.Style.FILL; isAntiAlias = true }
        private val paintTextQuote = Paint().apply { color = Color.WHITE; textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC) }
        private val paintTextDate = Paint().apply { color = Color.GRAY; textAlign = Paint.Align.CENTER; isAntiAlias = true; typeface = Typeface.DEFAULT_BOLD }

        private val defaultQuotes = listOf("Time is the most valuable thing a man can spend.", "Focus on being productive instead of busy.", "Action is the foundational key to all success.")

        override fun onCreate(surfaceHolder: SurfaceHolder?) {
            super.onCreate(surfaceHolder)
            prefs.registerOnSharedPreferenceChangeListener(this)
            loadBackground()
        }

        override fun onDestroy() {
            super.onDestroy()
            prefs.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (key == "bg_image_uri") loadBackground()
            if (isVisible) draw()
        }

        private fun loadBackground() {
            val uriStr = prefs.getString("bg_image_uri", null)
            if (uriStr != null) {
                try {
                    val uri = Uri.parse(uriStr)
                    val inputStream = contentResolver.openInputStream(uri)
                    bgBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()
                } catch (e: Exception) { e.printStackTrace() }
            } else { bgBitmap = null }
        }

        override fun onVisibilityChanged(visible: Boolean) { if (visible) draw() }
        override fun onSurfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            draw()
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    val width = canvas.width.toFloat()
                    val height = canvas.height.toFloat()

                    val goalDuration = prefs.getInt("goal_duration", 365)
                    val defaultStart = Calendar.getInstance().apply { set(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
                    val startTime = prefs.getLong("goal_start_time", defaultStart)

                    val now = System.currentTimeMillis()
                    val diffMillis = now - startTime
                    val daysPassed = (diffMillis / (1000 * 60 * 60 * 24)).toInt() + 1
                    val currentDay = daysPassed.coerceIn(0, goalDuration)

                    // PREFS
                    val bgColor = prefs.getInt("bg_color", Color.BLACK)
                    val dotShape = prefs.getString("dot_shape", "circle") ?: "circle"
                    val dotSizeScale = prefs.getFloat("dot_size_scale", 1.0f)
                    val showBox = prefs.getBoolean("show_box", true)
                    val showText = prefs.getBoolean("show_text", true)

                    val boxStroke = prefs.getFloat("box_stroke", 5f)
                    val quoteSize = prefs.getFloat("quote_size", 50f)
                    val dateSize = prefs.getFloat("date_size", 35f)
                    val userQuote = prefs.getString("user_quote", "") ?: ""
                    val displayQuote = if (userQuote.isEmpty()) defaultQuotes[(daysPassed % defaultQuotes.size + defaultQuotes.size) % defaultQuotes.size] else userQuote

                    // DRAW BG
                    if (bgBitmap != null) {
                        val scale = (height / bgBitmap!!.height.toFloat()).coerceAtLeast(width / bgBitmap!!.width.toFloat())
                        val drawW = bgBitmap!!.width * scale
                        val drawH = bgBitmap!!.height * scale
                        val left = (width - drawW) / 2
                        val top = (height - drawH) / 2
                        canvas.drawBitmap(bgBitmap!!, null, RectF(left, top, left + drawW, top + drawH), null)
                        canvas.drawColor(Color.parseColor("#80000000"))
                    } else { canvas.drawColor(bgColor) }

                    paintBox.strokeWidth = boxStroke
                    paintTextQuote.textSize = quoteSize
                    paintTextDate.textSize = dateSize

                    var textY = prefs.getFloat("text_y", height * 0.15f)
                    var textX = prefs.getFloat("text_x", width / 2)
                    var boxY = prefs.getFloat("box_y", height * 0.25f)
                    var boxX = prefs.getFloat("box_x", width / 2)
                    var boxW = prefs.getFloat("box_w", width * 0.8f)
                    var boxH = prefs.getFloat("box_h", height * 0.5f)
                    if (boxW < 100) boxW = width * 0.8f
                    if (boxH < 100) boxH = height * 0.5f

                    // DRAW TEXT
                    if (showText) {
                        canvas.drawText("\"$displayQuote\"", textX, textY, paintTextQuote)
                        canvas.drawText("Day $currentDay of $goalDuration", textX, textY + (quoteSize * 1.5f), paintTextDate)
                    }

                    // DRAW BOX & DOTS
                    if (showBox) {
                        val boxLeft = boxX - (boxW / 2)
                        val boxTop = boxY
                        val boxRect = RectF(boxLeft, boxTop, boxLeft + boxW, boxTop + boxH)
                        canvas.drawRoundRect(boxRect, 40f, 40f, paintBox)

                        val ratio = boxW / boxH
                        val cols = sqrt(goalDuration * ratio).toInt().coerceAtLeast(3)
                        val rows = ceil(goalDuration.toFloat() / cols).toInt()
                        val hSpacing = boxW / (cols + 1)
                        val vSpacing = boxH / (rows + 1)
                        val baseRadius = 5f * dotSizeScale

                        var currentDot = 0
                        for (row in 0 until rows) {
                            for (col in 0 until cols) {
                                if (currentDot >= goalDuration) break
                                val cx = boxRect.left + (hSpacing * (col + 1))
                                val cy = boxRect.top + (vSpacing * (row + 1))
                                val p = if (currentDot < currentDay) paintDotComplete else paintDotPending

                                if (dotShape == "square") canvas.drawRect(cx - baseRadius, cy - baseRadius, cx + baseRadius, cy + baseRadius, p)
                                else canvas.drawCircle(cx, cy, baseRadius, p)
                                currentDot++
                            }
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() } finally { if (canvas != null) holder.unlockCanvasAndPost(canvas) }
        }
    }
}