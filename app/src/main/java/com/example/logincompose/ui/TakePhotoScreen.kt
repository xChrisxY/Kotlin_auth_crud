package com.example.logincompose.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.Manifest
import android.content.Context
import android.os.Looper
import android.os.Handler

import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import android.os.Environment
import java.io.FileInputStream
import java.io.FileOutputStream
import com.example.logincompose.cloudinary.CloudinaryApi;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory

import com.example.logincompose.model.CloudinaryResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@Composable
fun RequestCameraPermission(onPermissionGranted: @Composable () -> Unit) {
    val context = LocalContext.current

    var permissionGranted by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    if (permissionGranted) {
        onPermissionGranted()
    }
}



@Composable
fun TakePhotoScreen(onPhotoTaken: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val outputDirectory = remember { getOutputDirectory(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    RequestCameraPermission {
        Box(modifier = Modifier.fillMaxSize()) {
            val previewView = remember { PreviewView(context) }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            ) {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder().build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }

            Button(
                onClick = {
                    val photoFile = File(
                        outputDirectory,
                        "${System.currentTimeMillis()}.jpg"
                    )
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                    imageCapture?.takePicture(
                        outputOptions,
                        cameraExecutor,
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val savedUri = photoFile.toURI().toString()
                                Log.d("Camera", "Photo saved to: $savedUri")

                                // [+] Ejecutamos en el hilo principal
                                Handler(Looper.getMainLooper()).post {
                                    savePhotoToPublicDirectory(context, photoFile)
                                    uploadToCloudinary(context, photoFile) { imageUrl ->
                                        if (imageUrl != null) {
                                            Log.d("Cloudinary", "Uploaded successfully: $imageUrl")
                                            onPhotoTaken(imageUrl)
                                        } else {
                                            Log.e("Cloudinary", "Failed to upload image")
                                        }
                                    }
                                }

                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = "Tomar Foto")
            }
        }
    }
}

fun getOutputDirectory(context: Context): File {
    println("")
    return context.filesDir
}

fun savePhotoToPublicDirectory(context: Context, sourceFile: File) {
    val publicDir = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "LoginCompose"
    )

    if (!publicDir.exists()) {
        publicDir.mkdirs()
    }

    val destinationFile = File(publicDir, "login_image.jpg")

    try {
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        }
        Toast.makeText(context, "Foto guardada en: ${destinationFile.absolutePath}", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Error al guardar la foto", Toast.LENGTH_SHORT).show()
    }
}

fun createCloudinaryClient(): CloudinaryApi {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.cloudinary.com/v1_1/dv01nd8nv/") // Reemplaza con tu cloud name
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    return retrofit.create(CloudinaryApi::class.java)
}

fun uploadToCloudinary(context: Context, file: File, onUploadComplete: (String?) -> Unit) {
    val requestBody = file.asRequestBody("image/*".toMediaTypeOrNull())
    val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)
    val uploadPreset = "unsigned_preset".toRequestBody("text/plain".toMediaTypeOrNull())
    print(uploadPreset)

    val api = createCloudinaryClient()

    api.uploadImage(filePart, uploadPreset).enqueue(object : Callback<CloudinaryResponse> {
        override fun onResponse(
            call: Call<CloudinaryResponse>,
            response: Response<CloudinaryResponse>
        ) {
            if (response.isSuccessful) {
                val url = response.body()?.secureUrl
                Log.d("Cloudinary", "Uploaded to: $url")
                onUploadComplete(url)
            } else {
                Log.e("[+] Error ||", "Aquí está el error")
                Log.e("Cloudinary", "Upload failed: ${response.errorBody()?.string()}")
                Log.e("[+] Error ||", "Aquí está el error")
                onUploadComplete(null)
            }
        }

        override fun onFailure(call: Call<CloudinaryResponse>, t: Throwable) {
            t.printStackTrace()
            Toast.makeText(context, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
            onUploadComplete(null)
        }
    })
}



