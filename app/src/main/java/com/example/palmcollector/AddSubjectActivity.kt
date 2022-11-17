package com.example.palmcollector

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.solutions.hands.HandsResult
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.FileChannel

class AddSubjectActivity : AppCompatActivity() {

    private var imageGetter: ActivityResultLauncher<Intent>? = null

    private var cameraPermission: ActivityResultLauncher<String>? = null

    private var storagePermission: ActivityResultLauncher<String>? = null

    private var latestHandsResult: HandsResult? = null

    private lateinit var leftPalmRecyclerView: RecyclerView
    private lateinit var rightPalmRecyclerView: RecyclerView

    private var tempLeftList = mutableListOf<SubjectMetaData>()
    private var tempRightList = mutableListOf<SubjectMetaData>()

    //    Models
    private var subject: Subject? = null

    // Intent data
    private var existSubLeftSize = 0
    private var existSubRightSize = 0

    private var leftOrRight : String? = null
    private var palmOrBack : String? = null

    private var listOfFiles = listFiles()

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
        existingSubjectAdd()
    }

    private fun existingSubjectAdd(){
        if(intent.hasExtra(MainActivity.SUBJECT_DETAILS)){
            subject = intent.getSerializableExtra(MainActivity.SUBJECT_DETAILS) as Subject
        }

        if(subject != null){
            var editText = findViewById<EditText>(R.id.etSubjectName)
            editText.setText(subject!!.subjectID)
            editText.isEnabled = false
            this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

            existSubLeftSize = subject!!.leftList.size
            existSubRightSize = subject!!.rightList.size

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
                if(subject!=null){
                    //if((subject!!.leftList.size>0) || (subject!!.rightList.size>0)){
                    if((subject!!.leftList.size) == (existSubLeftSize) && (subject!!.rightList.size) == (existSubRightSize)) {
                        Toast.makeText(this,"Please click images to save", Toast.LENGTH_SHORT).show()
                    } else {
                        if((subject!!.leftList.size != existSubLeftSize)){
                            for(i in existSubLeftSize..subject!!.leftList.size-1){
                                val bitmapImg = subject!!.leftList[i]
                                copy(bitmapImg, "left")
                            }
                        }
                        if((subject!!.rightList.size) != existSubRightSize ){
                            for(i in existSubRightSize..subject!!.rightList.size-1){
                                val bitmapImg = subject!!.rightList[i]
                                copy(bitmapImg, "right")
                            }
                        }
                        setResult(Activity.RESULT_OK)
                        finish()
                        Log.i("onclicklistener", "workingu")
                        return true
                    }
                    } else if (tempLeftList.size>0 || tempRightList.size>0 ){
                    Log.i("kikiki", "workingu")
                    if(tempLeftList.size>0){
                        Log.i("kikiki_leftsize${tempRightList.size}", "workingu")
                        for(i in 0..tempLeftList.size-1){
                            val bitmapImg = tempLeftList[i]
                            copy(bitmapImg, "left")
                        }
                    }
                    if(tempRightList.size>0){
                        Log.i("kikiki_rightsize${tempRightList.size}", "workingu")
                        for(i in 0..tempRightList.size-1){
                            val bitmapImg = tempRightList[i]
                            copy(bitmapImg, "right")
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
        recreate()
        finish()
        return true
    }

    private fun initClickListener() {
        var subName = findViewById<EditText>(R.id.etSubjectName).toString()
        findViewById<ImageButton>(R.id.btn_capture_image).setOnClickListener {

            if (checkCameraPermission() && checkStoragePermission()) {
                performImageCapture()
            } else {
                showRationalDialogForPermissions()
                storagePermission?.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                storagePermission?.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                storagePermission?.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
                cameraPermission?.launch(Manifest.permission.CAMERA)
            }
        }

        latestHandsResult?.let {
            Toast.makeText(this,"Landmark Count of the image obtained from jni ${NativeInterface().display(it).landmarksize}", Toast.LENGTH_SHORT).show()
            Log.i("is this", "executing")
        }
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
        imageGetter =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                if (result.resultCode == 78) {
                    var intent = result.data
                    if (intent != null) {
                        var image_path = intent.getStringExtra("bitmapURI_intent")
                        leftOrRight = intent.getStringExtra("handedness_intent")
                        palmOrBack = intent.getStringExtra("frontOrBack_intent")
                        var tempUrl: Uri = Uri.parse(image_path)
                        //theBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, tempUrl)
                        var theBitmapfile = File(tempUrl.encodedPath.toString())
                        //bitmapFiles.add(theBitmapfile)
                        Log.i("urireceived", "${tempUrl}")
                        Log.i("handedness_rec", "$leftOrRight")
                        Log.i("frontOrBack_rec", "$palmOrBack")

                        if (leftOrRight == "left") {
                            if(subject!=null){
                                subject!!.leftList.add(SubjectMetaData(theBitmapfile!!))
                            } else {
                                tempLeftList.add(SubjectMetaData(theBitmapfile!!))
                            }
                        } else {
                            if(subject!=null){
                                subject!!.rightList.add(SubjectMetaData(theBitmapfile!!))
                            } else {
                                tempRightList.add(SubjectMetaData(theBitmapfile!!))
                            }
                        }
                        Log.i("leftpalmsu", tempLeftList.toString())
                        Log.i("rightpalmsu", tempRightList.toString())

                        val leftLayoutManager =
                            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                        leftPalmRecyclerView = findViewById(R.id.rv_left_palm_images)
                        leftPalmRecyclerView.layoutManager = leftLayoutManager
                        if(subject!=null){
                            leftPalmRecyclerView.adapter = PalmAdapter(subject!!.leftList)
                        } else {
                            leftPalmRecyclerView.adapter = PalmAdapter(tempLeftList)
                        }

                        val rightLayoutManager =
                            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                        rightPalmRecyclerView = findViewById(R.id.rv_right_palm_images)
                        rightPalmRecyclerView.layoutManager = rightLayoutManager
                        if(subject!=null){
                            rightPalmRecyclerView.adapter = PalmAdapter(subject!!.rightList)
                        } else {
                            rightPalmRecyclerView.adapter = PalmAdapter(tempRightList)
                        }
                    }
                }
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

    private fun performImageCapture() {
        val pickImageIntent = Intent(this, CameraActivity::class.java)
        imageGetter!!.launch(pickImageIntent)
    }

    @Throws(IOException::class)
    private fun copy(finalBitmap: SubjectMetaData, handedness: String) {
        //
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
        //
        val inStream = FileInputStream(finalBitmap.Image)
        val outStream = FileOutputStream(file)
        val inChannel: FileChannel = inStream.channel
        val outChannel: FileChannel = outStream.channel
        inChannel.transferTo(0, inChannel.size(), outChannel)
        inStream.close()
        outStream.close()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

}