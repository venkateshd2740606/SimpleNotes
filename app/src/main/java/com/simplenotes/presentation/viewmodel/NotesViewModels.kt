package com.simplenotes.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.simplenotes.domain.model.Note
import com.simplenotes.domain.model.TodoItem
import com.simplenotes.domain.repository.NoteRepository
import com.simplenotes.domain.repository.TodoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesHomeViewModel @Inject constructor(
    noteRepository: NoteRepository,
    todoRepository: TodoRepository
) : ViewModel() {
    val notes = noteRepository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val todos = todoRepository.observeTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@HiltViewModel
class NotesListViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    val notes = noteRepository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteNote(id: Long) {
        viewModelScope.launch { noteRepository.deleteNote(id) }
    }
}

@HiltViewModel
class NoteEditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val noteId: Long = savedStateHandle.get<Long>("noteId") ?: 0L

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _body = MutableStateFlow("")
    val body: StateFlow<String> = _body.asStateFlow()

    private var loaded = false

    init {
        if (noteId > 0) {
            viewModelScope.launch {
                noteRepository.getNote(noteId)?.let { note ->
                    _title.value = note.title
                    _body.value = note.body
                    loaded = true
                }
            }
        } else {
            loaded = true
        }
    }

    fun setTitle(value: String) { _title.value = value }
    fun setBody(value: String) { _body.value = value }

    fun save() {
        if (!loaded) return
        viewModelScope.launch {
            noteRepository.saveNote(
                Note(
                    id = noteId,
                    title = _title.value.trim(),
                    body = _body.value,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }
}

@HiltViewModel
class TodosViewModel @Inject constructor(
    private val todoRepository: TodoRepository
) : ViewModel() {
    val todos = todoRepository.observeTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _newTodoText = MutableStateFlow("")
    val newTodoText: StateFlow<String> = _newTodoText.asStateFlow()

    fun setNewTodoText(text: String) { _newTodoText.value = text }

    fun addTodo() {
        val text = _newTodoText.value.trim()
        if (text.isBlank()) return
        viewModelScope.launch {
            todoRepository.addTodo(text)
            _newTodoText.value = ""
        }
    }

    fun toggleTodo(todo: TodoItem) {
        viewModelScope.launch { todoRepository.setCompleted(todo.id, !todo.completed) }
    }

    fun deleteTodo(id: Long) {
        viewModelScope.launch { todoRepository.deleteTodo(id) }
    }

    fun deleteCompleted() {
        viewModelScope.launch { todoRepository.deleteCompleted() }
    }
}
