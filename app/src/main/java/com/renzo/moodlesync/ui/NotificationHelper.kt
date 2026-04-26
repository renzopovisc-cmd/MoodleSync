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

    fun notifyTask(context: Context, task: Task, message: String, franja: Int = 0) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
            if (task.url.isNotEmpty()) task.url else "https://www.vidalibarraquer.net/moodle"
        ))
        val pendingIntent = PendingIntent.getActivity(
            context, franja, intent,
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
        manager.notify("${task.id}-$franja".hashCode(), notification)
    }
    fun checkAndNotify(context: Context, tasks: List<Task>) {
        val now = System.currentTimeMillis()
        tasks.filter { !it.completada }.sortedByDescending { it.dueDate }.forEach { task ->
            val diff = task.dueDate - now
            val hours = diff / (1000 * 60 * 60)
            when {
                diff < 0 -> notifyTask(context, task, "⚠️ Tarea VENCIDA — entrégala ya", 0)
                hours < 1 -> notifyTask(context, task, "🚨 ¡Menos de 1 hora para entregar!", 1)
                hours < 2 -> notifyTask(context, task, "🔴 ¡Solo quedan 2 horas!", 2)
                hours < 3 -> notifyTask(context, task, "🔴 Quedan menos de 3 horas", 3)
                hours < 6 -> notifyTask(context, task, "🟠 Quedan menos de 6 horas", 4)
                hours < 12 -> notifyTask(context, task, "🟠 Quedan menos de 12 horas", 5)
                hours < 24 -> notifyTask(context, task, "🟡 Entrega mañana — menos de 24h", 6)
                hours < 48 -> notifyTask(context, task, "🟡 Entrega en 2 días", 7)
                hours < 72 -> notifyTask(context, task, "🟠 Entrega en 3 días", 8)
                hours < 96 -> notifyTask(context, task, "🟡 Entrega en 4 días", 9)
                hours < 120 -> notifyTask(context, task, "🟡 Entrega en 5 días", 10)
                hours < 144 -> notifyTask(context, task, "🟡 Entrega en 6 días", 11)
                hours < 168 -> notifyTask(context, task, "🟢 Entrega esta semana", 12)
            }
        }
    }
}