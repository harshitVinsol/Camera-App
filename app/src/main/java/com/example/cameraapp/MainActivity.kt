package com.example.cameraapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.Exception
import java.util.concurrent.Executors

private const val REQUEST_CODE_PERMISSIONS = 10
/*
This is an array of all the permission specified in the manifest.
 */
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
/*
MainActivity to display the Camera Preview with a simple button to capture the image
 */
class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Request camera permissions
        if (allPermissionsGranted()) {
            textureView.post {
                startCamera()
            }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }
    /*
    Function startCamera() to instruct the system to start the camera
     */
    private fun startCamera() {
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(textureView.width, textureView.height))
        }.build()

        // Build the viewfinder use case
        val preview = Preview(previewConfig)

        // Every time the viewfinder is updated, recompute layout
        preview.setOnPreviewOutputUpdateListener {

            // To update the SurfaceTexture, we have to remove it and re-add it
            val parent = textureView.parent as ViewGroup
            parent.removeView(textureView)
            parent.addView(textureView, 0)

            textureView.surfaceTexture = it.surfaceTexture
        }
        // Create configuration object for the image capture use case
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)
            }.build()

        // Build the image capture use case and attach button click listener
        val imageCapture = ImageCapture(imageCaptureConfig)
        findViewById<Button>(R.id.capture).setOnClickListener {
            val file = File(externalMediaDirs.first(),
                "${System.currentTimeMillis()}.jpg")

            imageCapture.takePicture(file, executor,
                object : ImageCapture.OnImageSavedListener {
                    override fun onError(
                        imageCaptureError: ImageCapture.ImageCaptureError,
                        message: String,
                        exc: Throwable?
                    ) {
                        val msg = "Photo capture failed: $message"

                        textureView.post {
                            val snack = Snackbar.make(it,msg,Snackbar.LENGTH_LONG)
                            snack.show()
                        }
                    }

                    override fun onImageSaved(file: File) {
                        val photoPath = file.absolutePath
                        val msg = "Photo capture succeeded"

                        textureView.post {
                            val snack = Snackbar.make(it,msg,Snackbar.LENGTH_LONG)
                                .setAction("OPEN"
                                ){
                                    val uri = Uri.fromFile(File(photoPath))
                                    showImage()
                                    imageView.setImageURI(uri)
                                }
                            snack.show()
                        }
                    }
                })
        }
        CameraX.bindToLifecycle(this, preview, imageCapture)

        close_button.setOnClickListener {
            closeImage()
        }
    }
    /*
    A function to show ImageView and Close Button
     */
    private fun showImage(){
        textureView.isInvisible = true
        imageView.isInvisible = false
        capture.isInvisible = true
        close_button.isInvisible = false
    }
    /*
    A function to show Preview and Capture Button
     */
    private fun closeImage(){
        textureView.isInvisible = false
        imageView.isInvisible = true
        capture.isInvisible = false
        close_button.isInvisible = true
    }
    /*
     Process result from permission request dialog box, has the request been granted? If yes, start Camera. Otherwise display a toast
     */
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                textureView.post {
                    startCamera()
                }
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
    /*
     Check if all permission specified in the manifest have been granted
     */
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
}