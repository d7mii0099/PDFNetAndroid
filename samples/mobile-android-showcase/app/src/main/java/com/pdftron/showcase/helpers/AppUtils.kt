package com.pdftron.showcase.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.pdftron.pdf.utils.Utils
import com.pdftron.showcase.R
import org.apache.commons.io.FilenameUtils

object AppUtils {

    private val PERMISSIONS_CAMERA = arrayOf(Manifest.permission.CAMERA)

    fun hasCameraPermission(context: Context): Boolean {
        if (Utils.isMarshmallow()) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Camera permissions have not been granted.
                return false
            }
        }
        return true
    }

    fun requestCameraPermissions(activity: Activity, requestCode: Int) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.CAMERA)) {
            Toast.makeText(activity, R.string.permission_camera_rationale, Toast.LENGTH_SHORT).show()
        } else {
            // Camera permissions have not been granted yet. Request them directly.
            ActivityCompat.requestPermissions(activity, PERMISSIONS_CAMERA, requestCode)
        }
    }

    fun verifyPermissions(grantResults: IntArray): Boolean {
        // At least one result must be checked.
        if (grantResults.size < 1) {
            return false
        }

        // Verify that each required permission has been granted, otherwise return false.
        for (result in grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    @Throws(WriterException::class)
    fun createImage(message: String?, type: String?, size: Int, size_width: Int, size_height: Int): Bitmap? {
        var bitMatrix: BitMatrix? = null
        bitMatrix = when (type) {
            "QR Code" -> MultiFormatWriter().encode(message, BarcodeFormat.QR_CODE, size, size)
            "Barcode" -> MultiFormatWriter().encode(message, BarcodeFormat.CODE_128, size_width, size_height)
            "Data Matrix" -> MultiFormatWriter().encode(message, BarcodeFormat.DATA_MATRIX, size, size)
            "PDF 417" -> MultiFormatWriter().encode(message, BarcodeFormat.PDF_417, size_width, size_height)
            "Barcode-39" -> MultiFormatWriter().encode(message, BarcodeFormat.CODE_39, size_width, size_height)
            "Barcode-93" -> MultiFormatWriter().encode(message, BarcodeFormat.CODE_93, size_width, size_height)
            "AZTEC" -> MultiFormatWriter().encode(message, BarcodeFormat.AZTEC, size, size)
            else -> MultiFormatWriter().encode(message, BarcodeFormat.QR_CODE, size, size)
        }
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (i in 0 until height) {
            for (j in 0 until width) {
                if (bitMatrix[j, i]) {
                    pixels[i * width + j] = -0x1000000
                } else {
                    pixels[i * width + j] = -0x1
                }
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    fun getUriFromRawName(applicationContext: Context, fileName: String): Uri {
        val extension = FilenameUtils.getExtension(fileName)
        val name = FilenameUtils.removeExtension(fileName)
        val res = applicationContext.resources
        val fileId = res.getIdentifier(name, "raw", applicationContext.packageName)
        val file = Utils.copyResourceToLocal(applicationContext, fileId, name, ".$extension")
        return Uri.fromFile(file)
    }
}