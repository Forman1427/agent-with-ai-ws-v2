package edu.learn.taskprocessor.repository

import edu.learn.taskprocessor.model.Task
import edu.learn.taskprocessor.model.TaskStatus
import edu.learn.taskprocessor.repository.tables.TasksTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresTaskRepository : TaskRepository {

    override fun save(task: Task): Task = transaction {
        TasksTable.insert {
            it[id] = task.id
            it[userName] = task.userName
            it[taskText] = task.taskText
            it[status] = task.status.name
            it[progress] = task.progress
            it[createdAt] = task.createdAt
            it[completedAt] = task.completedAt
            it[resultText] = task.resultText
        }
        task
    }

    override fun update(task: Task): Task = transaction {
        val updatedRows = TasksTable.update({ TasksTable.id eq task.id }) {
            it[userName] = task.userName
            it[taskText] = task.taskText
            it[status] = task.status.name
            it[progress] = task.progress
            it[createdAt] = task.createdAt
            it[completedAt] = task.completedAt
            it[resultText] = task.resultText
        }

        if (updatedRows == 0) throw NoSuchElementException("Task with id=${task.id} not found")
        task
    }

    override fun findById(id: UUID): Task? = transaction {
        TasksTable
            .selectAll()
            .where { TasksTable.id eq id }
            .map { it.toTask() }
            .singleOrNull()
    }

    override fun findByUserName(userName: String): List<Task> = transaction {
        TasksTable
            .selectAll()
            .where { TasksTable.userName eq userName }
            .orderBy(TasksTable.createdAt to SortOrder.ASC)
            .map { it.toTask() }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toTask(): Task {
        return Task(
            id = this[TasksTable.id].value,
            userName = this[TasksTable.userName],
            taskText = this[TasksTable.taskText],
            status = TaskStatus.valueOf(this[TasksTable.status]),
            progress = this[TasksTable.progress],
            createdAt = this[TasksTable.createdAt],
            completedAt = this[TasksTable.completedAt],
            resultText = this[TasksTable.resultText]
        )
    }
}