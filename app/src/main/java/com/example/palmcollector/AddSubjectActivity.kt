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

    var imgCount = 0

    val subjectID: MutableList<String> = ArrayList()

    private lateinit var leftPalmRecyclerView: RecyclerView
    private lateinit var rightPalmRecyclerView: RecyclerView

    private lateinit var leftOrRight: String

//    private lateinit var palmAdapter: PalmAdapter

    private var tempLeftList = mutableListOf<Bitmap>()
    private var tempRightList = mutableListOf<Bitmap>()


//    Models
    private var subject: Subject? = null
//    private lateinit var subjectList: SubjectList
//    private lateinit var subjectMetaData: SubjectMetaData


    private var passableSubjectList: MutableList<Subject> = mutableListOf()

    private val listOfFiles = listFiles()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            val leftLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            leftPalmRecyclerView = findViewById(R.id.rv_left_palm_images)
            leftPalmRecyclerView.layoutManager = leftLayoutManager
            leftPalmRecyclerView.adapter = PalmAdapter(subject!!.leftList)

            val rightLayoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            rightPalmRecyclerView = findViewById(R.id.rv_right_palm_images)
            rightPalmRecyclerView.layoutManager = rightLayoutManager
            rightPalmRecyclerView.adapter = PalmAdapter(subject!!.rightList)
        }

//        var subId = listOfFiles[0].name.split("_")[0]
//        for (i in 0..(listOfFiles.size - 1)) {
//            subjectMetaData = SubjectMetaData(listOfFiles[i])
//            subjectList = SubjectList(passableSubjectList)
//            if(!subjectMetaData.Image.name.split("_")[0].equals(subId)){
//                subId = subjectMetaData.Image.name.split("_")[0]
//                var leftList: MutableList<SubjectMetaData> = mutableListOf()
//                var rightList: MutableList<SubjectMetaData> = mutableListOf()
//                if (subjectMetaData.Image.name.contains(subId) && subjectMetaData.Image.name.contains("left"))  {
//                    leftList.add(subjectMetaData)
//                } else if(subjectMetaData.Image.name.contains(subId) && subjectMetaData.Image.name.contains("right")){
//                    rightList.add(subjectMetaData)
//                }
//                subject = Subject(subId, leftList, rightList)
//                passableSubjectList.add(subject)
//            } else {
//                if (subjectMetaData.Image.name.contains(subId) && subjectMetaData.Image.name.contains("left")){
//                        subjectList.subjects.find { x -> x.subjectID.equals(subId) }?.leftList?.add(subjectMetaData)
//                } else if(subjectMetaData.Image.name.contains(subId) && subjectMetaData.Image.name.contains("right")){
//                        subjectList.subjects.find { x -> x.subjectID.equals(subId) }?.rightList?.add(subjectMetaData)
//                }
//            }
//        }

//        setupPalmRecyclerView()
        }


//    private fun setupPalmRecyclerView(){
//        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
//        palmRecyclerView = findViewById(R.id.rv_left_palm_images)
//        palmRecyclerView.layoutManager = layoutManager
//        palmRecyclerView.adapter = PalmAdapter(subject.leftList)
//    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun initClickListener() {
        var subName = findViewById<EditText>(R.id.etSubjectName).toString()
        findViewById<ImageButton>(R.id.btn_capture_image).setOnClickListener {
            if(subjectID.contains(subName)) {
                Toast.makeText(this, "Subject ID already exists", Toast.LENGTH_SHORT).show()
            } else {
                if (checkCameraPermission() && checkStoragePermission()) {
                    performImageCapture()
//                    setResult(Activity.RESULT_OK)
//                    finish()
                } else {
                    storagePermission?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    cameraPermission?.launch(Manifest.permission.CAMERA)
                }
            }
        }

        latestHandsResult?.let {
            Toast.makeText(this,"Landmark Count of the image obtained from jni ${NativeInterface().display(it).landmarksize}", Toast.LENGTH_SHORT).show()
            Log.i("is this", "executing")
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
                        if(imageView?.calculatehandedness(handsResult) == true){
                            leftOrRight = "right"
                        }else{
                            leftOrRight = "left"
                        }
                    }
//                    latestHandsResult?.let {
//                        Toast.makeText(this,"Landmark Count of the image obtained from jni ${NativeInterface().display(it).landmarksize}", Toast.LENGTH_SHORT).show()
//                    }
                    if(latestHandsResult?.let { NativeInterface().display(it).landmarksize }==21){
                        Toast.makeText(this,"Hand detected!", Toast.LENGTH_SHORT).show()

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


                        val saveImageToDirectory = saveImageToDirectory(bitmap, leftOrRight)
                        Log.i("Image saved", "path : : $saveImageToDirectory")
                    } else {
                        Toast.makeText(this,"No hand detected. Try again.", Toast.LENGTH_SHORT).show()
                    }
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
        return FileProvider.getUriForFile(
            this@AddSubjectActivity,
            "com.example.palmcollector.provider",
            File(externalCacheDir!!.path, "temp.png")
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

    private fun saveImageToDirectory(finalBitmap: Bitmap, handedness: String) {
        var subName = findViewById<EditText>(R.id.etSubjectName).text
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/palm_collector_images")
        Log.i("diectory", "$myDir")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        val generator = Random()
        var n = 10000
        n = generator.nextInt(n)
        val fname = "${subName}_${handedness}-${n}.png"
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

//    private fun bitmapToFile(bitmap: Bitmap): File{
//
//    }

    private fun deleteImage(){
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/palm_collector_images")
        val file = File(myDir, "my_filename")
        val deleted = file.delete()
    }

    private fun listFiles(): List<File>{
        var path = Environment.getExternalStorageDirectory().toString()+"/palm_collector_images";
        Log.d("Files","Path:"+path)
        val directory=File(path)
        val files=arrayListOf<File>(*directory.listFiles())
        files.sortWith { text1, text2 ->
            text1.compareTo(text2)
        }
        Log.d("Files","Size:"+files.size)
//        var subject = Subject
//        Subject.leftList = listOf<SubjectMetaData>()
//        for(i in 0..(files.size-1))
//        {
//            var subjectMetaData = SubjectMetaData(files[i])
//            if(subjectMetaData.Image.name.contains("left")){
//                var leftList  =
//            }
//            for(i in 0..files.size-1){
//                var subject = Subject(subjectMetaData.Image.name.split("_")[0], leftList)
//            }
//
//            var subject = Subject()
//            var splitted_array = subjectMetaData.Image.name.split("_").toTypedArray()
//            for(i in splitted_array){
//                Log.i("splitted","${i}")
//            }

//            Log.i("thevalues", "${splitted_array}")



//            var strs = files[i].getName().split("_").toTypedArray()
//            Log.i("strs","${strs}")
//            for(i in strs){
//                Log.i("splitted","${i}")
//            }
//            Log.d("Files","FileName:"+files[i-1].getName());
//        }
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
            runOnUiThread { imageView?.update() }

            Log.i("hand value", "$leftOrRight")
        }
        hands?.setErrorListener { message: String, e: RuntimeException? ->
            Log.e(
                TAG, "MediaPipe Hands error:$message"
            )
        }

//        var handednessValue = latestHandsResult?.let { calculateHandedness(it) }
//        Log.i("leftorright", handednessValue.toString())

//        val leftScrollView = findViewById<RecyclerView>(R.id.rv_palm_images)
//        leftScrollView.addView(imageView)
//        imageView?.visibility = View.VISIBLE

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

//    companion object {
//        private val IMAGE_DIRECTORY = "PalmCollectorImages"
//    }
}


