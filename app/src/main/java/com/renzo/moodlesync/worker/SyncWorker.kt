package com.renzo.moodlesync.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.renzo.moodlesync.data.AppDatabase
import com.renzo.moodlesync.data.Task
import java.net.URL
import java.util.Scanner

class SyncWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val icalUrl = inputData.getString("ical_url") ?: return Result.failure()
            val icalContent = URL(icalUrl).readText()
            val db = AppDatabase.getDatabase(applicationContext)

            // Guardar cuáles están completadas antes de sincronizar
            val completadas = db.taskDao().getAllTasks()
                .filter { it.completada }
                .map { it.id }

            val tasks = parseIcal(icalContent).map { task ->
                if (task.id in completadas) task.copy(completada = true) else task
            }
            db.taskDao().insertAll(tasks)
            Log.d("SyncWorker", "Sincronizadas ${tasks.size} tareas")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Error: ${e.message}")
            Result.retry()
        }
    }

    private fun parseIcal(content: String): List<Task> {
        val tasks = mutableListOf<Task>()
        val events = content.split("BEGIN:VEVENT")
        for (event in events.drop(1)) {
            val uid = extractField(event, "UID") ?: continue
            val summary = extractField(event, "SUMMARY") ?: "Sin título"
            val due = extractField(event, "DTSTART") ?: extractField(event, "DUE") ?: continue
            val description = extractField(event, "DESCRIPTION") ?: ""
            tasks.add(
                Task(
                    id = uid,
                    title = summary,
                    description = description,
                    dueDate = parseIcalDate(due),
                    courseId = "",
                    courseName = extractCourse(summary),
                    lastModified = System.currentTimeMillis()
                )
            )
        }
        return tasks
    }

    private fun extractField(event: String, field: String): String? {
        val line = event.lines().find { it.startsWith("$field:") || it.startsWith("$field;") }
        return line?.substringAfter(":")?.trim()
    }

    private fun parseIcalDate(dateStr: String): Long {
        return try {
            val clean = dateStr.replace("T", "").replace("Z", "")
            val sdf = java.text.SimpleDateFormat("yyyyMMddHHmmss", java.util.Locale.getDefault())
            sdf.parse(clean)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun extractCourse(summary: String): String {
        return if (summary.contains(":")) summary.substringBefore(":").trim() else "General"
    }
}