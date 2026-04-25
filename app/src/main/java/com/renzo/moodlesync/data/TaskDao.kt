package com.renzo.moodlesync.data

import androidx.room.*

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    suspend fun getAllTasks(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<Task>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: String): Task?

    @Query("UPDATE tasks SET completada = :completada WHERE id = :taskId")
    suspend fun updateCompletada(taskId: String, completada: Boolean)
}