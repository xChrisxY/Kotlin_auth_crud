package com.example.logincompose.viewmodel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.logincompose.model.Task
import com.example.logincompose.repository.TaskRepository
import com.example.logincompose.ui.TaskCard
import kotlinx.coroutines.launch


@Composable
fun HomeScreen(username: String, userId : String, onCreateTaskClick: () -> Unit, onUpdateTaskClick: (String) -> Unit) {

    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val myobject = TaskRepository()

    LaunchedEffect(userId) {
        try {

            val response = myobject.fetchTasksForUser(userId)
            tasks = response
        } catch (e: Exception) {
            errorMessage = "Error al obtener las tareas: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Bienvenido $username", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onCreateTaskClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Crear una nueva tarea")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
        } else if (tasks.isEmpty()) {
            Text(text = "No tienes tareas pendientes")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks) { task ->
                    TaskCard(
                        task = task,
                        onUpdate = {
                            onUpdateTaskClick(task.id)
                        },
                        // eliminando una tarea.
                        onDelete = {
                            myobject.deleteTask(
                                task,
                                onSuccess = {
                                    tasks = tasks.filter { it.id != task.id }
                                },
                                onError = { message ->
                                    errorMessage = message
                                }
                            )
                        }
                    )
                }
            }
        }

    }
}
