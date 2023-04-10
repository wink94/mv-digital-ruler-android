package com.windula.mv_cpp_android_digital_ruler

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.material.snackbar.Snackbar
import com.windula.mv_cpp_android_digital_ruler.databinding.CameraCalibrateBinding
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Mat

class CalibrateActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var mOpenCvCameraView: CameraBridgeViewBase? = null
    private lateinit var binding: CameraCalibrateBinding
    private var refObjectPXPerCM:String = "0.000"
    private var width:Double = 0.0
    private var height:Double = 0.0
    private lateinit var sensorManager: SensorManager
    private lateinit var orientationListener: SensorEventListener
    private var currentRotation = 0

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
            this@CalibrateActivity,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST
        )

        binding = CameraCalibrateBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        mOpenCvCameraView = findViewById<CameraBridgeViewBase>(R.id.main_surface)

        mOpenCvCameraView!!.visibility = SurfaceView.VISIBLE

        mOpenCvCameraView!!.setCvCameraViewListener(this)

        width=intent.getDoubleExtra("width",0.0)
        height=intent.getDoubleExtra("height",0.0)

        binding.captureButton.setOnClickListener {
            val refObjectPXPerCMDouble: Double? = refObjectPXPerCM.toDoubleOrNull()
            if (refObjectPXPerCMDouble != null) {
                if (refObjectPXPerCMDouble>0){
                    val resultIntent = Intent()
                    resultIntent.putExtra("refObjectPXPerCM", refObjectPXPerCM)
                    setResult(Activity.RESULT_OK, resultIntent)
                    finish()
                }
                else{
                    // Implement the capture functionality here
                    Snackbar.make(binding.mainSurface, "Error in calibration, please retry", Snackbar.LENGTH_LONG).show()

                }
            }

        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        orientationListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed
            }

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val rotation = when {
                    event.values[0] < 45 || event.values[0] > 315 -> 0
                    event.values[0] > 45 && event.values[0] < 135 -> 90
                    event.values[0] > 135 && event.values[0] < 225 -> 180
                    else -> 270
                }

                if (currentRotation != rotation) {
                    currentRotation = rotation
//                    updateCameraDisplayOrientation()
                }
            }
        }

        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        sensorManager.registerListener(orientationListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)

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
        sensorManager.unregisterListener(orientationListener)
        if (mOpenCvCameraView != null)
            mOpenCvCameraView!!.disableView()
    }

    override fun onResume() {
        super.onResume()
        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        sensorManager.registerListener(orientationListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)
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
        val mat = frame.gray()


       refObjectPXPerCM = NativeBridge.calibrationFromJNI(mat.nativeObjAddr,width.toFloat(),height.toFloat())
        Log.d(TAG, "refObjectPXPerCM ${refObjectPXPerCM}")
//         return processed frame for live preview
        return mat
    }

    private fun getCameraDisplayOrientation(cameraId:Int): Int {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList[cameraId]
        val characteristics = manager.getCameraCharacteristics(cameraId)

        val rotation = currentRotation
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        val landscapeFlip = if (sensorOrientation == 270) 180 else 0
        return (sensorOrientation - rotation + landscapeFlip) % 360
    }

//    private fun updateCameraDisplayOrientation() {
//        val displayRotation = getCameraDisplayOrientation(0)
//        val camera = (mOpenCvCameraView as JavaCameraView).
//        if (camera != null) {
//            try {
//                camera.setDisplayOrientation(displayRotation)
//                val params = camera.parameters
//                params.setRotation(displayRotation)
//                camera.parameters = params
//            } catch (e: Exception) {
//                Log.e(TAG, "Error updating camera display orientation: ", e)
//            }
//        }
//    }



    object NativeBridge {
        init {
            System.loadLibrary("mv_cpp_android_digital_ruler")
        }

        external fun calibrationFromJNI(matAddr: Long,width:Float,height:Float):String
    }

    companion object {

        private const val TAG = "CalibrateActivity"
        private const val CAMERA_PERMISSION_REQUEST = 1
    }
}
