package com.example.logincompose.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.logincompose.data.LoginRepository
import com.example.logincompose.ui.CreateTaskScreen
import com.example.logincompose.ui.UpdateTaskScreen
import com.example.logincompose.viewmodel.HomeScreen
import com.example.logincompose.ui.theme.LoginScreen
import com.example.logincompose.ui.TakePhotoScreen
import org.json.JSONObject


@Composable
fun AppNavigation() {

    fun parseUserId(responseBody: String): String {
        return try {
            val jsonObject = JSONObject(responseBody)
            var userObject = jsonObject.getJSONObject("user")
            userObject = userObject.getJSONObject("user")
            userObject.getString("id")
        } catch (e: Exception) {
            throw IllegalArgumentException("No se pudo extraer el ID: ${e.message}")
        }
    }

    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "login") {

        // [+] Pantalla de login
        composable("login") {
            LoginScreen { email, password ->

                LoginRepository.login(
                    email = email,
                    password = password,
                    onSuccess = { responseBody ->
                        val id = parseUserId(responseBody)
                        navController.navigate("home/$email/$id")
                    },
                    onFailure = {
                        println("Error: $it")
                    }
                )
            }
        }

        // [+] Pantall de Inicio (Home)
        composable(
            route = "home/{username}/{id}",
            arguments = listOf(
                navArgument("username") { type = NavType.StringType},
                navArgument("id") { type = NavType.StringType}
            )

        ) { BackStackEntry ->
            val username = BackStackEntry.arguments?.getString("username") ?: ""
            val id = BackStackEntry.arguments?.getString("id") ?: ""
            HomeScreen(
                username = username,
                userId = id,
                onCreateTaskClick = { navController.navigate("createTask/$id") },
                onUpdateTaskClick = { taskId -> navController.navigate("updateTask/$taskId") },
                onTakePhotoClick = { navController.navigate("takephoto")}
            )

        }

        // [+] Pantalla para crear una tarea
        composable(
            route = "createTask/{id}",
            arguments = listOf(
                navArgument("id") { type = NavType.StringType}
            )
        ) { BackStackEntry ->

            val userId = BackStackEntry.arguments?.getString("id") ?: ""

            CreateTaskScreen(
                userId = userId,
                onTaskCreated = { navController.popBackStack() }
            )

        }

        composable(
            route = "updateTask/{taskId}",
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""

            UpdateTaskScreen(
                taskId = taskId, // Necesitas obtener la tarea actual
                onTaskUpdated = { navController.popBackStack() },
                onClose = { navController.popBackStack() }
            )
        }

        // Nueva pantall para tomar foto
        composable("takephoto") {
            TakePhotoScreen(onPhotoTaken = { photoUri ->
                // Aquí puedes manejar la URI de la foto tomada
                navController.popBackStack()
            })
        }

    }

}