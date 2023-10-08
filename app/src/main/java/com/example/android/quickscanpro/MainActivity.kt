package com.example.android.quickscanpro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.android.quickscanpro.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var resultTextView: TextView
    private lateinit var cameraPermission: Array<String>
    private var barcodeScannerOptions: BarcodeScannerOptions? = null
    private var barcodeScanner: BarcodeScanner? = null
    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        resultTextView = binding.resultTextView

        cameraPermission = arrayOf(Manifest.permission.CAMERA)

        barcodeScannerOptions =
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions!!)

        binding.cameraButton.setOnClickListener {
            if (checkCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }
    }
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val imageBitmap = data?.extras?.get("data") as Bitmap
                detectBarcodeFromImage(imageBitmap)
            } else {
                showToast(getString(R.string.camera_capture_failed))
            }
    }
    private fun detectBarcodeFromImage(imageBitmap: Bitmap) {
        try {
            val inputImage = InputImage.fromBitmap(imageBitmap, 0)
            barcodeScanner!!.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isEmpty()) {
                        showToast(getString(R.string.no_qr_code_detected))
                    } else {
                        extractBarcodeInformation(barcodes)
                        binding.qrImageView.visibility = View.GONE
                        binding.frameLayout.visibility = View.VISIBLE
                        binding.resultTitleLabel.visibility = View.VISIBLE
                    }
                }
                .addOnFailureListener { e ->
                    showToast(getString(R.string.failed_scanning_message, e.message))
                }
        } catch (e: Exception) {
            showToast(getString(R.string.failed_scanning_message, e.message))
        }
    }
    private fun extractBarcodeInformation(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            when (barcode.valueType) {
                Barcode.TYPE_WIFI -> {
                    val typeWifi = barcode.wifi
                    val ssid = "${typeWifi?.ssid}"
                    val password = "${typeWifi?.password}"
                    var encryptionType = "${typeWifi?.encryptionType}"

                    encryptionType = when (encryptionType) {
                        "1" -> "OPEN"
                        "2" -> "WPA"
                        "3" -> "WEP"
                        else -> encryptionType
                    }

                    resultTextView.text = getString(R.string.wifi_network_details, ssid, password, encryptionType)
                }
                Barcode.TYPE_URL -> {
                    val typeUrl = barcode.url
                    val title = "${typeUrl?.title}"
                    val url = "${typeUrl?.url}"
                    resultTextView.text = getString(R.string.url_details, title, url)
                }
                Barcode.TYPE_EMAIL -> {
                    val typeEmail = barcode.email
                    val address = "${typeEmail?.address}"
                    val body = "${typeEmail?.body}"
                    val subject = "${typeEmail?.subject}"
                    resultTextView.text = getString(R.string.email_details, address, subject, body)
                }
                Barcode.TYPE_CONTACT_INFO -> {
                    val typeContact = barcode.contactInfo
                    val title = "${typeContact?.title}"
                    val organization = "${typeContact?.organization}"
                    val name = "${typeContact?.name?.first} ${typeContact?.name?.last}"
                    val phone = "${typeContact?.name?.first} ${typeContact?.phones?.getOrNull(0)?.number ?: ""}"
                    resultTextView.text = getString(R.string.vcard_details, title, organization, name, phone)
                } else -> {
                    val rawValue = barcode.rawValue ?: ""
                    val formattedText = if (rawValue.isNotBlank()) {
                        getString(R.string.raw_value, rawValue)
                    } else {
                        getString(R.string.raw_value_empty)
                    }

                    val startIndex = formattedText.indexOf(rawValue)
                    val spannableString = SpannableString(formattedText)
                    val clickableSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            val googleSearchUrl = "https://www.google.com/search?q=$rawValue"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googleSearchUrl))
                            startActivity(intent)
                        }
                    }

                    spannableString.setSpan(clickableSpan, startIndex, startIndex + rawValue.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    resultTextView.text = spannableString
                    resultTextView.movementMethod = LinkMovementMethod.getInstance()
                }
            }

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
                showToast(getString(R.string.camera_permission_required))
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}