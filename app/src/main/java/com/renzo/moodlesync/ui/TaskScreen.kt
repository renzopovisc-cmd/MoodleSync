package com.renzo.moodlesync.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.renzo.moodlesync.data.Task
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskScreen(tasks: List<Task>, onToggleCompletada: (Task) -> Unit = {}) {
    var selectedTab by remember { mutableStateOf("Todas") }
    var showCompletadas by remember { mutableStateOf(false) }

    val asignaturas = listOf("Todas") + tasks.map { it.courseName }.distinct().sorted()

    val tareasFiltradas = if (selectedTab == "Todas") tasks else tasks.filter { it.courseName == selectedTab }
    val pendientes = tareasFiltradas.filter { !it.completada }.sortedBy { it.dueDate }
    val completadas = tareasFiltradas.filter { it.completada }.sortedBy { it.dueDate }

    Column(modifier = Modifier.fillMaxSize()) {

        // Header
        Surface(color = MaterialTheme.colorScheme.primary) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "MoodleSync",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White
                )
                Text(
                    text = "${pendientes.size} pendientes · ${completadas.size} completadas",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Pestañas de asignaturas
        LazyRow(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(asignaturas) { asignatura ->
                val shortName = if (asignatura == "Todas") "Todas"
                else asignatura.take(12) + if (asignatura.length > 12) "..." else ""
                FilterChip(
                    selected = selectedTab == asignatura,
                    onClick = { selectedTab = asignatura },
                    label = { Text(shortName) }
                )
            }
        }

        HorizontalDivider()

        // Lista de tareas
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (pendientes.isEmpty() && completadas.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay tareas en esta asignatura")
                    }
                }
            }

            if (pendientes.isNotEmpty()) {
                item {
                    Text(
                        "📋 Pendientes",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(pendientes) { task ->
                    TaskItem(task, onToggleCompletada)
                }
            }

            if (completadas.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = { showCompletadas = !showCompletadas },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(if (showCompletadas) "▼ Completadas (${completadas.size})"
                        else "▶ Completadas (${completadas.size})")
                    }
                }
                if (showCompletadas) {
                    items(completadas) { task ->
                        TaskItem(task, onToggleCompletada)
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItem(task: Task, onToggleCompletada: (Task) -> Unit) {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val now = System.currentTimeMillis()
    val diff = task.dueDate - now
    val hours = diff / (1000 * 60 * 60)

    val cardColor = when {
        task.completada -> Color(0xFF424242)
        diff < 0 -> Color(0xFFB71C1C)
        hours < 24 -> Color(0xFFE53935)
        hours < 72 -> Color(0xFFF57C00)
        hours < 168 -> Color(0xFFF9A825)
        else -> Color(0xFF2E7D32)
    }

    val urgencyText = when {
        task.completada -> "✅ Entregada"
        diff < 0 -> "⚠️ VENCIDA"
        hours < 24 -> "🔴 Menos de 24h"
        hours < 72 -> "🟠 ${hours / 24 + 1} días"
        hours < 168 -> "🟡 Esta semana"
        else -> "🟢 Sin urgencia"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.completada) TextDecoration.LineThrough else null
                    ),
                    color = Color.White
                )
                Text(
                    text = task.courseName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = "Entrega: ${if (task.dueDate > 0) sdf.format(Date(task.dueDate)) else "Sin fecha"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                Text(
                    text = urgencyText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                val context = androidx.compose.ui.platform.LocalContext.current
                TextButton(onClick = {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(
                            if (task.url.isNotEmpty()) task.url
                            else "https://www.vidalibarraquer.net/moodle"
                        )
                    )
                    context.startActivity(intent)
                }) {
                    Text("🔗 Ver en Moodle", color = Color.White)
                }
            }
            Checkbox(
                checked = task.completada,
                onCheckedChange = { onToggleCompletada(task) },
                colors = CheckboxDefaults.colors(
                    checkmarkColor = Color.Black,
                    checkedColor = Color.White,
                    uncheckedColor = Color.White
                )
            )
        }
    }
}