package jp.studio.edamame.simplebarcodereader

import android.Manifest
import android.annotation.SuppressLint
import android.databinding.DataBindingUtil
import android.hardware.Camera
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.SurfaceHolder
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import jp.studio.edamame.simplebarcodereader.databinding.ActivityMainBinding
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding

    private var mCameraSource : CameraSource? = null
    private val rxIsPermissionEnable: BehaviorSubject<Boolean> = BehaviorSubject.create()
    private val rxBarcode: PublishSubject<Barcode> = PublishSubject.create()
    private var flashMode = false

    private val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        disposable.add(rxIsPermissionEnable.filter { it }.observeOn(AndroidSchedulers.mainThread()).subscribe {
            setupReader()
        })

        disposable.add(rxBarcode.subscribe { barcode ->
            barcode.format
        })

        val rxPermission = RxPermissions(this)
        disposable.add(rxPermission.request(Manifest.permission.CAMERA)
                .subscribe { granted ->
                    if (granted) {
                        rxIsPermissionEnable.onNext(true)
                    } else {
                        showConfirmationDialog()
                    }
                })

        binding.mainHeaderLeftButton.setOnClickListener {
            flashOnButton()

            if (flashMode) {
                binding.mainHeaderLeftButton.setImageResource(R.drawable.light_on)
            } else {
                binding.mainHeaderLeftButton.setImageResource(R.drawable.light_off)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        this.disposable.dispose()
        mCameraSource?.release()
        mCameraSource = null
    }

    @SuppressLint("MissingPermission")
    private fun setupReader() {
        val barcodeDetector = BarcodeDetector.Builder(this).build()

        barcodeDetector.setProcessor(object: Detector.Processor<Barcode> {
            override fun release() {

            }

            //読み取り成功時
            override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
                detections?.let { it } ?: return

                if (detections.detectedItems.size() <= 0) { return }

                val barcode = detections.detectedItems?.valueAt(0)
                Log.e("test", barcode?.displayValue)
                Log.e("test", "barcode?.valueFormat = " + barcode?.valueFormat)

//                mCameraSource?.stop()
            }
        })

        mCameraSource = CameraSource.Builder(this, barcodeDetector)
                .setAutoFocusEnabled(true)
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
                }catch (ioe: IOException){
                    Log.e("Camera","Could not start camera source.",ioe)
                }
            }

        })

        mCameraSource?.start(binding.mainPreview.holder)
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

    private fun showConfirmationDialog() {
        val alert = AlertDialog.Builder(this).create().apply {
            this.setTitle("確認して下さい")
            this.setMessage("カメラへのアクセスを許可していただかないとアプリを使えません。")
            this.setButton(android.support.v7.app.AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok)) { _, _ ->
                this@MainActivity.finish()
            }
        }
        alert.show()
    }

    //dpからpxに変換する
    fun dpToPx(dp : Float): Int {
        val d = this.resources.displayMetrics.density
        return (dp*d).toInt()
    }
}
