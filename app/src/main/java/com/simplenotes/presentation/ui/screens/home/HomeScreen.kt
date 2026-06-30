package com.simplenotes.presentation.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simplenotes.R
import com.simplenotes.ads.AdManager
import com.simplenotes.domain.model.Note
import com.simplenotes.domain.model.TodoItem
import com.simplenotes.presentation.ui.components.AdBanner
import com.simplenotes.presentation.viewmodel.NotesListViewModel
import com.simplenotes.presentation.viewmodel.TodosViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToNoteEditor: (Long) -> Unit,
    onNavigateToNewNote: () -> Unit,
    adManager: AdManager,
    adsEnabled: Boolean = true,
    notesViewModel: NotesListViewModel = hiltViewModel(),
    todosViewModel: TodosViewModel = hiltViewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val notes by notesViewModel.notes.collectAsStateWithLifecycle()
    val todos by todosViewModel.todos.collectAsStateWithLifecycle()
    val newTodoText by todosViewModel.newTodoText.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.home_title), fontWeight = FontWeight.Bold)
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = onNavigateToNewNote) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_note))
                }
            }
        },
        bottomBar = { AdBanner(adManager, adsEnabled = adsEnabled) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text(stringResource(R.string.tab_notes)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text(stringResource(R.string.tab_todos)) }
                )
            }
            when (selectedTab) {
                0 -> NotesTab(
                    notes = notes,
                    onNoteClick = onNavigateToNoteEditor,
                    onDeleteNote = notesViewModel::deleteNote
                )
                1 -> TodosTab(
                    todos = todos,
                    newTodoText = newTodoText,
                    onNewTodoTextChange = todosViewModel::setNewTodoText,
                    onAddTodo = todosViewModel::addTodo,
                    onToggleTodo = todosViewModel::toggleTodo,
                    onDeleteTodo = todosViewModel::deleteTodo,
                    onDeleteCompleted = todosViewModel::deleteCompleted
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotesTab(
    notes: List<Note>,
    onNoteClick: (Long) -> Unit,
    onDeleteNote: (Long) -> Unit
) {
    if (notes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.no_notes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            items(notes, key = { it.id }) { note ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            onDeleteNote(note.id)
                            true
                        } else false
                    }
                )
                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {},
                    enableDismissFromStartToEnd = false
                ) {
                    NoteListItem(note = note, onClick = { onNoteClick(note.id) })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun NoteListItem(note: Note, onClick: () -> Unit) {
    val dateText = remember(note.updatedAt) {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(note.updatedAt))
    }
    val preview = note.body.lineSequence().firstOrNull().orEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                note.title.ifBlank { stringResource(R.string.untitled_note) },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (preview.isNotBlank()) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                dateText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun TodosTab(
    todos: List<TodoItem>,
    newTodoText: String,
    onNewTodoTextChange: (String) -> Unit,
    onAddTodo: () -> Unit,
    onToggleTodo: (TodoItem) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    onDeleteCompleted: () -> Unit
) {
    val hasCompleted = todos.any { it.completed }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newTodoText,
                onValueChange = onNewTodoTextChange,
                label = { Text(stringResource(R.string.add_todo_hint)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            TextButton(onClick = onAddTodo, enabled = newTodoText.isNotBlank()) {
                Text(stringResource(R.string.add))
            }
        }
        if (hasCompleted) {
            TextButton(
                onClick = onDeleteCompleted,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.delete_completed))
            }
        }
        if (todos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.no_todos),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(todos, key = { it.id }) { todo ->
                    TodoListItem(
                        todo = todo,
                        onToggle = { onToggleTodo(todo) },
                        onDelete = { onDeleteTodo(todo.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TodoListItem(
    todo: TodoItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = todo.completed, onCheckedChange = { onToggle() })
            Text(
                todo.text,
                modifier = Modifier.weight(1f),
                style = if (todo.completed) {
                    MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    MaterialTheme.typography.bodyLarge
                }
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}
