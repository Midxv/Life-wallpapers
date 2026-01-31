package com.app.wallpaper

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class GoalBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_goal, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnWeek = view.findViewById<Button>(R.id.btnWeek)
        val btnMonth = view.findViewById<Button>(R.id.btnMonth)
        val btnYear = view.findViewById<Button>(R.id.btnYear)
        val btnCustom = view.findViewById<Button>(R.id.btnCustom)
        val layoutCustom = view.findViewById<LinearLayout>(R.id.layoutCustom)
        val tvCustomDays = view.findViewById<TextView>(R.id.tvCustomDays)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBarDays)
        val btnApplyCustom = view.findViewById<Button>(R.id.btnApplyCustom)

        // 1. WEEK: Start = Monday of current week
        btnWeek.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.firstDayOfWeek = Calendar.MONDAY
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            resetTime(cal)
            saveGoal(7, cal.timeInMillis)
        }

        // 2. MONTH: Start = 1st of current Month
        btnMonth.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            resetTime(cal)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            saveGoal(maxDays, cal.timeInMillis)
        }

        // 3. YEAR: Start = Jan 1st of current Year
        btnYear.setOnClickListener {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_YEAR, 1)
            resetTime(cal)
            val maxDays = cal.getActualMaximum(Calendar.DAY_OF_YEAR)
            saveGoal(maxDays, cal.timeInMillis)
        }

        // 4. CUSTOM: Start = TODAY (0 progress initially)
        btnCustom.setOnClickListener {
            layoutCustom.visibility = View.VISIBLE
            btnCustom.visibility = View.GONE
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val p = if (progress < 1) 1 else progress
                tvCustomDays.text = "$p Days"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        tvCustomDays.setOnClickListener {
            showManualEntryDialog(tvCustomDays, seekBar)
        }

        btnApplyCustom.setOnClickListener {
            val days = if (seekBar.progress < 1) 1 else seekBar.progress
            val cal = Calendar.getInstance()
            resetTime(cal) // Start counting from today 00:00
            saveGoal(days, cal.timeInMillis)
        }
    }

    private fun resetTime(cal: Calendar) {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
    }

    private fun showManualEntryDialog(tv: TextView, seekBar: SeekBar) {
        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.hint = "Enter number of days"
        AlertDialog.Builder(requireContext())
            .setTitle("Manual Entry")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val str = input.text.toString()
                if (str.isNotEmpty()) {
                    val days = str.toIntOrNull() ?: 1
                    seekBar.progress = days
                    tv.text = "$days Days"
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveGoal(days: Int, startTimeMillis: Long) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        prefs.edit().apply {
            putInt("goal_duration", days)
            putLong("goal_start_time", startTimeMillis)
            apply()
        }
        Toast.makeText(requireContext(), "Goal Updated!", Toast.LENGTH_SHORT).show()
        dismiss()
    }
}