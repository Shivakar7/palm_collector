package com.example.palmcollector

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.ActionMenuItemView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
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

    private lateinit var leftPalmRecyclerView: RecyclerView
    private lateinit var rightPalmRecyclerView: RecyclerView

    private lateinit var leftOrRight: String

    private var tempLeftList = mutableListOf<Bitmap>()
    private var tempRightList = mutableListOf<Bitmap>()

//    Models
    private var subject: Subject? = null

    private var passableSubjectList: MutableList<Subject> = mutableListOf()

    private val listOfFiles = listFiles()

    private var palmOrBack = "palm"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_PalmCollector)
        setContentView(R.layout.activity_add_subject)
        assert(
            supportActionBar != null //null check
        )
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        initialize()
        initClickListener()

        if(intent.hasExtra(MainActivity.SUBJECT_DETAILS)){
            subject = intent.getSerializableExtra(MainActivity.SUBJECT_DETAILS) as Subject
        }

        if(subject != null){
            var editText = findViewById<EditText>(R.id.etSubjectName)
            editText.setText(subject!!.subjectID)
            editText.isEnabled = false
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

            val leftLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            leftPalmRecyclerView = findViewById(R.id.rv_left_palm_images)
            leftPalmRecyclerView.layoutManager = leftLayoutManager
            leftPalmRecyclerView.adapter = PalmAdapter(subject!!.leftList)

            val rightLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rightPalmRecyclerView = findViewById(R.id.rv_right_palm_images)
            rightPalmRecyclerView.layoutManager = rightLayoutManager
            rightPalmRecyclerView.adapter = PalmAdapter(subject!!.rightList)
        }
        }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.add_subject_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miSave -> {
//              Code to save
                if(tempLeftList.size>0 || tempRightList.size>0 ){
                    Log.i("kikiki", "workingu")
                    if(tempLeftList.size>0){
                        Log.i("kikiki_leftsize${tempRightList.size}", "workingu")
                        for(i in 0..tempLeftList.size-1){
                            val bitmapImg = tempLeftList[i]
                            saveImageToDirectory(bitmapImg, "left")
                        }
                    }
                    if(tempRightList.size>0){
                        Log.i("kikiki_rightsize${tempRightList.size}", "workingu")
                        for(i in 0..tempRightList.size-1){
                            val bitmapImg = tempRightList[i]
                            saveImageToDirectory(bitmapImg, "right")
                        }
                    }
                    setResult(Activity.RESULT_OK)
                    finish()
                    Log.i("onclicklistener", "workingu")
                    return true
                } else {
                    Toast.makeText(this,"Please click images to save", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return false
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initClickListener() {
        var subName = findViewById<EditText>(R.id.etSubjectName).toString()
        findViewById<ImageButton>(R.id.btn_capture_image).setOnClickListener {

                if (checkCameraPermission() && checkStoragePermission()) {
                    performImageCapture()
                    setResult(Activity.RESULT_OK)
//                    finish()
                } else {
//                    Toast.makeText(this, "Please enable camera and storage permissions",Toast.LENGTH_SHORT).show()
                    showRationalDialogForPermissions()
                    storagePermission?.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                    storagePermission?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    storagePermission?.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                    cameraPermission?.launch(Manifest.permission.CAMERA)
                }
            }

//        latestHandsResult?.let {
//            Toast.makeText(this,"Landmark Count of the image obtained from jni ${NativeInterface().display(it).landmarksize}", Toast.LENGTH_SHORT).show()
//            Log.i("is this", "executing")
//        }
    }

    fun showRationalDialogForPermissions(){
        Log.i("Alertdialogex", "done")
        AlertDialog.Builder(this@AddSubjectActivity).setMessage("It looks like you have not granted permission" +
                " required for the app's proper functioning. "+
                "It can be enabled under the Application Settings")
            .setPositiveButton("Go to Settings"){
                    _,_ ->
                try{
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }.setNegativeButton("Cancel"){
                    dialog,_ ->
                dialog.dismiss()
            }.show()
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

                    hands?.send(bitmap)
                    hands = Hands(
                        this,
                        HandsOptions.builder()
                            .setStaticImageMode(true)
                            .setMaxNumHands(2)
                            .setRunOnGpu(RUN_ON_GPU)
                            .build()
                    )
                    hands?.setResultListener { handsResult: HandsResult? ->
                        latestHandsResult = handsResult
                    }
//                        if(imageView?.calculatehandedness(handsResult) == true){
//                            leftOrRight = "right"
//                        } else {
//                            leftOrRight = "left"
//                        }
//                        if(imageView?.frontOrBack(handsResult) == true){
//                            palmOrBack = "palm"
//                        } else {
//                            palmOrBack = "back"
//                        }
//                    }
                    if(latestHandsResult?.let { NativeInterface().display(it).landmarksize } == 21 && palmOrBack == "palm"){
                        Toast.makeText(this,"Palm detected!", Toast.LENGTH_SHORT).show()

                        if(leftOrRight == "left"){
                            tempLeftList.add(bitmap)
                        } else {
                            tempRightList.add(bitmap)
                        }

                        val leftLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                        leftPalmRecyclerView = findViewById(R.id.rv_left_palm_images)
                        leftPalmRecyclerView.layoutManager = leftLayoutManager
                        leftPalmRecyclerView.adapter = TempPalmAdapter(tempLeftList)

                        val rightLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                        rightPalmRecyclerView = findViewById(R.id.rv_right_palm_images)
                        rightPalmRecyclerView.layoutManager = rightLayoutManager
                        rightPalmRecyclerView.adapter = TempPalmAdapter(tempRightList)
                    } else {
                        Toast.makeText(this,"No palm detected. Try again.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun performImageCapture() {
        hands?.close()
        setupStaticImageModePipeline()

        // Open camera to capture image
        val outputFileUri: Uri = getCaptureImageOutputUri()
        val pickImageIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        pickImageIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri)
        imageGetter!!.launch(pickImageIntent)
    }

    private fun getCaptureImageOutputUri(): Uri {
        return FileProvider.getUriForFile(
            this@AddSubjectActivity,
            "com.example.palmcollector.provider",
            File(externalCacheDir!!.path, "temp.png")
        )
    }

    private fun saveImageToDirectory(finalBitmap: Bitmap, handedness: String) {
        var subName = findViewById<EditText>(R.id.etSubjectName).text
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/palm_collector_images")
        if(!myDir.exists()){
            myDir.mkdirs()
        }
        Log.i("diectory", "$myDir")
        val fname = "${subName}_${handedness}_${System.currentTimeMillis()}.png"
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

    private fun listFiles(): List<File>{
        var path = Environment.getExternalStorageDirectory().toString()+"/palm_collector_images";
        Log.d("Files","Path:"+path)
        val directory=File(path)
        if(!directory.exists()){
            return arrayListOf()
        }
        val files=arrayListOf<File>(*directory.listFiles())
        files.sortWith { text1, text2 ->
            text1.compareTo(text2)
        }
        Log.d("Files","Size:"+files.size)
        return files
    }

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
            if(imageView?.calculatehandedness(handsResult) == true){
                leftOrRight = "right"
            }else{
                leftOrRight = "left"
            }
            if(imageView?.frontOrBack(handsResult)==true){
                palmOrBack = "palm"
            }else{
                palmOrBack = "back"
            }
            runOnUiThread { imageView?.update() }

            Log.i("hand value", "$leftOrRight")
            Log.i("palmness", "$palmOrBack")
        }
        hands?.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(
                TAG, "MediaPipe Hands error:$message"
            )
        }

//         Updates the preview layout
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
        Log.i("ScaleBitmap", "$width, $height, $originalBitmap")
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
}




