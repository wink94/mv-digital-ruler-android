package com.windula.mv_cpp_android_digital_ruler

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.AttributeSet
import androidx.core.app.ActivityCompat
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import com.windula.mv_cpp_android_digital_ruler.databinding.CameraCalibrateBinding
import com.windula.mv_cpp_android_digital_ruler.databinding.CameraDetectBinding
import org.opencv.android.*
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

class DetectActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private lateinit var binding: CameraDetectBinding
    private var refObjectPXPerCM:String = "0.000"
    private val points = mutableListOf<Point>()

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")

//                     Load native library after(!) OpenCV initialization
                    System.loadLibrary("mv_cpp_android_digital_ruler")

                    mOpenCvCameraView!!.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//         Permissions for Android 6+
        ActivityCompat.requestPermissions(
            this@DetectActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )

        binding = CameraDetectBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val overlayView = findViewById<OverlayView>(R.id.overlay_view)
        overlayView.setWillNotDraw(false)

        mOpenCvCameraView = findViewById<CameraBridgeViewBase>(R.id.main_surface)

        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE

        mOpenCvCameraView!!.setCvCameraViewListener(this)

        refObjectPXPerCM = intent.getStringExtra("refObjectPXPerCM").toString()

//        points.add(Point(255.2, 100.0))
//        points.add(Point(25.2, 10.0))
        val refObjectPXPerCMDouble: Double? = refObjectPXPerCM?.toDoubleOrNull()
        if (refObjectPXPerCMDouble != null) {
            if (refObjectPXPerCMDouble>0){
                overlayView.setRefObjectPXPerCM(refObjectPXPerCMDouble)

            }
        }
//        binding.captureButton.setOnClickListener {
//            val refObjectPXPerCMDouble: Double? = refObjectPXPerCM.toDoubleOrNull()
//            if (refObjectPXPerCMDouble != null) {
//                if (refObjectPXPerCMDouble>0){
//                    val resultIntent = Intent()
//                    resultIntent.putExtra("refObjectPXPerCM", refObjectPXPerCM)
//                    setResult(Activity.RESULT_OK, resultIntent)
//                    finish()
//                }
//                else{
//                    // Implement the capture functionality here
//                    Snackbar.make(binding.mainSurface, "Error in calibration, please retry", Snackbar.LENGTH_LONG).show()
//
//                }
//            }
//
//        }
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {

        if (event.action == MotionEvent.ACTION_DOWN && points.size < 2) {
            points.add(Point(event.x.toDouble(), event.y.toDouble()))
        }
        return super.onTouchEvent(event)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            CAMERA_PERMISSION_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mOpenCvCameraView!!.setCameraPermissionGranted()
                } else {
                    val message = "Camera permission was not granted"
                    Log.e(TAG, message)
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                }
            }
            else -> {
                Log.e(TAG, "Unexpected permission request")
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {}

    override fun onCameraViewStopped() {}

    override fun onCameraFrame(frame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
//         get current camera frame as OpenCV Mat object
        val mat = frame.rgba()

        if (points.size == 2) {
            val point1 = points[0]
            val point2 = points[1]
            val lineColor = Scalar(255.0, 0.0, 0.0) // Red color
            val lineWidth = 5

            Imgproc.line(mat, point1, point2, lineColor, lineWidth)
        }

//         return processed frame for live preview
        return mat
    }

//    private external fun calibrationFromJNI(matAddr: Long,width:Float,height:Float)


    companion object {

        private const val TAG = "CalibrateActivity"
        private const val CAMERA_PERMISSION_REQUEST = 1
    }
}
