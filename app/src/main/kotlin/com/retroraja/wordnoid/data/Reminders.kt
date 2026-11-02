package com.retroraja.wordnoid.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.retroraja.wordnoid.MainActivity
import com.retroraja.wordnoid.R
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Friendly local reminders (no server needed): spin ready, gift waiting, bricks to spend, new challenges,
 * and "Tiko misses you" after 2-3 days away. At most 2 a day, never at night (9 pm to 9 am), and they can be
 * switched off in Settings.
 */
object Reminders {
    private const val CHANNEL = "tiko"
    private const val WORK = "wordnoid_reminders"

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(4, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun post(context: Context, text: String) {
        if (!canPost(context)) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "Tiko's reminders", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Wordnoid")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(1, n) } catch (_: SecurityException) {}
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = ctx.getSharedPreferences("wordnoid_v2", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notifications", true)) return Result.success()
        val hour = LocalTime.now().hour
        if (hour < 9 || hour >= 21) return Result.success()
        val today = LocalDate.now().toEpochDay()
        val sentToday = if (prefs.getLong("notif_day", -1) == today) prefs.getInt("notif_count", 0) else 0
        if (sentToday >= 2) return Result.success()
        val lastClose = prefs.getLong("last_close", 0)
        if (lastClose == 0L || System.currentTimeMillis() - lastClose < 3 * 3600_000L) return Result.success()
        val p = Progress(ctx)
        val daysAway = (System.currentTimeMillis() - lastClose) / 86_400_000L
        val used = prefs.getStringSet("notif_kinds_$today", emptySet()) ?: emptySet()
        val options = buildList {
            if (daysAway >= 2) add("miss" to "Tiko misses you! The town is waiting for its builder. 🏗️")
            if (p.freeSpinReady) add("spin" to "Your free Daily Spin is ready! 🎡")
            if (p.dailyGiftDay() > 0) add("gift" to "A Daily Gift is waiting for you 🎁")
            if (p.bricksToSpend >= 3) add("bricks" to "You have ${p.bricksToSpend} Letter Bricks to build with! 🧱")
            if (p.challengeProgress(0) == 0) add("challenge" to "3 new Daily Challenges are here! ⭐")
        }.filter { it.first !in used }
        val pick = options.firstOrNull() ?: return Result.success()
        Reminders.post(ctx, pick.second)
        prefs.edit()
            .putLong("notif_day", today).putInt("notif_count", sentToday + 1)
            .putStringSet("notif_kinds_$today", used + pick.first)
            .apply()
        return Result.success()
    }
}
