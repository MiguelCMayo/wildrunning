package com.migueldev.wildrunning

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.media.Image
import android.media.MediaScannerConnection
import android.media.VolumeShaper
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storageMetadata
import com.migueldev.wildrunning.LoginActivity.Companion.useremail
import com.migueldev.wildrunning.MainActivity.Companion.countPhotos
import com.migueldev.wildrunning.MainActivity.Companion.lastimage
import com.migueldev.wildrunning.databinding.ActivityCamaraBinding
import java.io.File
import java.lang.Exception

import kotlin.math.max
import kotlin.math.min
import kotlin.math.abs
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Camara : AppCompatActivity() {

    companion object{
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val REQUEST_CODE_PERMISSIONS = 10

        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
    }

    private var FILENAME : String = ""
    lateinit var binding : ActivityCamaraBinding

    private var preview: Preview? = null

    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var cameraProvider: ProcessCameraProvider? = null

    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File

    private lateinit var cameraExecutor: ExecutorService

    private lateinit var dateRun: String
    private lateinit var startTimeRun: String

    private lateinit var metadata: StorageMetadata



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityCamaraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.extras
        dateRun = bundle?.getString("dateRun").toString()
        startTimeRun = bundle?.getString("startTimeRun").toString()

        cameraExecutor = Executors.newSingleThreadExecutor()

        outputDirectory = getOutputDirectory()
        binding.cameraCaptureButton.setOnClickListener {  takePhoto() }

        binding.cameraSwitchButton.setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing){
                CameraSelector.LENS_FACING_BACK
            }else{
                CameraSelector.LENS_FACING_FRONT
            }
            bindCamera()
        }

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)

    }

    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS){
            if (allPermissionsGranted()) startCamera()
            else{
                Toast.makeText(this, "Debes proporcionar permisos si quieres tomar fotos", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all{
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }


    private fun bindCamera(){

        val metrics = DisplayMetrics().also { binding.viewFinder.display.getRealMetrics(it) }
        val screenAspectRatio = aspectRadio(metrics.widthPixels, metrics.heightPixels)
        val rotation = binding.viewFinder.display.rotation

        val cameraProvider = cameraProvider ?: throw IllegalStateException("Fallo al iniciar la camara")

        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        preview = Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build()

        cameraProvider.unbindAll()

        try{
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            preview?.setSurfaceProvider(binding.viewFinder.surfaceProvider)
        }catch(exc: Exception){
            Log.e("CameraWildRunning", "Fallo al vincular la camara", exc)
        }


    }
    private fun aspectRadio(width: Int, height: Int): Int{
        val previewRatio = max(width, height).toDouble() / min(width, height)

        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)){
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9


    }
    private fun startCamera(){
        val cameraProviderFinnaly = ProcessCameraProvider.getInstance(this)
        cameraProviderFinnaly.addListener(Runnable {

            cameraProvider = cameraProviderFinnaly.get()

            lensFacing = when{
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("No tenemos camara")
            }

            manageSwitchButton()

            bindCamera()

        }, ContextCompat.getMainExecutor(this))


    }

    private fun hasBackCamera(): Boolean{
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }
    private fun hasFrontCamera(): Boolean{
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }
    private fun manageSwitchButton(){
        val switchButton = binding.cameraSwitchButton
        try {
            switchButton.isEnabled = hasBackCamera() && hasFrontCamera()

        }catch (exc: CameraInfoUnavailableException){
            switchButton.isEnabled = false
        }

    }


    private fun getOutputDirectory(): File{
        val mediaDir = externalMediaDirs.firstOrNull()?.let{
            File(it, "wildRunning").apply {  mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists()) mediaDir else filesDir

    }
    private fun takePhoto(){
        FILENAME = getString(R.string.app_name) + useremail + dateRun + startTimeRun
        FILENAME = FILENAME.replace(":", "")
        FILENAME = FILENAME.replace("/", "")

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
            metadata = storageMetadata {
                contentType = "image/jpg"
                setCustomMetadata("orientation", "horizontal")
            }
        else
            metadata = storageMetadata {
                contentType = "image/jpg"
                setCustomMetadata("orientation", "vertical")
            }


        val photoFile = File (outputDirectory, FILENAME + ".jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object:ImageCapture.OnImageSavedCallback{
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {

                    val  savedUri = Uri.fromFile(photoFile)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        setGalleryThumbnail (savedUri)
                    }

                    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(savedUri.toFile().extension)
                    MediaScannerConnection.scanFile(
                        baseContext,
                        arrayOf(savedUri.toFile().absolutePath),
                        arrayOf(mimeType)
                    ){ _, uri ->

                    }

                    var clMain = findViewById<ConstraintLayout>(R.id.clMain)
                    Snackbar.make(clMain, "Imagen guardada con ??xito", Snackbar.LENGTH_LONG).setAction("OK"){
                        clMain.setBackgroundColor(Color.CYAN)
                    }.show()

                    upLoadFile(photoFile)

                }

                override fun onError(exception: ImageCaptureException) {
                    var clMain = findViewById<ConstraintLayout>(R.id.clMain)
                    Snackbar.make(clMain, "Error al guardar la imagen", Snackbar.LENGTH_LONG).setAction("OK"){
                        clMain.setBackgroundColor(Color.CYAN)
                    }.show()
                }
            })
    }
    private fun setGalleryThumbnail(uri: Uri){
        var thumbnail = binding.photoViewButton
        thumbnail.post {
            Glide.with (thumbnail)
                .load(uri)
                .apply(RequestOptions.circleCropTransform())
                .into(thumbnail)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun upLoadFile(image: File){
        var dirName = dateRun + startTimeRun
        dirName = dirName.replace(":", "")
        dirName = dirName.replace("/", "")


        var fileName = dirName + "-" + countPhotos

        val storageReference = FirebaseStorage.getInstance().getReference("images/$useremail/$dirName/$fileName")

        storageReference.putFile(Uri.fromFile(image))
            .addOnSuccessListener {
                lastimage = "images/$useremail/$dirName/$fileName"
                countPhotos++

                val myFile = File(image.absolutePath)
                myFile.delete()


                val metaRef = FirebaseStorage.getInstance().getReference("images/$useremail/$dirName/$fileName")
                metaRef.updateMetadata(metadata)
                    .addOnSuccessListener {

                    }
                    .addOnFailureListener {

                    }


                var clMain = findViewById<ConstraintLayout>(R.id.clMain)
                Snackbar.make(clMain, "Imagen Subida a la nube", Snackbar.LENGTH_LONG).setAction("OK") {
                    clMain.setBackgroundColor(Color.CYAN)
                }.show()


            }
            .addOnFailureListener{
                Toast.makeText(this, "Tu imagen se guard?? en el tfno, pero no en la nube :(",Toast.LENGTH_LONG).show()
            }


    }
}