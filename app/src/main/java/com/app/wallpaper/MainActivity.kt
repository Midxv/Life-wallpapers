package com.app.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnSetGoal = findViewById<Button>(R.id.btnSetGoal)
        val btnEdit = findViewById<Button>(R.id.btnEdit)
        val btnOpen = findViewById<Button>(R.id.btnOpenWallpaper)

        // 1. Open Goal Setting Popup
        btnSetGoal.setOnClickListener {
            val bottomSheet = GoalBottomSheet()
            bottomSheet.show(supportFragmentManager, "GoalBottomSheet")
        }

        // 2. Open Drag & Drop Editor
        btnEdit.setOnClickListener {
            startActivity(Intent(this, EditorActivity::class.java))
        }

        // 3. Open System Wallpaper Picker
        btnOpen.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, MidwallsService::class.java)
            )
            startActivity(intent)
        }
    }
}