package com.app.wallpaper

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sqrt

class EditorActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var canvasView: EditorCanvasView

    // UI Panels
    private lateinit var bottomPanel: LinearLayout
    private lateinit var panelBackground: View
    private lateinit var panelElement: View
    private lateinit var tvLabel: TextView
    private lateinit var ivDustbin: ImageView
    private lateinit var layoutAddMenu: LinearLayout

    // Controls
    private lateinit var seekSize: SeekBar
    private lateinit var seekStroke: SeekBar
    private lateinit var seekDotSize: SeekBar
    private lateinit var btnEditQuote: Button
    private lateinit var btnToggleShape: Button

    // Add Menu Buttons
    private lateinit var btnAddBox: Button
    private lateinit var btnAddText: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        prefs = PreferenceManager.getDefaultSharedPreferences(this)

        val container = findViewById<FrameLayout>(R.id.editor_container)
        // Pass "this" (Activity) to view so it can callback methods
        canvasView = EditorCanvasView(this, this)
        container.addView(canvasView)

        // Bind Views
        bottomPanel = findViewById(R.id.bottom_panel)
        panelBackground = findViewById(R.id.panelBackground)
        panelElement = findViewById(R.id.panelElement)
        tvLabel = findViewById(R.id.tvLabel)
        ivDustbin = findViewById(R.id.ivDustbin)
        layoutAddMenu = findViewById(R.id.layoutAddMenu)

        seekSize = findViewById(R.id.seekSize)
        seekStroke = findViewById(R.id.seekStroke)
        seekDotSize = findViewById(R.id.seekDotSize)
        btnEditQuote = findViewById(R.id.btnEditQuote)
        btnToggleShape = findViewById(R.id.btnToggleShape)
        btnAddBox = findViewById(R.id.btnAddBox)
        btnAddText = findViewById(R.id.btnAddText)
        val btnSave = findViewById<Button>(R.id.btnSave)

        setupBackgroundColors()
        setupListeners()
        setupAddMenu()

        btnSave.setOnClickListener {
            canvasView.saveToPrefs()
            Toast.makeText(this, "Layout Saved!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun setupAddMenu() {
        btnAddBox.setOnClickListener {
            canvasView.restoreElement("box", layoutAddMenu.x, layoutAddMenu.y)
            layoutAddMenu.visibility = View.GONE
        }
        btnAddText.setOnClickListener {
            canvasView.restoreElement("text", layoutAddMenu.x, layoutAddMenu.y)
            layoutAddMenu.visibility = View.GONE
        }
    }

    private fun setupListeners() {
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                if (canvasView.selectedMode == "box") {
                    val scale = 0.5f + (progress / 100f)
                    canvasView.updateBoxScale(scale)
                } else if (canvasView.selectedMode == "text") {
                    canvasView.textQuoteSize = 20f + (progress * 0.8f)
                    canvasView.invalidate()
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        seekStroke.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                canvasView.boxStroke = 1f + (progress / 2f)
                canvasView.invalidate()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        seekDotSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(p0: SeekBar?, progress: Int, p2: Boolean) {
                val scale = 0.5f + (progress / 50f)
                canvasView.dotSizeScale = scale
                canvasView.invalidate()
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        btnEditQuote.setOnClickListener {
            val input = EditText(this)
            input.setText(canvasView.currentQuote)
            AlertDialog.Builder(this)
                .setTitle("Enter Quote")
                .setView(input)
                .setPositiveButton("OK") { _, _ ->
                    canvasView.currentQuote = input.text.toString()
                    canvasView.invalidate()
                }
                .show()
        }

        btnToggleShape.setOnClickListener {
            canvasView.isCircleDot = !canvasView.isCircleDot
            btnToggleShape.text = if (canvasView.isCircleDot) "Shape: Circle" else "Shape: Square"
            canvasView.invalidate()
        }
    }

    private fun setupBackgroundColors() {
        val colors = listOf(Color.BLACK, Color.parseColor("#1A1A1A"), Color.parseColor("#001F3F"),
            Color.parseColor("#2E0404"), Color.parseColor("#052805"), Color.parseColor("#200528"), Color.parseColor("#331800"))
        val ids = listOf(R.id.color1, R.id.color2, R.id.color3, R.id.color4, R.id.color5, R.id.color6, R.id.color7)

        for (i in colors.indices) {
            findViewById<View>(ids[i]).setOnClickListener {
                canvasView.bgColor = colors[i]
                canvasView.bgImage = null
                prefs.edit().remove("bg_image_uri").apply()
                canvasView.invalidate()
            }
        }

        findViewById<Button>(R.id.btnCustomImage).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    val inputStream = contentResolver.openInputStream(uri)
                    val file = File(filesDir, "custom_bg.jpg")
                    val outputStream = FileOutputStream(file)
                    inputStream?.copyTo(outputStream)
                    inputStream?.close()
                    outputStream.close()
                    val internalUri = Uri.fromFile(file)
                    canvasView.loadBgImage(internalUri)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    // --- UI METHODS CALLED BY CANVAS ---
    fun setUIVisibility(visible: Boolean) {
        val state = if (visible) View.VISIBLE else View.GONE
        bottomPanel.visibility = state

        // Dustbin is OPPOSITE of UI
        ivDustbin.visibility = if (visible) View.GONE else View.VISIBLE
    }

    fun showAddMenu(x: Float, y: Float) {
        // Position the menu where finger held
        layoutAddMenu.x = x.coerceIn(0f, (layoutAddMenu.parent as View).width - layoutAddMenu.width.toFloat())
        layoutAddMenu.y = y.coerceIn(0f, (layoutAddMenu.parent as View).height - layoutAddMenu.height.toFloat())
        layoutAddMenu.visibility = View.VISIBLE
    }

    fun hideAddMenu() {
        layoutAddMenu.visibility = View.GONE
    }

    fun isOverDustbin(x: Float, y: Float): Boolean {
        if (ivDustbin.visibility != View.VISIBLE) return false

        // Get dustbin screen location
        val location = IntArray(2)
        ivDustbin.getLocationOnScreen(location)
        val dx = location[0]
        val dy = location[1] // Note: this is raw screen coord, might need adjustment relative to view

        // Simpler approach: Check if Y is in bottom 15% and X is center
        // Dustbin is centered at bottom.
        val w = ivDustbin.width
        val h = ivDustbin.height
        // ivDustbin.y is relative to parent, EditorActivity root
        val binTop = ivDustbin.y
        val binLeft = ivDustbin.x

        return x > binLeft && x < binLeft + w && y > binTop && y < binTop + h
    }

    fun onElementSelected(mode: String) {
        if (mode == "none") {
            tvLabel.text = "Background"
            panelBackground.visibility = View.VISIBLE
            panelElement.visibility = View.GONE
        } else {
            panelBackground.visibility = View.GONE
            panelElement.visibility = View.VISIBLE

            if (mode == "box") {
                tvLabel.text = "Box Settings"
                findViewById<View>(R.id.rowStroke).visibility = View.VISIBLE
                findViewById<View>(R.id.rowDotSize).visibility = View.VISIBLE
                btnToggleShape.visibility = View.VISIBLE
                btnEditQuote.visibility = View.GONE
                seekSize.progress = 50
                seekDotSize.progress = ((canvasView.dotSizeScale - 0.5f) * 50).toInt()
            } else if (mode == "text") {
                tvLabel.text = "Text Settings"
                findViewById<View>(R.id.rowStroke).visibility = View.GONE
                findViewById<View>(R.id.rowDotSize).visibility = View.GONE
                btnToggleShape.visibility = View.GONE
                btnEditQuote.visibility = View.VISIBLE
                seekSize.progress = ((canvasView.textQuoteSize - 20) / 0.8).toInt()
            }
        }
    }

    inner class EditorCanvasView(context: Context, val activity: EditorActivity) : View(context) {

        var bgColor = Color.BLACK
        var bgImage: Bitmap? = null
        var currentQuote: String = "Time is valuable."
        var isCircleDot = true
        var boxStroke = 5f
        var textQuoteSize = 50f
        var dotSizeScale = 1.0f

        // Visibility Flags
        var showBox = true
        var showText = true

        var textX = -1f
        var textY = -1f
        var boxX = -1f
        var boxY = -1f
        var boxW = 0f
        var boxH = 0f

        var selectedMode = "none"
        private var dragOffsetX = 0f
        private var dragOffsetY = 0f

        // Long Press Logic
        private val handler = Handler(Looper.getMainLooper())
        private var isLongPressTriggered = false
        private val longPressRunnable = Runnable {
            if (selectedMode == "none") {
                isLongPressTriggered = true
                activity.showAddMenu(lastTouchX, lastTouchY)
            }
        }
        private var lastTouchX = 0f
        private var lastTouchY = 0f

        private val pBox = Paint().apply { style = Paint.Style.STROKE; color = Color.parseColor("#CCCCCC") }
        private val pText = Paint().apply { color = Color.WHITE; textAlign = Paint.Align.CENTER }
        private val pDot = Paint().apply { style = Paint.Style.FILL; color = Color.WHITE; alpha = 100 }
        private val pSelection = Paint().apply { style = Paint.Style.STROKE; color = Color.YELLOW; strokeWidth = 4f; pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f) }

        init {
            bgColor = prefs.getInt("bg_color", Color.BLACK)
            boxStroke = prefs.getFloat("box_stroke", 5f)
            textQuoteSize = prefs.getFloat("quote_size", 50f)
            dotSizeScale = prefs.getFloat("dot_size_scale", 1.0f)
            showBox = prefs.getBoolean("show_box", true)
            showText = prefs.getBoolean("show_text", true)

            val savedUri = prefs.getString("bg_image_uri", null)
            if (savedUri != null) loadBgImage(Uri.parse(savedUri))

            val savedQuote: String? = prefs.getString("user_quote", "")
            if (savedQuote != null && savedQuote.isNotEmpty()) currentQuote = savedQuote

            val shape: String? = prefs.getString("dot_shape", "circle")
            if (shape != null) isCircleDot = (shape == "circle")

            textX = prefs.getFloat("text_x", -1f)
            textY = prefs.getFloat("text_y", -1f)
            boxX = prefs.getFloat("box_x", -1f)
            boxY = prefs.getFloat("box_y", -1f)
            boxW = prefs.getFloat("box_w", -1f)
            boxH = prefs.getFloat("box_h", -1f)
        }

        fun loadBgImage(uri: Uri) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                bgImage = BitmapFactory.decodeStream(stream)
                invalidate()
            } catch(e: Exception) {}
        }

        fun updateBoxScale(scale: Float) {
            boxW = width * 0.8f * scale
            boxH = height * 0.5f * scale
            invalidate()
        }

        fun restoreElement(type: String, x: Float, y: Float) {
            if (type == "box") {
                showBox = true
                boxX = x
                boxY = y
                // Default size if missing
                if (boxW <= 0) { boxW = width * 0.8f; boxH = height * 0.5f }
                selectedMode = "box"
            } else {
                showText = true
                textX = x
                textY = y
                selectedMode = "text"
            }
            activity.onElementSelected(selectedMode)
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
            if (textX == -1f) textX = w / 2f
            if (textY == -1f) textY = h * 0.15f
            if (boxX == -1f) boxX = w / 2f
            if (boxY == -1f) boxY = h * 0.25f
            if (boxW == -1f) boxW = w * 0.8f
            if (boxH == -1f) boxH = h * 0.5f
        }

        override fun onDraw(canvas: Canvas) {
            // BG
            if (bgImage != null) {
                val scale = (height.toFloat() / bgImage!!.height).coerceAtLeast(width.toFloat() / bgImage!!.width)
                val drawW = bgImage!!.width * scale
                val drawH = bgImage!!.height * scale
                val left = (width - drawW) / 2
                val top = (height - drawH) / 2
                canvas.drawBitmap(bgImage!!, null, RectF(left, top, left + drawW, top + drawH), null)
                canvas.drawColor(Color.parseColor("#80000000"))
            } else {
                canvas.drawColor(bgColor)
            }

            // Draw Text
            if (showText) {
                pText.textSize = textQuoteSize
                canvas.drawText(currentQuote, textX, textY, pText)
            }

            // Draw Box
            if (showBox) {
                pBox.strokeWidth = boxStroke
                val left = boxX - (boxW/2)
                val top = boxY
                val rect = RectF(left, top, left+boxW, top+boxH)
                canvas.drawRoundRect(rect, 40f, 40f, pBox)

                // Sample Dots
                val cx = boxX
                val cy = boxY + (boxH/2)
                val r = 5f * dotSizeScale
                if(isCircleDot) canvas.drawCircle(cx, cy, r, pDot)
                else canvas.drawRect(cx-r, cy-r, cx+r, cy+r, pDot)
            }

            // Highlights
            if (selectedMode == "text" && showText) {
                canvas.drawRect(textX - 200, textY - textQuoteSize, textX + 200, textY + 20, pSelection)
            } else if (selectedMode == "box" && showBox) {
                val left = boxX - (boxW/2)
                val rect = RectF(left, boxY, left+boxW, boxY+boxH)
                canvas.drawRect(rect.left - 20, rect.top - 20, rect.right + 20, rect.bottom + 20, pSelection)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = x
                    lastTouchY = y
                    isLongPressTriggered = false
                    activity.hideAddMenu() // Hide menu if visible

                    // Hit Test
                    var hitText = false
                    if (showText) {
                        val distText = sqrt((x-textX)*(x-textX) + (y-textY)*(y-textY))
                        if (distText < 150) hitText = true
                    }

                    var hitBox = false
                    if (showBox) {
                        val left = boxX - (boxW/2)
                        if (x > left && x < left+boxW && y > boxY && y < boxY+boxH) hitBox = true
                    }

                    if (hitText) {
                        selectedMode = "text"
                        dragOffsetX = textX - x
                        dragOffsetY = textY - y
                        activity.setUIVisibility(false) // Hide UI
                    } else if (hitBox) {
                        selectedMode = "box"
                        dragOffsetX = boxX - x
                        dragOffsetY = boxY - y
                        activity.setUIVisibility(false) // Hide UI
                    } else {
                        selectedMode = "none"
                        // Start Long Press Timer
                        handler.postDelayed(longPressRunnable, 500) // 500ms for long press
                    }

                    activity.onElementSelected(selectedMode)
                    invalidate()
                }

                MotionEvent.ACTION_MOVE -> {
                    // If moved significantly, cancel long press
                    val moveDist = sqrt((x-lastTouchX)*(x-lastTouchX) + (y-lastTouchY)*(y-lastTouchY))
                    if (moveDist > 20) handler.removeCallbacks(longPressRunnable)

                    if (selectedMode == "text") {
                        textX = (x + dragOffsetX).coerceIn(0f, width.toFloat())
                        textY = (y + dragOffsetY).coerceIn(0f, height.toFloat())
                    } else if (selectedMode == "box") {
                        boxX = (x + dragOffsetX).coerceIn(0f, width.toFloat())
                        boxY = (y + dragOffsetY).coerceIn(0f, height.toFloat())
                    }

                    // Highlight Dustbin if hovering
                    if (selectedMode != "none") {
                        if (activity.isOverDustbin(x, y)) {
                            // visual feedback could be added here (e.g. scale dustbin)
                        }
                    }
                    invalidate()
                }

                MotionEvent.ACTION_UP -> {
                    handler.removeCallbacks(longPressRunnable)

                    if (selectedMode != "none") {
                        // Check drop
                        if (activity.isOverDustbin(x, y)) {
                            // DELETE
                            if (selectedMode == "text") showText = false
                            if (selectedMode == "box") showBox = false
                            selectedMode = "none"
                            activity.onElementSelected("none")
                            Toast.makeText(context, "Element Removed", Toast.LENGTH_SHORT).show()
                        }
                        activity.setUIVisibility(true) // Restore UI
                    }
                    invalidate()
                }
            }
            return true
        }

        fun saveToPrefs() {
            prefs.edit().apply {
                putInt("bg_color", bgColor)
                if (bgImage != null) {
                    val file = File(filesDir, "custom_bg.jpg")
                    putString("bg_image_uri", Uri.fromFile(file).toString())
                }
                putString("user_quote", currentQuote)
                putString("dot_shape", if(isCircleDot) "circle" else "square")
                putFloat("box_stroke", boxStroke)
                putFloat("quote_size", textQuoteSize)
                putFloat("dot_size_scale", dotSizeScale)
                putFloat("text_x", textX)
                putFloat("text_y", textY)
                putFloat("box_x", boxX)
                putFloat("box_y", boxY)
                putFloat("box_w", boxW)
                putFloat("box_h", boxH)

                // Save Visibility
                putBoolean("show_box", showBox)
                putBoolean("show_text", showText)
                apply()
            }
        }
    }
}