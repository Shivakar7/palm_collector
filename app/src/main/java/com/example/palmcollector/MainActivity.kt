package com.example.palmcollector

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.palmcollector.databinding.ActivityMainBinding
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    var addSubjectActivity: AddSubjectActivity? = null

//    private lateinit var subject: Subject
//    private lateinit var subjectList: SubjectList
//    private lateinit var subjectMetaData: SubjectMetaData
//
//    var listOfFiles = addSubjectActivity?.fileList()

    private var listOfFiles = listFiles()

    //    Models
    private lateinit var subject: Subject
    private lateinit var subjectList: SubjectList
    private lateinit var subjectMetaData: SubjectMetaData


    private var passableSubjectList: MutableList<Subject> = mutableListOf()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) { /* ... */
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest?>?,
                    token: PermissionToken?
                ) { /* ... */
                }
            }).check()

        var subId = listOfFiles[0].name.split("_")[0]
        for (i in 0..(listOfFiles.size - 1)) {
            subjectMetaData = SubjectMetaData(listOfFiles[i])
            subjectList = SubjectList(passableSubjectList)
            if(!subjectMetaData.Image.name.split("_")[0].equals(subId)){
                subId = subjectMetaData.Image.name.split("_")[0]
                var leftList: MutableList<SubjectMetaData> = mutableListOf()
                var rightList: MutableList<SubjectMetaData> = mutableListOf()
                if (subjectMetaData.Image.name.contains(subId) && subjectMetaData.Image.name.contains("left"))  {
                    leftList.add(subjectMetaData)
                } else if(subjectMetaData.Image.name.contains(subId) && subjectMetaData.Image.name.contains("right")){
                    rightList.add(subjectMetaData)
                }
                subject = Subject(subId, leftList, rightList)
                passableSubjectList.add(subject)
            } else {
                if (subjectMetaData.Image.name.contains(subId) && subjectMetaData.Image.name.contains("left")){
                    subjectList.subjects.find { x -> x.subjectID.equals(subId) }?.leftList?.add(subjectMetaData)
                } else if(subjectMetaData.Image.name.contains(subId) && subjectMetaData.Image.name.contains("right")){
                    subjectList.subjects.find { x -> x.subjectID.equals(subId) }?.rightList?.add(subjectMetaData)
                }
            }
        }

        Log.i("heysubject", "$subject")
        Log.i("heysubjectlist", "$subjectList")
        Log.i("heysubjectmetadata", "$subjectMetaData")

        val subjectAdapter = SubjectAdapter(subjectList.subjects)
        rv_subject_list.adapter = subjectAdapter
        rv_subject_list.layoutManager = LinearLayoutManager(this)
        subjectAdapter.setOnClickListener(object : SubjectAdapter.OnClickListener{
            override fun onClick(position: Int, model: Subject) {
                val intent = Intent(this@MainActivity, AddSubjectActivity::class.java)
                intent.putExtra(SUBJECT_DETAILS, model)
                startActivity(intent)
            }
        })

    }

    // Action bar menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miAddSubject -> {
                val Intent = Intent(this, AddSubjectActivity::class.java)
                startActivity(Intent)
            }
        }
        return true
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

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if(requestCode == ADD_SUBJECT_ACTIVITY_REQUEST_CODE){
//            if(requestCode == Activity.RESULT_OK){
//                listOfFiles = listFiles()
//            } else {
//                Log.i("Activity", "Cancelled or Backpressed")
//            }
//        }
//    }

    /**
     * A native method that is implemented by the 'palmcollector' native library,
     * which is packaged with this application.
     */
//    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'mldemo' library on application startup.
        init {
            System.loadLibrary("palmcollector")
        }
        var SUBJECT_DETAILS = "subject_details"

    }
}