package io.github.marozauartsiom.tvlauncher.system.power

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import io.github.marozauartsiom.tvlauncher.LauncherApp
import kotlinx.coroutines.launch

/**
 * The top bar's sleep timer: one alarm at the chosen time, nothing else. When it goes off the TV is turned
 * off through [PowerService]. Exact when the owner granted SCHEDULE_EXACT_ALARM over adb, otherwise the
 * system may run it a little late.
 */
class SleepTimer(private val context: Context) {
    private val alarms get() = context.getSystemService(AlarmManager::class.java)

    private fun pending(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, SleepTimerReceiver::class.java).setPackage(context.packageName),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(atMillis: Long) {
        val exact = Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms()
        if (exact) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending())
        else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pending())
    }

    fun cancel() = alarms.cancel(pending())
}

/** The sleep timer went off. Not exported: only our own alarm reaches it. */
class SleepTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val graph = (context.applicationContext as? LauncherApp)?.graph ?: return
        val done = goAsync()
        graph.appScope.launch {
            try { graph.settings.setSleepTimer(null) } finally { done.finish() }
        }
        PowerService.powerOff()
    }
}
