package com.example.logincompose.data

import android.os.Handler
import android.os.Looper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.internal.http2.Http2Reader
import java.io.IOException

object LoginRepository {

    private val client = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun login(email: String, password: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {

        var jsonBody = """
            {"username": "$email", "password": "$password"}
        """.trimIndent()

        val requestBody = jsonBody.toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("http://10.0.2.2:8000/api/v1/login/")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                mainHandler.post {
                    onFailure(e.message ?: "Error desconocido")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    var body = response.body?.string() ?: ""
                    mainHandler.post {
                        onSuccess(body)
                    }
                } else {
                    mainHandler.post {
                        onFailure("Error : ${response.code}")
                    }
                }
            }

        })
    }

}