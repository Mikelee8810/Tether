package com.relationshipradar.app.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * High-precision tactile haptic feedback engine for Tether.
 * Provides physical feedback tailored for neurodivergent sensory satisfaction.
 */
object Haptics {

    fun tick(context: Context) {
        vibrate(context, VibrationEffect.EFFECT_TICK, fallbackDurationMs = 10)
    }

    fun click(context: Context) {
        vibrate(context, VibrationEffect.EFFECT_CLICK, fallbackDurationMs = 20)
    }

    fun crunch(context: Context) {
        vibrate(context, VibrationEffect.EFFECT_HEAVY_CLICK, fallbackDurationMs = 50)
    }

    fun celebrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = manager?.defaultVibrator
                val wave = VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 40, 60, 70), intArrayOf(0, 180, 0, 220, 0, 255), -1)
                vibrator?.vibrate(wave)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                val wave = VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 40, 60, 70), intArrayOf(0, 180, 0, 220, 0, 255), -1)
                vibrator?.vibrate(wave)
            }
        } catch (_: Exception) {
            crunch(context)
        }
    }

    private fun vibrate(context: Context, predefinedEffect: Int, fallbackDurationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = manager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(predefinedEffect))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(VibrationEffect.createPredefined(predefinedEffect))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(fallbackDurationMs)
            }
        } catch (_: Exception) {
            // Silently fallback if hardware vibration is unavailable or permission denied
        }
    }
}
