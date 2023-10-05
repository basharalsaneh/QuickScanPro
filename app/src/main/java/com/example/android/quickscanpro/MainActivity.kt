package com.example.android.quickscanpro

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.android.quickscanpro.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraButton: MaterialButton
    private lateinit var resultTextView: TextView

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
        private const val TAG = "MainActivity"
    }

    private lateinit var cameraPermission: Array<String>
    private var barcodeScannerOptions: BarcodeScannerOptions? = null
    private var barcodeScanner: BarcodeScanner? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraButton = binding.cameraButton.findViewById(R.id.cameraButton)
        resultTextView = binding.resultTextView

        cameraPermission = arrayOf(Manifest.permission.CAMERA)

        barcodeScannerOptions =
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions!!)

        cameraButton.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun detectBarcodeFromImage(imageBitmap: Bitmap) {
        try {
            val inputImage = InputImage.fromBitmap(imageBitmap, 0)
            val barcodeResult = barcodeScanner!!.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    extractBarcodeInformation(barcodes)
                }.addOnFailureListener { e ->
                    Log.e(TAG, "Barcode detection failed: ${e.message}")
                    showToast("Failed scanning due to ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception occurred during barcode detection: ${e.message}")
            showToast("Failed scanning due to ${e.message}")
        }
    }

    private fun extractBarcodeInformation(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            val rawValue = barcode.rawValue
            Log.d(TAG, "Raw Value: $rawValue")

            if (barcode.valueType == Barcode.TYPE_WIFI) {
                val typeWifi = barcode.wifi
                val ssid = "${typeWifi?.ssid}"
                val password = "${typeWifi?.password}"
                var encryptionType = "${typeWifi?.encryptionType}"

                if (encryptionType == "1") {
                    encryptionType = "OPEN"
                } else if (encryptionType == "2") {
                    encryptionType = "WPA"
                } else if (encryptionType == "3") {
                    encryptionType = "WEP"
                }


                resultTextView.text = "TYPE_WIFI \n\nSSID: $ssid \nPassword: $password \nEncryptionType: $encryptionType"
            } else if (barcode.valueType == Barcode.TYPE_URL) {
                val typeUrl = barcode.url
                val title = "${typeUrl?.title}"
                val url = "${typeUrl?.url}"

                resultTextView.text = "TYPE_URL \n\nTitle: $title \nUrl: $url "
            } else if (barcode.valueType == Barcode.TYPE_EMAIL) {
                val typeEmail = barcode.email
                val address = "${typeEmail?.address}"
                val body = "${typeEmail?.body}"
                val subject = "${typeEmail?.subject}"


                resultTextView.text = "TYPE_EMAIL \n\nEmail: $address\nSubject: $subject\nBody: $body"
            } else if (barcode.valueType == Barcode.TYPE_CONTACT_INFO) {
                val typeContact = barcode.contactInfo
                val title = "${typeContact?.title}"
                val organization = "${typeContact?.organization}"
                val name = "${typeContact?.name?.first} ${typeContact?.name?.last}"
                val phone = "${typeContact?.name?.first} ${typeContact?.phones?.get(0)?.number}"


                resultTextView.text = "TYPE_CONTACT_INFO \n\nTitle: $title \nOrganization: $organization \nName: $name \nPhone: $phone "
            } else {
                resultTextView.text = "Raw Value: $rawValue"
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val imageBitmap = data?.extras?.get("data") as Bitmap
                detectBarcodeFromImage(imageBitmap)
            } else {
                showToast("Camera capture failed")
            }
        }


    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_PERMISSION_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                showToast("Camera permission is required")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}