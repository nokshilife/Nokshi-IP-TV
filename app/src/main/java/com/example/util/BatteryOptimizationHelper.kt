package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher

object BatteryOptimizationHelper {

    private const val TAG = "BatteryOptHelper"

    /**
     * Returns true if the app is already Unrestricted / Excluded from Android's Doze & App Standby
     * battery optimizations (or if running below Android M).
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManager?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query battery optimization status", e)
            false
        }
    }

    /**
     * Launches the official Android system prompt (`ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`)
     * to exclude this app from Battery Optimization (Unrestricted background usage).
     * Falls back gracefully to Battery Optimization list or App Info settings if an OEM/TV ROM
     * blocks the direct package dialog.
     */
    fun requestIgnoreBatteryOptimizations(
        context: Context,
        launcher: ActivityResultLauncher<Intent>? = null
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return false

        val packageUri = Uri.parse("package:${context.packageName}")

        // 1. Primary: Direct Android System Prompt ("Allow Nokshi IP TV to always run in background?")
        try {
            val directIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = packageUri
                if (launcher == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            if (directIntent.resolveActivity(context.packageManager) != null) {
                if (launcher != null) {
                    launcher.launch(directIntent)
                } else {
                    context.startActivity(directIntent)
                }
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Direct ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS failed, trying fallback", e)
        }

        // 2. Fallback: System Battery Optimization Settings screen
        try {
            val listIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                if (launcher == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            if (listIntent.resolveActivity(context.packageManager) != null) {
                if (launcher != null) {
                    launcher.launch(listIntent)
                } else {
                    context.startActivity(listIntent)
                }
                return true
            }
        } catch (e: Exception) {
            Log.w(TAG, "ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS fallback failed", e)
        }

        // 3. Final fallback: Application Details Settings (where user can select Battery -> Unrestricted)
        return try {
            val appDetailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = packageUri
                if (launcher == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            if (appDetailsIntent.resolveActivity(context.packageManager) != null) {
                if (launcher != null) {
                    launcher.launch(appDetailsIntent)
                } else {
                    context.startActivity(appDetailsIntent)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "App details settings fallback failed", e)
            false
        }
    }
}
