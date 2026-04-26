package com.renzo.moodlesync

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.renzo.moodlesync.data.AppDatabase
import com.renzo.moodlesync.data.Task
import com.renzo.moodlesync.ui.NotificationHelper
import com.renzo.moodlesync.ui.SplashScreen
import com.renzo.moodlesync.ui.TaskScreen
import com.renzo.moodlesync.ui.theme.MoodleSyncTheme
import com.renzo.moodlesync.worker.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Switch

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Permiso de notificaciones concedido")
        }
    }

    private fun pedirPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pedirPermisoNotificaciones()
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            MoodleSyncTheme(darkTheme = darkMode) {
                val prefs = getSharedPreferences("moodlesync", Context.MODE_PRIVATE)
                var icalUrl by remember { mutableStateOf(prefs.getString("ical_url", "") ?: "") }
                var urlConfigurada by remember { mutableStateOf(icalUrl.isNotEmpty()) }
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else if (!urlConfigurada) {
                    // Pantalla de configuración
                    var inputUrl by remember { mutableStateOf("") }
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("MoodleSync", style = MaterialTheme.typography.headlineMedium)
                        Text("Desarrollado por Renzo Povis", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("📋 Cómo obtener tu URL:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("1️⃣ Entra a tu Moodle del instituto")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("2️⃣ Arriba a la derecha toca el menú → 'Calendario'")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("3️⃣ En el calendario toca 'Importa o exporta calendaris'")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("4️⃣ Luego 'Exporta el calendari'")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("5️⃣ Selecciona 'Esdeveniments relacionats amb els cursos'")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("6️⃣ Selecciona 'Recents i els propers 60 dies'")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("7️⃣ Dale a 'Genera l'URL del calendari'")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("8️⃣ Copia la URL y pégala aquí abajo ⬇️")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = inputUrl,
                            onValueChange = { inputUrl = it },
                            label = { Text("URL iCal de Moodle") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                if (inputUrl.isNotEmpty()) {
                                    prefs.edit().putString("ical_url", inputUrl).apply()
                                    icalUrl = inputUrl
                                    urlConfigurada = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Guardar y continuar")
                        }
                    }
                } else {
                    // Pantalla principal
                    var tasks by remember { mutableStateOf(listOf<Task>()) }
                    var loading by remember { mutableStateOf(true) }
                    var syncing by remember { mutableStateOf(false) }
                    var showSettings by remember { mutableStateOf(false) }

                    suspend fun sync() {
                        syncing = true
                        withContext(Dispatchers.IO) {
                            try {
                                val content = java.net.URL(icalUrl).readText()
                                val db = AppDatabase.getDatabase(applicationContext)
                                val completadas = db.taskDao().getAllTasks()
                                    .filter { it.completada }
                                    .map { it.id }
                                val parsed = parseTasks(content).map { task ->
                                    if (task.id in completadas) task.copy(completada = true) else task
                                }
                                db.taskDao().insertAll(parsed)
                            } catch (e: Exception) {
                                android.util.Log.e("MainActivity", "Error: ${e.message}")
                            }
                        }
                        tasks = loadTasks()
                        NotificationHelper.createChannel(applicationContext)
                        NotificationHelper.checkAndNotify(applicationContext, tasks)
                        loading = false
                        syncing = false
                    }

                    LaunchedEffect(Unit) {
                        scheduleSyncWorker(icalUrl)
                        sync()
                    }

                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = !showSettings,
                                    onClick = { showSettings = false },
                                    icon = { Text("📋") },
                                    label = { Text("Tareas") }
                                )
                                NavigationBarItem(
                                    selected = showSettings,
                                    onClick = { showSettings = true },
                                    icon = { Text("⚙️") },
                                    label = { Text("Ajustes") }

                                )
                            }
                        }
                    ) { paddingValues ->
                        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                            if (showSettings) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🌙 Modo oscuro")
                                        Switch(
                                            checked = darkMode,
                                            onCheckedChange = { darkMode = it }
                                        )
                                    }
                                    Button(
                                        onClick = { if (!syncing) { val scope = kotlinx.coroutines.MainScope(); scope.launch { sync() } } },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (syncing) "Sincronizando..." else "🔄 Sincronizar ahora")
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            prefs.edit().remove("ical_url").apply()
                                            icalUrl = ""
                                            urlConfigurada = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("🔗 Cambiar URL de Moodle")
                                    }
                                    HorizontalDivider()
                                    Text("MoodleSync", style = MaterialTheme.typography.titleMedium)
                                    Text("Desarrollado por Renzo Povis", style = MaterialTheme.typography.bodySmall)
                                    Text("Versión 1.5", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            } else {
                                if (loading) {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            CircularProgressIndicator()
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text("Sincronizando tareas...")
                                        }
                                    }
                                } else {
                                    TaskScreen(
                                        tasks = tasks,
                                        onToggleCompletada = { task ->
                                            kotlinx.coroutines.MainScope().launch {
                                                withContext(Dispatchers.IO) {
                                                    AppDatabase.getDatabase(applicationContext)
                                                        .taskDao()
                                                        .updateCompletada(task.id, !task.completada)
                                                }
                                                tasks = loadTasks()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun scheduleSyncWorker(icalUrl: String) {
        val inputData = Data.Builder()
            .putString("ical_url", icalUrl)
            .build()
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(30, TimeUnit.MINUTES)
            .setInputData(inputData)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "moodle_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun parseTasks(content: String): List<Task> {
        val tasks = mutableListOf<Task>()
        val events = content.split("BEGIN:VEVENT")
        for (event in events.drop(1)) {
            val uid = extractField(event, "UID") ?: continue
            val summary = extractField(event, "SUMMARY") ?: "Sin título"
            val due = extractField(event, "DTSTART") ?: extractField(event, "DUE") ?: continue
            val description = extractField(event, "DESCRIPTION") ?: ""
            val category = extractField(event, "CATEGORIES") ?: "General"
            val taskUrl = extractField(event, "URL") ?: getCursUrl(category)
            tasks.add(Task(
                id = uid,
                title = summary,
                description = description,
                dueDate = parseIcalDate(due),
                courseId = "",
                courseName = getCursName(category),
                lastModified = System.currentTimeMillis(),
                url = taskUrl
            ))
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
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(clean)?.time ?: 0L
        } catch (e: Exception) { 0L }
    }

    private fun getCursName(category: String): String {
        return when {
            category.contains("1709") -> "Itinerario Personal Ocupabilidad"
            category.contains("0373") -> "Lenguajes de Marcas"
            category.contains("0483") -> "Sistemas Informáticos"
            category.contains("0484") -> "Bases de Datos"
            category.contains("0485") -> "Programación DAM"
            category.contains("0487") -> "Entornos de Desarrollo"
            category.contains("0488") -> "Desarrollo de Interfaces"
            category.contains("0491") -> "Sistemas de Gestión Empresarial"
            category.contains("1665") -> "Digitalización Aplicada"
            category.contains("COBOL") -> "Módulo Optativo COBOL"
            category.contains("Tutoria") -> "Tutoría"
            else -> category
        }
    }

    private fun getCursUrl(category: String): String {
        return when {
            category.contains("1709") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4069"
            category.contains("0373") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4058"
            category.contains("0483") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4059"
            category.contains("0484") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4060"
            category.contains("0485") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4061"
            category.contains("0487") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4062"
            category.contains("0488") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4063"
            category.contains("0491") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4066"
            category.contains("1665") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4068"
            category.contains("COBOL") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4073"
            category.contains("Tutoria") -> "https://www.vidalibarraquer.net/moodle/course/view.php?id=4074"
            else -> ""
        }
    }

    private suspend fun loadTasks(): List<Task> {
        return withContext(Dispatchers.IO) {
            AppDatabase.getDatabase(applicationContext).taskDao().getAllTasks()
        }
    }
}