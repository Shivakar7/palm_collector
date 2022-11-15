package com.example.palmcollector

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.mediapipe.components.TextureFrameConsumer
import com.google.mediapipe.framework.TextureFrame
import com.google.mediapipe.solutioncore.CameraInput
import com.google.mediapipe.solutioncore.ErrorListener
import com.google.mediapipe.solutioncore.ResultListener
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView
import com.google.mediapipe.solutions.hands.HandLandmark
import com.google.mediapipe.solutions.hands.Hands
import com.google.mediapipe.solutions.hands.HandsOptions
import com.google.mediapipe.solutions.hands.HandsResult
import java.io.*
import kotlin.math.sqrt


class CameraActivity : AppCompatActivity(){

    private val TAG = "MainActivity"

    private var hands: Hands? = null

    private var palmOrBack: String? = null

    // Run the pipeline and the model inference on GPU or CPU.
    private val RUN_ON_GPU = true

    //private var latestHandsResult: HandsResult? = null

    private var imageGetter: ActivityResultLauncher<Intent>? = null

    private var imageView: HandsResultImageView? = null

    private var leftOrRight: String? = null

    private var flag = false

    private enum class InputSource {
        CAMERA
    }

    private var glSurfaceView: SolutionGlSurfaceView<HandsResult>? = null
    private var surfaceHolder = glSurfaceView?.holder

    // Live camera demo UI and camera components.
    private var cameraInput: CameraInput? = null

    private var inputSource: InputSource = InputSource.CAMERA

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        // hide the action bar
        supportActionBar?.hide()

        // Check camera permissions if all permission granted
        // start camera else ask for the permission
        setupLiveDemoUiComponents()
    }

    override fun onResume() {
        super.onResume()
        if (inputSource == InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = CameraInput(this)
            cameraInput!!.setNewFrameListener { textureFrame: TextureFrame? ->
                hands!!.send(
                    textureFrame
                )
            }
            glSurfaceView?.post { startCamera() }
            glSurfaceView?.setVisibility(View.VISIBLE)
        }
    }

    override fun onPause() {
        super.onPause()
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView!!.setVisibility(View.GONE)
            cameraInput!!.close()
        }
    }

    //Hands code

    private fun setupLiveDemoUiComponents() {
        stopCurrentPipeline()
        setupStreamingModePipeline(InputSource.CAMERA)
    }

    private fun setupStreamingModePipeline(inputSource: InputSource) {
        this.inputSource = inputSource
        // Initializes a new MediaPipe Hands solution instance in the streaming mode.
        hands = Hands(
            this,
            HandsOptions.builder()
                .setStaticImageMode(false)
                .setMaxNumHands(2)
                .setRunOnGpu(RUN_ON_GPU)
                .build()
        )
        hands!!.setErrorListener(ErrorListener { message: String, e: RuntimeException? ->
            Log.e(
                TAG,
                "MediaPipe Hands error:$message"
            )
        })
        if (inputSource == InputSource.CAMERA) {
            cameraInput = CameraInput(this)
            cameraInput!!.setNewFrameListener(TextureFrameConsumer { textureFrame: TextureFrame? ->
                hands!!.send(
                    textureFrame
                )
            })
        }
        val guide = findViewById<TextView>(R.id.tv_guide)

//         Initializes a new Gl surface view with a user-defined HandsResultGlRenderer.
        glSurfaceView = SolutionGlSurfaceView<HandsResult>(
            this,
            hands!!.getGlContext(),
            hands!!.getGlMajorVersion()
        )
        glSurfaceView!!.setSolutionResultRenderer(HandsResultGlRenderer())
        glSurfaceView!!.setRenderInputImage(true)
        hands!!.setResultListener(
            ResultListener { handsResult: HandsResult? ->
                logWristLandmark(handsResult!!,  /*showPixelValues=*/false)
                glSurfaceView!!.setRenderData(handsResult)
                glSurfaceView!!.requestRender()
                //
                //
                imageView = HandsResultImageView(this)
                logWristLandmark(handsResult!!,  /*showPixelValues=*/true)
                imageView?.setHandsResult(handsResult)
                runOnUiThread { imageView?.update() }

                if (handsResult?.let { NativeInterface().display(it).landmarksize } == 21 && imageView!!.frontOrBack(handsResult) ){
                    var area = 0.0

                    val handPointList = handsResult.multiHandLandmarks()?.get(0)?.landmarkList

                    for(k in 0..3){
                        var a = sqrt(Math.pow(((handPointList!![0].x - handPointList.get(4*k+1).x).toDouble()),2.0) + Math.pow(((handPointList!!.get(0).y - handPointList.get(4*k+1).y).toDouble()),2.0))
                        var b = sqrt(Math.pow(((handPointList!!.get(0).x - handPointList.get(4*(k+1)+1).x).toDouble()),2.0) + Math.pow(((handPointList!!.get(0).y - handPointList.get(4*(k+1)+1).y).toDouble()),2.0))
                        var c = sqrt(Math.pow(((handPointList!!.get(4*k+1).x - handPointList.get(4*(k+1)+1).x).toDouble()),2.0) + Math.pow(((handPointList!!.get(4*k+1).y - handPointList.get(4*(k+1)+1).y).toDouble()),2.0))
                        var s = a+b+c
                        area += (s*(s-a)*(s-b)*(s-c))
                    }

                    Log.i("areavalue", "$area")

                    //runOnUiThread {
                    if(area in 1.0..1.3){
                        Log.i("beluga_isPalm", "${imageView?.frontOrBack(handsResult)}")
                        Log.i("walter_isLeft", "${imageView?.calculatehandedness(handsResult)}")
                        runOnUiThread{guide.text = "Hold still"}
                        //stopCurrentPipeline()
                        //imageanalysis
                        //var bitmap =
                        if (imageView?.calculatehandedness(handsResult) == true) {
                            leftOrRight = "right"
                        } else {
                            leftOrRight = "left"
                        }
                        if (imageView?.frontOrBack(handsResult) == true) {
                            palmOrBack = "palm"
                        } else {
                            palmOrBack = "back"
                        }
                        //imageanalysis
                        //Log.i("inputBitmap", "$bitmap")
                        var uri = SaveImage(handsResult.inputBitmap())
//                            var uri = getImageUri(this, bitmap)
                        var i = Intent(this, AddSubjectActivity::class.java)
                        i.putExtra("bitmapURI_intent", uri.toString())
                        i.putExtra("handedness_intent", leftOrRight)
                        i.putExtra("frontOrBack_intent", palmOrBack)
                        //imageGetter!!.launch(i)
                        setResult(78, i)
                        finish()
//                            this.startActivity(i)
                    } else if (area in 0.0..1.0) {
                        runOnUiThread{guide.text = "Bring palm closer"}
                    } else if (area > 1.3){
                        runOnUiThread{guide.text = "Place palm further"}
                    }
                }
            })


        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView!!.post(Runnable { startCamera() })
        }

        // Updates the preview layout.
        val frameLayout = findViewById<FrameLayout>(R.id.preview_display_layout)
        //imageView.setVisibility(View.GONE)
        frameLayout.removeAllViewsInLayout()
        frameLayout.addView(glSurfaceView)

        glSurfaceView!!.setVisibility(View.VISIBLE)
        frameLayout.requestLayout()
    }

    // add sub code

//    private fun imageAnalysis(inputBitmap: Bitmap) : Bitmap? {
//
//
//        var bitmap: Bitmap? = null
//        var uriTemp = getImageUri(this, inputBitmap)
//        try {
//            bitmap = downscaleBitmap(
//                MediaStore.Images.Media.getBitmap(
//                    this.contentResolver, uriTemp
//                )
//            )
//        } catch (e: IOException) {
//            Log.e(TAG, "Bitmap reading error:$e")
//        }
//        try {
//            val imageData =
//                this.contentResolver.openInputStream(uriTemp!!)
//            if (bitmap != null && imageData != null) {
//                bitmap = rotateBitmap(bitmap, imageData)
//                Log.i("rotationhappens", "$bitmap")
//            }
//        } catch (e: IOException) {
//            Log.e(TAG, "Bitmap rotation error:$e")
//        }
//        if (bitmap != null) {
//            hands?.send(bitmap)
////            hands = Hands(
////                this,
////                HandsOptions.builder()
////                    .setStaticImageMode(true)
////                    .setMaxNumHands(2)
////                    .setRunOnGpu(RUN_ON_GPU)
////                    .build()
////            )
//            hands?.setResultListener { handsResult: HandsResult? ->
//                if(flag == false){
//                    //latestHandsResult = handsResult
//                    imageView?.setHandsResult(handsResult)
//                    imageView?.setImageDrawable(null)
//                    imageView?.visibility = View.VISIBLE
//                    Log.i("henlo", "block")
//                    //**
//                    flag = true
//
//                    runOnUiThread { imageView?.update() }
//
//                    Log.i("hand value", "$leftOrRight")
//                    Log.i("palmness", "$palmOrBack")
//                }
//                hands?.setErrorListener { message: String, e: RuntimeException? ->
//                    Log.e(
//                        TAG, "MediaPipe Hands error:$message"
//                    )
//                }
//                }
//                }
//        return bitmap
//        }


    private fun rotateBitmap(inputBitmap: Bitmap, imageData: InputStream): Bitmap? {
        val orientation = ExifInterface(imageData).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
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

    private fun downscaleBitmap(originalBitmap: Bitmap): Bitmap? {
        val aspectRatio = originalBitmap.width.toDouble() / originalBitmap.height
        var width = originalBitmap!!.width
        var height = originalBitmap!!.height
        if (originalBitmap!!.width.toDouble() / originalBitmap!!.height > aspectRatio) {
            width = (height * aspectRatio).toInt()
        } else {
            height = (width / aspectRatio).toInt()
        }
        Log.i("ScaleBitmap", "$width, $height, $originalBitmap")
        return Bitmap.createScaledBitmap(originalBitmap, width, height, false)
    }

    // add sub code

    private fun logWristLandmark(result: HandsResult, showPixelValues: Boolean) {
        if (result.multiHandLandmarks().isEmpty()) {
            return
        }
        val wristLandmark = result.multiHandLandmarks()[0].landmarkList[HandLandmark.WRIST]
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            val width = result.inputBitmap().width
            val height = result.inputBitmap().height
            Log.i(
                TAG, String.format(
                    "MediaPipe Hand wrist coordinates (pixel values): x=%f, y=%f",
                    wristLandmark.x * width, wristLandmark.y * height
                )
            )
        } else {
            Log.i(
                TAG, String.format(
                    "MediaPipe Hand wrist normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                    wristLandmark.x, wristLandmark.y
                )
            )
        }
        if (result.multiHandWorldLandmarks().isEmpty()) {
            return
        }
        val wristWorldLandmark =
            result.multiHandWorldLandmarks()[0].landmarkList[HandLandmark.WRIST]
        Log.i(
            TAG, String.format(
                "MediaPipe Hand wrist world coordinates (in meters with the origin at the hand's"
                        + " approximate geometric center): x=%f m, y=%f m, z=%f m",
                wristWorldLandmark.x, wristWorldLandmark.y, wristWorldLandmark.z
            )
        )
    }

    private fun startCamera() {
        cameraInput!!.start(
            this,
            hands!!.glContext,
            CameraInput.CameraFacing.BACK,
            glSurfaceView!!.width,
            glSurfaceView!!.height
        )
    }

    private fun stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput!!.setNewFrameListener(null)
            cameraInput!!.close()
        }
        if (glSurfaceView != null) {
            glSurfaceView!!.visibility = View.GONE
        }
        if (hands != null) {
            hands!!.close()
        }
    }

    fun captureImage() : Bitmap {
        var bitmap = Bitmap.createBitmap(
            glSurfaceView!!.getWidth(), glSurfaceView!!.getHeight(),
            Bitmap.Config.ARGB_8888
        )
        var canvas = Canvas(bitmap)
        glSurfaceView!!.draw(canvas)

        return bitmap
    }

    fun getBitmapUri() : Uri{
        val FILENAME = "image.png"
        val PATH = externalCacheDir!!.path
        val f = File(PATH, FILENAME)
        val yourUri: Uri = Uri.fromFile(f)

        return yourUri
    }

    fun getImageUri(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(
            inContext.getContentResolver(),
            inImage,
            "Titletata",
            null
        )
        return Uri.parse(path)
    }

    private fun SaveImage(finalBitmap: Bitmap) : Uri {
        val fname = "temp_cam.jpg"
        val file = File(externalCacheDir!!.path, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        var uridaw = Uri.fromFile(file)
        Log.i("urichech", "$uridaw")

        return uridaw
    }
}
