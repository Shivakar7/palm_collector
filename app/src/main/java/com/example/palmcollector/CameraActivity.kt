package com.example.palmcollector

import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import kotlin.math.sqrt

class CameraActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private var hands: Hands? = null

    private var palmOrBack = "palm"

    // Run the pipeline and the model inference on GPU or CPU.
    private val RUN_ON_GPU = true

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
//        val startCameraButton = findViewById<Button>(R.id.camera_capture_button)
//        startCameraButton.setOnClickListener { v: View? ->
//            if (inputSource == InputSource.CAMERA) {
//                return@setOnClickListener
//            }
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


                if (handsResult?.let { NativeInterface().display(it).landmarksize } == 21){
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

                    runOnUiThread {
                        if(area >= 1){
                            guide.text = "Hold still"
                        } else if (area > 0) {
                            guide.text = "Bring palm closer"
                        }
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
    //Hands code
}
