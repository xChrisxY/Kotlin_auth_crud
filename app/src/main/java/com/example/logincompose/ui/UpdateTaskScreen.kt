package com.example.logincompose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.logincompose.repository.TaskRepository
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch


import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun UpdateTaskScreen(
    taskId: String,  // Solo recibimos el ID de la tarea
    onTaskUpdated: () -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val taskRepository = TaskRepository()

    // Estados para los campos del formulario
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") } // Este será invisible
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    // Efecto para cargar los datos de la tarea
    LaunchedEffect(taskId) {
        try {
            taskRepository.getTaskById(
                taskId = taskId,
                onSuccess = { task ->
                    title = task.title
                    description = task.description
                    date = task.date
                    author = task.author
                    isLoading = false
                },
                onError = { error ->
                    errorMessage = "Error al cargar la tarea: $error"
                    println(error)
                    isLoading = false
                }
            )
        } catch (e: Exception) {
            errorMessage = "Error al cargar la tarea: ${e.message}"
            println(e)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Actualizar Tarea", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Descripción") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Fecha (YYYY-MM-DD)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }

            Button(
                onClick = {
                    if (title.isBlank() || description.isBlank() || date.isBlank()) {
                        errorMessage = "Todos los campos son obligatorios"
                        return@Button
                    }

                    isSubmitting = true
                    errorMessage = null

                    scope.launch {
                        taskRepository.updateTask(
                            taskId = taskId,
                            title = title,
                            description = description,
                            date = date,
                            author = author, // Usamos el authorId que obtuvimos al cargar la tarea
                            onSuccess = {
                                onTaskUpdated()
                            },
                            onError = { error ->
                                errorMessage = error
                                isSubmitting = false
                            }
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isSubmitting
            ) {
                Text(text = if (isSubmitting) "Actualizando..." else "Actualizar")
            }
        }
    }
}