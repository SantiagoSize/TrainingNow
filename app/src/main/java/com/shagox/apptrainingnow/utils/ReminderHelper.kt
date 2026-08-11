package com.shagox.apptrainingnow.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.shagox.apptrainingnow.ReminderReceiver
import java.util.Calendar

/**
 * Gestiona el recordatorio diario de rutina: preferencias, programación y cancelación de alarmas.
 * La hora se configura con pulsación larga en el botón Notificaciones; el tap activa/desactiva.
 */
object ReminderHelper {

    private const val PREFS_NAME = "reminder_prefs"
    private const val KEY_ENABLED = "routine_reminder_enabled"
    private const val KEY_HOUR = "routine_reminder_hour"
    private const val KEY_MINUTE = "routine_reminder_minute"
    private const val KEY_USER_ID = "routine_reminder_user_id"
    private const val REQUEST_CODE_ALARM = 2001
    /** Base para los códigos de las alarmas por día (se suma el id del día). */
    private const val REQUEST_CODE_DAY_BASE = 30000
    const val EXTRA_DAY_ID = "extra_reminder_day_id"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
    }

    fun getHour(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_HOUR, 8).coerceIn(0, 23)
    }

    fun getMinute(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_MINUTE, 0).coerceIn(0, 59)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun saveTime(context: Context, hour: Int, minute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putInt(KEY_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun saveUserId(context: Context, userId: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(KEY_USER_ID, userId).apply()
    }

    /**
     * Programa la alarma para la próxima ocurrencia de (hour, minute).
     * Guarda userId para que el receiver inserte la notificación y pueda reprogramar.
     */
    fun schedule(context: Context, userId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ENABLED, true).putInt(KEY_USER_ID, userId).apply()
        val hour = prefs.getInt(KEY_HOUR, 8).coerceIn(0, 23)
        val minute = prefs.getInt(KEY_MINUTE, 0).coerceIn(0, 59)

        val triggerTime = nextTriggerMillis(hour, minute)
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent, flags)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
        } else {
            @Suppress("DEPRECATION")
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pending)
        }
    }

    /**
     * Cancela la alarma y marca el recordatorio como desactivado.
     */
    fun cancel(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, false).apply()
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent, flags)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }

    /**
     * Programa el recordatorio de un día concreto de la semana.
     * @param dayOrder 0 = lunes ... 6 = domingo
     * Se repite cada semana a la hora indicada.
     */
    fun scheduleForDay(context: Context, userId: Int, dayId: Int, dayOrder: Int, hour: Int, minute: Int) {
        saveUserId(context, userId)
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_DAY_ID, dayId)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE_DAY_BASE + dayId, intent, flags)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val triggerTime = nextTriggerForWeekday(dayOrder, hour, minute)
        val semana = 7L * 24 * 60 * 60 * 1000
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime, semana, pending)
    }

    /** Cancela el recordatorio propio de un día. */
    fun cancelForDay(context: Context, dayId: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pending = PendingIntent.getBroadcast(context, REQUEST_CODE_DAY_BASE + dayId, intent, flags)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
    }

    /**
     * Próxima ocurrencia de un día de la semana a cierta hora.
     * @param dayOrder 0 = lunes ... 6 = domingo
     */
    fun nextTriggerForWeekday(dayOrder: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        // Calendar.MONDAY = 2 ... DOMINGO = 1
        val objetivo = when (dayOrder.coerceIn(0, 6)) {
            6 -> Calendar.SUNDAY
            else -> Calendar.MONDAY + dayOrder
        }
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var diferencia = objetivo - cal.get(Calendar.DAY_OF_WEEK)
        if (diferencia < 0) diferencia += 7
        cal.add(Calendar.DAY_OF_YEAR, diferencia)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 7)
        }
        return cal.timeInMillis
    }

    /** Devuelve el timestamp (RTC) para la próxima ocurrencia de hour:minute. */
    fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    fun getUserId(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_USER_ID, 0)
    }
}
