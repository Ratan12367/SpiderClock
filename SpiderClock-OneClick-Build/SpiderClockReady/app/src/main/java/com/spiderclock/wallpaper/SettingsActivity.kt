package com.spiderclock.wallpaper

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        if (savedInstanceState == null) supportFragmentManager.beginTransaction().replace(R.id.settings_container, SettingsFragment()).commit()
        findViewById<android.widget.Button>(R.id.btn_apply_wallpaper).setOnClickListener {
            startActivity(Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this, SpiderWallpaperService::class.java)))
        }
    }
    class SettingsFragment : PreferenceFragmentCompat() { override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) { setPreferencesFromResource(R.xml.preferences, rootKey) } }
}
