package jp.studio.edamame.simplebarcodereader

import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.SurfaceHolder
import android.view.ViewTreeObserver
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import jp.studio.edamame.simplebarcodereader.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    private var mCameraSource : CameraSource? = null

    private var readSize: BehaviorSubject<Pair<Int, Int>> = BehaviorSubject.create()
    private var previewSize: BehaviorSubject<Pair<Int, Int>> = BehaviorSubject.create()

    private var mainLayoutObserver: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var barcodeImageObserver: ViewTreeObserver.OnGlobalLayoutListener? = null

    private var flashMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        mainLayoutObserver = ViewTreeObserver.OnGlobalLayoutListener {
            previewSize.onNext(Pair(binding.mainLayoutBase.width, binding.mainLayoutBase.height))
            previewSize.onComplete()

            mainLayoutObserver?.let {
                binding.mainLayoutBase.viewTreeObserver.removeOnGlobalLayoutListener(it)
            }
        }
        binding.mainLayoutBase.viewTreeObserver.addOnGlobalLayoutListener(mainLayoutObserver)

        barcodeImageObserver = ViewTreeObserver.OnGlobalLayoutListener {
            readSize.onNext(Pair(binding.mainBarcodeImage.width, binding.mainBarcodeImage.height))

            barcodeImageObserver?.let {
                binding.mainBarcodeImage.viewTreeObserver.removeOnGlobalLayoutListener(it)
            }
        }
        binding.mainBarcodeImage.viewTreeObserver.addOnGlobalLayoutListener(barcodeImageObserver)


        Observable.combineLatest<Pair<Int, Int>, Pair<Int, Int>, Array<Pair<Int, Int>>>(
                readSize, previewSize, BiFunction {a, b -> arrayOf(a, b)}
        ).subscribe { array ->
            this.setupReader(array[0], array[1])
        }
    }

    private fun setupReader(readSize: Pair<Int, Int>, previewSize: Pair<Int, Int>) {
        val barcodeDetector = BarcodeDetector.Builder(this).build()

        val boxDetector = BoxDetector(
                barcodeDetector, readSize.first, readSize.second)

        boxDetector.setProcessor(object: Detector.Processor<Barcode> {
            override fun release() {

            }

            //読み取り成功時
            override fun receiveDetections(detections: Detector.Detections<Barcode>?) {

            }
        })

        val cameraSource = CameraSource.Builder(this, boxDetector)
                .setRequestedPreviewSize(previewSize.first, previewSize.second)
                .setAutoFocusEnabled(true)

        mCameraSource = cameraSource
                .build()

        binding.mainPreview.holder?.addCallback(object : SurfaceHolder.Callback{
            override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {

            }

            //プレビュー破棄 バックグラウンド時にも呼ばれる
            override fun surfaceDestroyed(holder: SurfaceHolder?) {
                Log.d("Camera","stop camera source.")
                mCameraSource?.stop()
            }

            //プレビュー生成 フォアグラウンド時にも呼ばれる
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(holder: SurfaceHolder?) {
                try {
                    mCameraSource?.start(binding.mainPreview.holder)

//                    if (!isDialog){
//                        Log.d("Camera","start camera source.")
//                        mCameraSource?.start(surfaceView?.holder)
//                    }else{
//                        mCameraSource?.start(surfaceView?.holder)
//                        val cameraStop = Runnable {
//                            mCameraSource?.stop()
//                        }
//                        val handler = Handler()
//                        handler.postDelayed(cameraStop,1000)
//                    }
                }catch (ioe: IOException){
                    Log.e("Camera","Could not start camera source.",ioe)
                }
            }

        })
    }

    private fun flashOnButton() {
        CameraSource::class.java.declaredFields
                .first { it.type === Camera::class.java }?.let { field ->
            field.isAccessible = true

            val cameraSource = mCameraSource?.let { it } ?: return@let
            val camera = try {
                field.get(cameraSource) as Camera
            } catch (exception : IllegalArgumentException) {
                Log.e("camera", "field.get(cameraSource) : " + exception.stackTrace)
                return@let
            }

            try {
                val param = camera.parameters
                param?.flashMode = if (!flashMode) Camera.Parameters.FLASH_MODE_TORCH else Camera.Parameters.FLASH_MODE_OFF
                camera.parameters = param
                flashMode = !flashMode
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
