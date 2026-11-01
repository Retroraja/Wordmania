package com.retroraja.wordnoid.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Short buzzes for big moments. */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }.getOrNull()
    var enabled = true

    fun tap() = buzz(15, 80)
    fun word() = buzz(35, 150)
    fun big() = buzz(80, 255)

    private fun buzz(ms: Long, amp: Int) {
        if (!enabled) return
        runCatching { vibrator?.vibrate(VibrationEffect.createOneShot(ms, amp)) }
    }
}
