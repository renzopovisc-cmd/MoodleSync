package com.renzo.moodlesync.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey
    val id: String,
    val title: String,
    val description: String,
    val dueDate: Long,
    val courseId: String,
    val courseName: String,
    val lastModified: Long,
    val completada: Boolean = false,
    val url: String = ""
)