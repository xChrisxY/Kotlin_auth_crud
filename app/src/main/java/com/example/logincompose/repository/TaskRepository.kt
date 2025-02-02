package com.example.logincompose.repository
import okhttp3.MediaType.Companion.toMediaType
import com.example.logincompose.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class TaskRepository {

    private val client = OkHttpClient

    suspend fun fetchTasksForUser(userId: String): List<Task> {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/api/v1/tasks/user/${userId}/")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: throw IllegalStateException("Cuerpo vacío")
                parseTasks(responseBody)
            } else {
                throw IllegalStateException("Error en la petición: ${response.code}")
            }
        }
    }

    fun parseTasks(responseBody: String): List<Task> {
        val jsonArray = JSONArray(responseBody)
        val tasks = mutableListOf<Task>()

        for (i in 0 until jsonArray.length()) {
            val taskObject = jsonArray.getJSONObject(i)
            val taskName = taskObject.getString("title")
            val description = taskObject.getString("description")
            val date = taskObject.getString("date")
            val id = taskObject.getString("id")
            val author = taskObject.getString("author")

            val taskDescription = Task(title = taskName, description = description, date = date, id = id, author = author)

            tasks.add(taskDescription)
        }

        return tasks
    }


    // [+] Eliminando tarea
    fun deleteTask(task: Task, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("http://10.0.2.2:8000/api/v1/tasks/${task.id}/")
            .delete()
            .build()

        Thread {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onError("Error al eliminar la tarea: ${response.code}")
                }
            } catch (e: Exception) {
                onError("Error en la conexión: ${e.message}")
            }
        }.start()
    }

    // [+] Crear una tarea
    suspend fun createTask(title: String, description: String, date: String, authorId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val client = OkHttpClient()

        val url = "http://10.0.2.2:8000/api/v1/tasks/create_task/"

        val json = JSONObject().apply {
            put("title", title)
            put("description", description)
            put("date", date)
            put("author", authorId)
        }

        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        try {

             withContext(Dispatchers.IO) {
                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) { // onsuccess se tiene que ejecutar en el hilo principal
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) { // también onerror
                        onError("errorBody")
                    }

                }

            }
        } catch (e: Exception) {
            println(e)
            onError(e.message ?: "Error desconocido")
        }

    }

    // [+] Actualizar una tarea
    suspend fun updateTask(
        taskId: String,
        title: String,
        description: String,
        date: String,
        author: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {

        val client = OkHttpClient()
        withContext(Dispatchers.IO) {
            try {
                val json = """
                    {
                        "title": "$title",
                        "description": "$description",
                        "date": "$date",
                        "author": "$author"
                    }
                """.trimIndent()

                val body = json.toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url("http://10.0.2.2:8000/api/v1/tasks/$taskId/")
                    .put(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            onSuccess()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError("Error al actualizar la tarea: ${response.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Error de conexión: ${e.message}")
                }
            }
        }
    }

    // [+] Obtener una tarea por su ID
    suspend fun getTaskById(
        taskId: String,
        onSuccess: (Task) -> Unit,
        onError: (String) -> Unit = {}
    ) {

        val client = OkHttpClient()
        try {
            withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("http://10.0.2.2:8000/api/v1/tasks/$taskId/")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->

                    val responseBody = response.body?.string()
                    println("Response Code: ${response.code}")
                    println("Response Body: $responseBody")

                    if (response.isSuccessful && responseBody != null) {
                        val jsonObject = JSONObject(responseBody)
                        println("Aquí pasas")
                        println(jsonObject)

                        val task = Task(
                            id = jsonObject.getString("id"),
                            title = jsonObject.getString("title"),
                            description = jsonObject.getString("description"),
                            date = jsonObject.getString("date"),
                            author = jsonObject.getString("author")
                        )

                        withContext(Dispatchers.Main) {
                            onSuccess(task)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError("Error al obtener la tarea: ${response}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Error de conexión: ${e.message}")
            }
        }
    }


}