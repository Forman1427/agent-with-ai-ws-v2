package edu.learn.taskprocessor.repository

import edu.learn.taskprocessor.model.Task
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class InMemoryTaskRepository : TaskRepository {

    private val tasks: MutableMap<UUID, Task> = ConcurrentHashMap()

    override fun save(task: Task): Task {
        tasks[task.id] = task
        return task
    }

    override fun update(task: Task): Task {
        if (!tasks.containsKey(task.id)) throw NoSuchElementException("Task with id=${task.id} not found")
        tasks[task.id] = task
        return task
    }

    override fun findById(id: UUID): Task? {
        return tasks[id]
    }

    override fun findByUserName(userName: String): List<Task> {
        return tasks.values.filter { it.userName == userName }.sortedBy { it.createdAt }
    }
}