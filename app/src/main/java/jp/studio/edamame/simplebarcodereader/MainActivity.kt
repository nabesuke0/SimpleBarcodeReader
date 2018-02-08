package jp.studio.edamame.simplebarcodereader

import android.databinding.DataBindingUtil
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewTreeObserver
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.barcode.BarcodeDetector
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.Subject
import jp.studio.edamame.simplebarcodereader.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private var mCameraSource : CameraSource? = null

    private var readSize: BehaviorSubject<Pair<Int, Int>> = BehaviorSubject.create()
    private var previewSize: BehaviorSubject<Pair<Int, Int>> = BehaviorSubject.create()

    private var mainLayoutObserver: ViewTreeObserver.OnGlobalLayoutListener? = null
    private var barcodeImageObserver: ViewTreeObserver.OnGlobalLayoutListener? = null

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

        val cameraSource = CameraSource.Builder(this, boxDetector)
                .setRequestedPreviewSize(previewSize.first, previewSize.second)
                .setAutoFocusEnabled(true)

        mCameraSource = cameraSource
                .build()

    }

    private fun dpToPx(dp : Float): Int {
        val d = this.resources.displayMetrics.density
        return (dp*d).toInt()
    }
}
