package com.renzo.moodlesync.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.renzo.moodlesync.data.Task
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri

object NotificationHelper {

    private const val CHANNEL_ID = "moodle_tasks"
    private const val CHANNEL_NAME = "Tareas Moodle"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de tareas próximas"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun notifyTask(context: Context, task: Task, message: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.vidalibarraquer.net/moodle"))
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(task.title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(task.id.hashCode(), notification)
    }

    fun checkAndNotify(context: Context, tasks: List<Task>) {
        val now = System.currentTimeMillis()
        tasks.forEach { task ->
            val diff = task.dueDate - now
            val hours = diff / (1000 * 60 * 60)
            when {
                hours in 0..2 -> notifyTask(context, task, "⚠️ Entrega en menos de 2 horas!")
                hours in 2..24 -> notifyTask(context, task, "🔴 Entrega en menos de 24 horas")
                hours in 48..168 -> notifyTask(context, task, "🟡 Entrega en ${hours/24} días")
                hours in 24..48 -> notifyTask(context, task, "🟠 Entrega mañana")
            }
        }
    }
}