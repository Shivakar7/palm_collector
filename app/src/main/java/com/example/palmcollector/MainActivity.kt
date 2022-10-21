package com.example.palmcollector

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
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

    private var listOfFiles = listFiles()

    //Models
    private lateinit var subject: Subject
    private lateinit var subjectList: SubjectList
    private lateinit var subjectMetaData: SubjectMetaData

    // Temporary list
    private var passableSubjectList: MutableList<Subject> = mutableListOf()

    //Adapter
    private lateinit var subjectAdapter: SubjectAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_PalmCollector)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                    if(report!!.areAllPermissionsGranted()){
                        val firstRun =
                            getSharedPreferences("preferences", MODE_PRIVATE).getBoolean("firstrun", true)
                        if (firstRun) {
                            //set the firstrun to false so the next run can see it.
                            getSharedPreferences("preferences", MODE_PRIVATE).edit().putBoolean("firstrun", false)
                                .commit()
                            Toast.makeText(applicationContext, "Directory created \n /storage/emulated/0/palm_collector_images", Toast.LENGTH_LONG)
                                .show()
                            val root = Environment.getExternalStorageDirectory().toString()
                            val myDir = File("$root/palm_collector_images")
                            myDir.mkdirs()
                        }
                        if(listOfFiles.isEmpty()){
                            rv_subject_list.visibility = View.INVISIBLE
                            sample_text.visibility = View.VISIBLE
                        } else {
                            var subId = ""
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

                            subjectAdapter = SubjectAdapter(subjectList.subjects)
                            rv_subject_list.adapter = subjectAdapter
                            rv_subject_list.layoutManager = LinearLayoutManager(this@MainActivity)
                            subjectAdapter.setOnClickListener(object : SubjectAdapter.OnClickListener{
                                override fun onClick(position: Int, model: Subject) {
                                    val intent = Intent(this@MainActivity, AddSubjectActivity::class.java)
                                    intent.putExtra(SUBJECT_DETAILS, model)
                                    startActivityForResult(intent, ADD_SUBJECT_ACTIVITY_REQUEST_CODE)
                                }
                            })
                        }
                        rv_subject_list.visibility = View.VISIBLE
                        sample_text.visibility = View.INVISIBLE
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest?>?,
                    token: PermissionToken?
                )
                {
                    showRationalDialogForPermissions()
                }
            }).onSameThread()
            .check()

//        var subId = listOfFiles[0].name.split("_")[0]
//        val subjectRecyclerView = findViewById<RecyclerView>(R.id.rv_subject_list)



//        Log.i("heysubject", "$subject")
//        Log.i("heysubjectlist", "$subjectList")
//        Log.i("heysubjectmetadata", "$subjectMetaData")
    }

    fun showRationalDialogForPermissions(){
        Log.i("Alertdialogex", "done")
        AlertDialog.Builder(this@MainActivity).setMessage("It looks like you have not granted permission" +
                " required for the app's proper functioning. "+
                "It can be enabled under the Application Settings")
            .setPositiveButton("Go to Settings"){
                    _,_->
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

    // Action bar menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.app_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miAddSubject -> {
                val Intent = Intent(this, AddSubjectActivity::class.java)
                startActivityForResult(Intent, ADD_SUBJECT_ACTIVITY_REQUEST_CODE)
            }
        }
        return true
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if(requestCode == ADD_SUBJECT_ACTIVITY_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
//                listOfFiles = listFiles()
//                subjectAdapter.notifyDataSetChanged()
//                this.recreate()
                Log.i("Is this block", "executing")
                finish()
                startActivity(intent)
            } else {
                super.onActivityResult(requestCode, resultCode, data)
                Log.i("Activity", "Cancelled or Backpressed")
            }
        }
    }

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
        var ADD_SUBJECT_ACTIVITY_REQUEST_CODE = 1

    }
}
