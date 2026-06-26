package edu.learn.taskprocessor.repository.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object TasksTable : UUIDTable("tasks") {
    val userName = varchar("user_name", 100)
    val taskText = text("task_text")
    val status = varchar("status", 30)
    val progress = integer("progress")
    val createdAt = datetime("created_at")
    val completedAt = datetime("completed_at").nullable()
    val resultText = text("result_text").nullable()
}