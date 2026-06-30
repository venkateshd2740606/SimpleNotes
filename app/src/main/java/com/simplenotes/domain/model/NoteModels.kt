package com.simplenotes.domain.model

data class Note(
    val id: Long = 0,
    val title: String,
    val body: String,
    val updatedAt: Long
)

data class TodoItem(
    val id: Long = 0,
    val text: String,
    val completed: Boolean,
    val createdAt: Long
)
