package com.windula.mv_cpp_android_digital_ruler

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.windula.mv_cpp_android_digital_ruler.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private val CALIBRATE = "CALIBRATE"
    private val DETECT = "DETECT"
    private var isCalibrare = false
    private var isDetect = false

    private lateinit var binding: ActivityMainBinding

    private var refObjectPXPerCM:String? = "0.000"

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            // Permission granted, you can open the camera
            if (isCalibrare)
                openCamera(CALIBRATE)
            if (isDetect)
                openCamera(DETECT)
        } else {
            // Permission denied, show a message
            Snackbar.make(binding.mainContainer, "Camera permission is required.", Snackbar.LENGTH_LONG).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.fab1.setOnClickListener {
            checkCameraPermissionAndOpenCamera(CALIBRATE)
            isCalibrare=true
            isDetect = false
        }

        binding.fab2.setOnClickListener {
            checkCameraPermissionAndOpenCamera(DETECT)
            isCalibrare=false
            isDetect = true
        }
    }

    private fun checkCameraPermissionAndOpenCamera(action:String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // If permission is already granted, open the camera
            openCamera(action)
        } else {
            // If permission is not granted, request it
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera(action:String) {
        // Implement the logic to open the camera here
        // You can use CameraX or another library to handle camera operations
        Snackbar.make(binding.mainContainer, "Camera opened ", Snackbar.LENGTH_LONG).show()
        if (action==CALIBRATE){
            val intent = Intent(this, CalibrateActivity::class.java)
            calibrateActivityLauncher.launch(intent)
        }
        if (action==DETECT){
            val refObjectPXPerCMDouble: Double? = refObjectPXPerCM?.toDoubleOrNull()
            if (refObjectPXPerCMDouble != null) {
                if (refObjectPXPerCMDouble>0){
                    val intent = Intent(this, DetectActivity::class.java)
                    intent.putExtra("refObjectPXPerCM",refObjectPXPerCM)
                    detectActivityLauncher.launch(intent)
                }
            }
            else{
                Snackbar.make(binding.mainContainer, "Distance calculation cannot be completed", Snackbar.LENGTH_LONG).show()
            }

        }
    }

    private val calibrateActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                // Extract the data sent from CameraActivity
                refObjectPXPerCM = data.getStringExtra("refObjectPXPerCM")
                // Do something with the received data
                Toast.makeText(this, "Reference object px/cm: $refObjectPXPerCM px/cm", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val detectActivityLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                // Extract the data sent from CameraActivity
                refObjectPXPerCM = data.getStringExtra("distance")
                // Do something with the received data
                Toast.makeText(this, "distance ", Toast.LENGTH_LONG).show()
            }
        }
    }


}
