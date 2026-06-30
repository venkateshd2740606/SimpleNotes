package com.simplenotes.domain.repository

import com.simplenotes.domain.model.Note
import com.simplenotes.domain.model.TodoItem
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeNotes(): Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun saveNote(note: Note): Long
    suspend fun deleteNote(id: Long)
}

interface TodoRepository {
    fun observeTodos(): Flow<List<TodoItem>>
    suspend fun addTodo(text: String): Long
    suspend fun setCompleted(id: Long, completed: Boolean)
    suspend fun deleteTodo(id: Long)
    suspend fun deleteCompleted()
}
