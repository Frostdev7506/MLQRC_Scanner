package com.akatski.mlqrcscanner

import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.frame.Frame
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.lang.StringBuilder
import java.util.jar.Manifest
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    internal var isDetected = false
    lateinit var options:FirebaseVisionBarcodeDetectorOptions
    lateinit var detector:FirebaseVisionBarcodeDetector



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        Dexter.withActivity(this@MainActivity)
            .withPermissions(android.Manifest.permission.CAMERA,
                android.Manifest.permission.RECORD_AUDIO)
            .withListener(object:MultiplePermissionsListener{
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    setupCamera()
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {

                }


            }).check()




    }

    private fun setupCamera() {
        options=FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build()

        detector= FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        btn_again.isEnabled = isDetected
        btn_again.setOnClickListener{
            isDetected=!isDetected
            btn_again.isEnabled=isDetected


        }

       cameraView.setLifecycleOwner(this)
        cameraView.addFrameProcessor { frame -> processImage(getVisionImageFromFrame(frame))}





        }

    private fun processImage(image: FirebaseVisionImage) {
if(!isDetected)
{
    detector.detectInImage(image)
        .addOnFailureListener{ exception -> Toast.makeText(this@MainActivity,""+exception.message,Toast.LENGTH_LONG).show() }
        .addOnSuccessListener{firebaseVisionBarcodes ->
            processResult(firebaseVisionBarcodes)
        }
}



    }

    private fun processResult(firebaseVisionBarcodes: List<FirebaseVisionBarcode>) {
        if(firebaseVisionBarcodes.size>0)
        {

            isDetected=true
            btn_again.isEnabled = isDetected
            for(item in firebaseVisionBarcodes)
            {
                val value_type = item.valueType
                when(value_type)
                {
                FirebaseVisionBarcode.TYPE_TEXT ->{

                    createDialog(item.rawValue)
                }
                FirebaseVisionBarcode.TYPE_CONTACT_INFO ->{

                     val info = StringBuilder("Name:")
                         .append(item.contactInfo!!.name!!.formattedName)
                         .append("/n")
                         .append("Address: ")
                         .append(item.contactInfo!!.addresses[0].addressLines[0])
                         .append("/n")
                         .append("Email:")
                         .append(item.contactInfo!!.emails[0].address)
                         .toString()
                    createDialog(info)
                }
                    FirebaseVisionBarcode.TYPE_URL ->{
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.rawValue))
                        startActivity(intent)
                    }


                }
            }
        }


    }

    private fun createDialog(text: String?) {

        val builder = AlertDialog.Builder(this)
        builder.setMessage(text)
            .setPositiveButton("okay",{ DialogInterface, _-> DialogInterface.dismiss()  })

        val dialog = builder.create()
        dialog.show()




    }

    private fun getVisionImageFromFrame(frame: Frame): FirebaseVisionImage {
      val data =frame.data
       val metadata=FirebaseVisionImageMetadata.Builder()
           .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
           .setHeight(frame.size.height)
           .setWidth(frame.size.width)
           //.setRotation(frame.rotation)
           .build()
        return FirebaseVisionImage.fromByteArray(data,metadata)



    }


}

