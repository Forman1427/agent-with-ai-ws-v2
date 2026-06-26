package edu.learn.taskprocessor.repository

import edu.learn.taskprocessor.model.Task
import java.util.UUID

/**
 * Интерфейс (контракт) репозитория задач.
 * Методы: save, findById, findByUserName, update.
 */
interface TaskRepository {
    fun save(task: Task): Task
    fun update(task: Task): Task
    fun findById(id: UUID): Task?
    fun findByUserName(userName: String): List<Task>
}