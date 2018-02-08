package jp.studio.edamame.simplebarcodereader

import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import android.util.SparseArray
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import java.io.ByteArrayOutputStream

class BoxDetector(private val mDelegate: Detector<*>, private val mBoxWidth: Int, private val mBoxHeight: Int) : Detector<Barcode>() {

    override fun detect(frame: Frame): SparseArray<Barcode>? {
        val width = frame.metadata.width
        val height = frame.metadata.height
        Log.d("height",height.toString())
        val right = width / 2 + mBoxHeight / 2
        val left = width / 2 - mBoxHeight / 2
        val bottom = height / 2 + mBoxWidth / 2
        val top = height / 2 - mBoxWidth / 2

        val yuvImage = YuvImage(frame.getGrayscaleImageData().array(), ImageFormat.NV21, width, height, null)
        val byteArrayOutputStream = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(left, top, right, bottom), 100, byteArrayOutputStream)
        val jpegArray = byteArrayOutputStream.toByteArray()
        val bitmap = BitmapFactory.decodeByteArray(jpegArray, 0, jpegArray.size)

        val croppedFrame = Frame.Builder()
                .setBitmap(bitmap)
                .setRotation(frame.metadata.rotation)
                .build()

        return mDelegate.detect(croppedFrame) as SparseArray<Barcode>?
    }

    override fun isOperational(): Boolean {
        return mDelegate.isOperational
    }

    override fun setFocus(id: Int): Boolean {
        return mDelegate.setFocus(id)
    }
}