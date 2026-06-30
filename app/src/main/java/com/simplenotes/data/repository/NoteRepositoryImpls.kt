package com.simplenotes.data.repository

import com.simplenotes.data.local.database.dao.NoteDao
import com.simplenotes.data.local.database.dao.TodoDao
import com.simplenotes.data.local.database.entity.NoteEntity
import com.simplenotes.data.local.database.entity.TodoEntity
import com.simplenotes.domain.model.Note
import com.simplenotes.domain.model.TodoItem
import com.simplenotes.domain.repository.NoteRepository
import com.simplenotes.domain.repository.TodoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun observeNotes(): Flow<List<Note>> =
        noteDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getNote(id: Long): Note? = noteDao.getById(id)?.toDomain()

    override suspend fun saveNote(note: Note): Long =
        noteDao.upsert(
            NoteEntity(
                id = note.id,
                title = note.title,
                body = note.body,
                updatedAt = System.currentTimeMillis()
            )
        )

    override suspend fun deleteNote(id: Long) {
        noteDao.delete(id)
    }

    private fun NoteEntity.toDomain() = Note(id, title, body, updatedAt)
}

@Singleton
class TodoRepositoryImpl @Inject constructor(
    private val todoDao: TodoDao
) : TodoRepository {

    override fun observeTodos(): Flow<List<TodoItem>> =
        todoDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun addTodo(text: String): Long =
        todoDao.insert(TodoEntity(text = text, completed = false, createdAt = System.currentTimeMillis()))

    override suspend fun setCompleted(id: Long, completed: Boolean) {
        todoDao.setCompleted(id, completed)
    }

    override suspend fun deleteTodo(id: Long) {
        todoDao.delete(id)
    }

    override suspend fun deleteCompleted() {
        todoDao.deleteCompleted()
    }

    private fun TodoEntity.toDomain() = TodoItem(id, text, completed, createdAt)
}
