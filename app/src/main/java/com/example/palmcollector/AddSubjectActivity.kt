package com.example.palmcollector

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.io.*
import java.util.*


class AddSubjectActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private var imageGetter: ActivityResultLauncher<Intent>? = null

    private var cameraPermission: ActivityResultLauncher<String>? = null

    private var storagePermission: ActivityResultLauncher<String>? = null

    private var hands: Hands? = null

    // Run the pipeline and the model inference on GPU or CPU.
    private val RUN_ON_GPU = true

    private var imageView: HandsResultImageView? = null

    private var latestHandsResult: HandsResult? = null

    var imgCount = 0

    val subjectID: MutableList<String> = ArrayList()

    private lateinit var palmRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_subject)
        assert(
            supportActionBar != null //null check
        )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        palmRecyclerView = findViewById(R.id.rv_palm_images)
        palmRecyclerView.layoutManager = layoutManager

        initialize()
        initClickListener()


    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initClickListener() {
        var subName = findViewById<EditText>(R.id.etSubjectName)
        findViewById<ImageButton>(R.id.btn_capture_image).setOnClickListener {
            if(subjectID.contains(subName.toString())) {
                Toast.makeText(this, "Subject ID already exists", Toast.LENGTH_SHORT).show()
            } else {
                if (checkCameraPermission() && checkStoragePermission()) {
                    performImageCapture()
                } else {
                    storagePermission?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    cameraPermission?.launch(Manifest.permission.CAMERA)
                }
            }
        }

        latestHandsResult?.let {
            Toast.makeText(this,"Landmark Count of the image obtained from jni ${NativeInterface().display(it).landmarksize}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initialize() {

        imageView = HandsResultImageView(this)
//        cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
//            if (isGranted) {
//                performImageCapture()
//            }
//        }



        latestHandsResult?.let {
            Toast.makeText(this,"Landmark Count of the image obtained from jni ${NativeInterface().display(it).landmarksize}", Toast.LENGTH_SHORT).show()
        }

        // The Intent to capture image from camera.
        imageGetter = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                var bitmap: Bitmap? = null
                val tempUrl: Uri = getCaptureImageOutputUri()
                Log.d(TAG, "The temporary url is : $tempUrl")
                try {
                    bitmap = downscaleBitmap(
                        MediaStore.Images.Media.getBitmap(
                            this.contentResolver, tempUrl
                        )
                    )
                } catch (e: IOException) {
                    Log.e(TAG, "Bitmap reading error:$e")
                }
                try {
                    val imageData =
                        this.contentResolver.openInputStream(tempUrl)
                    if (bitmap != null && imageData != null) {
                        bitmap = rotateBitmap(bitmap, imageData)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Bitmap rotation error:$e")
                }
                if (bitmap != null) {
                    val saveImage = saveImage(bitmap)
                    listFiles()
                    Log.i("Image saved", "path : : $saveImage")
                    hands?.send(bitmap)
                }
            }
        }
    }

    private fun performImageCapture() {
        hands?.close()
        setupStaticImageModePipeline()
        var subName = findViewById<EditText>(R.id.etSubjectName)
        subjectID.add(subName.toString())
        imgCount++
        Log.i(TAG, "Array elements ${subjectID.toString()}")

        // Open camera to capture image
        val outputFileUri: Uri = getCaptureImageOutputUri()
        val pickImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        pickImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
        imageGetter!!.launch(pickImageIntent)

    }

    private fun getCaptureImageOutputUri(): Uri {
        var subName = findViewById<EditText>(R.id.etSubjectName)

        return FileProvider.getUriForFile(
            this@AddSubjectActivity,
            "com.example.palmcollector.provider",
            File(externalCacheDir!!.path, "${subName.text}__${imgCount}.png")
        )
    }

//    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri{
//        var subName = findViewById<EditText>(R.id.etSubjectName)
//        val wrapper = ContextWrapper(applicationContext)
//        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
//        file = File(file, "${subName.text}_${imgCount}.png")
//        try {
//            val stream: OutputStream = FileOutputStream(file)
//            Log.i("stream block", "executed")
//            bitmap.compress(Bitmap.CompressFormat.PNG,100, stream)
//            stream.flush()
//            stream.close()
//        } catch (e: IOException){
//            e.printStackTrace()
//        }
//        return Uri.parse(file.absolutePath)
//    }

    private fun saveImage(finalBitmap: Bitmap) {
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/palm_collector_images")
        Log.i("diectory", "$myDir")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        val generator = Random()
        var n = 10000
        n = generator.nextInt(n)
        val fname = "Image-$n.png"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            Log.i("output strean block", "executed")
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun listFiles(){
        var path = Environment.getExternalStorageDirectory().toString()+"/palm_collector_images";
        Log.d("Files","Path:"+path);
        val directory=File(path)
        val files=arrayListOf<File>(*directory.listFiles())
        Log.d("Files","Size:"+files.size);
        for(i in 1..files.size)
        {
            Log.d("Files","FileName:"+files[i-1].getName());
        }
    }


//    private fun storeImage(image: Bitmap) {
//        val pictureFile: File = getOutputMediaFile()
//        if (pictureFile == null) {
//            Log.d(
//                TAG,
//                "Error creating media file, check storage permissions: "
//            ) // e.getMessage());
//            return
//        }
//        try {
//            val fos = FileOutputStream(pictureFile)
//            image.compress(Bitmap.CompressFormat.PNG, 90, fos)
//            fos.close()
//        } catch (e: FileNotFoundException) {
//            Log.d(TAG, "File not found: " + e)
//        } catch (e: IOException) {
//            Log.d(TAG, "Error accessing file: " + e)
//        }
//    }

//    private fun calculateHandedness() : Boolean {
//        var landmarksSize = latestHandsResult?.multiHandLandmarks()?.size
//        var handednessBool = false
//        for (i in 0 until landmarksSize!!){
//            handednessBool = latestHandsResult?.multiHandedness()?.get(i)?.getLabel().equals("Left")
//        }
//        return handednessBool
//    }

    /** Sets up core workflow for static image mode.  */
    private fun setupStaticImageModePipeline() {
        // Initializes a new MediaPipe Hands solution instance in the static image mode.
        hands = Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(true)
                .setMaxNumHands(2)
                .setRunOnGpu(RUN_ON_GPU)
                .build()
        )

        // Connects MediaPipe Hands solution to the user-defined HandsResultImageView.
        hands?.setResultListener { handsResult: HandsResult? ->
            latestHandsResult = handsResult
            imageView?.setHandsResult(handsResult)
            runOnUiThread { imageView?.update() }
        }
        hands?.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(
                TAG, "MediaPipe Hands error:$message"
            )
        }

//        val leftScrollView = findViewById<RecyclerView>(R.id.rv_palm_images)
//        leftScrollView.addView(imageView)
//        imageView?.visibility = View.VISIBLE

//         Updates the preview layout.
        val frameLayout = findViewById<FrameLayout>(R.id.fl_preview_display)
        frameLayout.removeAllViewsInLayout()
        imageView?.setImageDrawable(null)
        frameLayout.addView(imageView)
        imageView?.visibility = View.VISIBLE
    }

    private fun downscaleBitmap(originalBitmap: Bitmap): Bitmap? {
        val aspectRatio = originalBitmap.width.toDouble() / originalBitmap.height
        var width = imageView!!.width
        var height = imageView!!.height
        if (imageView!!.width.toDouble() / imageView!!.height > aspectRatio) {
            width = (height * aspectRatio).toInt()
        } else {
            height = (width / aspectRatio).toInt()
        }
        return Bitmap.createScaledBitmap(originalBitmap, width, height, false)
    }

    @Throws(IOException::class)
    private fun rotateBitmap(inputBitmap: Bitmap, imageData: InputStream): Bitmap? {
        val orientation = ExifInterface(imageData).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return inputBitmap
        }
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            else -> matrix.postRotate(0f)
        }
        return Bitmap.createBitmap(
            inputBitmap, 0, 0, inputBitmap.width, inputBitmap.height, matrix, true
        )
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }



    companion object {
        private val IMAGE_DIRECTORY = "PalmCollectorImages"
    }


}