package com.example.promptdispenser.util

import android.content.Context

object Prefs {
    private const val PREFS_NAME = "dispense_prefs"
    private const val KEY_DELAY_SECONDS = "delay_seconds"

    fun getDelaySeconds(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_DELAY_SECONDS, 0)  // Default: no delay
    }

    fun setDelaySeconds(context: Context, seconds: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_DELAY_SECONDS, seconds.coerceAtLeast(0)).apply()
    }
}