package io.github.saisana299.yolojan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.github.saisana299.yolojan.Constants.LABELS_PATH
import io.github.saisana299.yolojan.Constants.MODEL_PATH
import yolojan.R
import yolojan.databinding.ActivityMainBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), Detector.DetectorListener {
    private lateinit var binding: ActivityMainBinding
    private val isFrontCamera = false

    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var detector: Detector? = null

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // API 30以上の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.decorView.windowInsetsController?.apply {
                // systemBars : Status barとNavigation bar両方
                hide(WindowInsets.Type.systemBars())
                // hide(WindowInsets.Type.statusBars())
                // hide(WindowInsets.Type.navigationBars())
                systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        // API 29以下の場合
        } else {
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
            )
        }

        // バインディングを初期化
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // カメラ用のスレッドエグゼキュータを作成
        cameraExecutor = Executors.newSingleThreadExecutor()

        // ディテクタを初期化
        cameraExecutor.execute {
            detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
        }

        // パーミッションが許可されているか確認
        if (allPermissionsGranted()) {
            startCamera() // カメラを開始
        } else {
            // パーミッションをリクエスト
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        bindListeners() // リスナーをバインド
    }

    private fun bindListeners() {
        binding.apply {
        }
    }

    private fun startCamera() {
        // カメラプロバイダーのインスタンスを取得
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider  = cameraProviderFuture.get() // カメラプロバイダーを取得
            bindCameraUseCases() // カメラの使用ケースをバインド
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        // カメラプロバイダーが初期化されているか確認
        val cameraProvider = cameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // レイアウト変更
        val orientation = resources.configuration.orientation
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.cameraContainer)
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 縦向きの場合
            constraintSet.setDimensionRatio(R.id.view_finder, "9:16")
            constraintSet.setDimensionRatio(R.id.overlay, "9:16")
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横向きの場合
            constraintSet.setDimensionRatio(R.id.view_finder, "16:9")
            constraintSet.setDimensionRatio(R.id.overlay, "16:9")
        }
        constraintSet.applyTo(binding.cameraContainer)

        // 表示の回転を取得
        val rotation = binding.viewFinder.display.rotation

        // カメラセレクターを設定（バックカメラを使用）
        val cameraSelector = CameraSelector
            .Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()

        // プレビューの設定
        preview =  Preview.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                    .build()
            )
            .setTargetRotation(rotation)
            .build()

        // 画像解析の設定
        imageAnalyzer = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_16_9, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                    .build()
            )
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(binding.viewFinder.display.rotation)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        // 画像解析の実行
        imageAnalyzer?.setAnalyzer(cameraExecutor) { imageProxy ->
            // 画像をビットマップに変換
            val bitmapBuffer =
                Bitmap.createBitmap(
                    imageProxy.width,
                    imageProxy.height,
                    Bitmap.Config.ARGB_8888
                )
            imageProxy.use { bitmapBuffer.copyPixelsFromBuffer(imageProxy.planes[0].buffer) }
            imageProxy.close()

            // 画像の回転を適用
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat())

                // フロントカメラの場合、左右反転
                if (isFrontCamera) {
                    postScale(
                        -1f,
                        1f,
                        imageProxy.width.toFloat(),
                        imageProxy.height.toFloat()
                    )
                }
            }

            // 回転したビットマップを作成
            val rotatedBitmap = Bitmap.createBitmap(
                bitmapBuffer, 0, 0, bitmapBuffer.width, bitmapBuffer.height,
                matrix, true
            )

            // ディテクタで物体を検出
            detector?.detect(rotatedBitmap)
        }

        // すべての使用ケースを解除
        cameraProvider.unbindAll()

        try {
            // ライフサイクルにバインド
            camera = cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageAnalyzer
            )

            // プレビューのサーフェスプロバイダーを設定
            preview?.surfaceProvider = binding.viewFinder.surfaceProvider
        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc) // エラーログを出力
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        // 各パーミッションが許可されているか確認
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) {
        // パーミッションリクエストの結果を処理
        if (it[Manifest.permission.CAMERA] == true) {
            startCamera() // カメラのパーミッションが許可された場合、カメラを開始
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // ディテクタを閉じてリソースを解放
        detector?.close()
        // カメラエグゼキュータをシャットダウン
        cameraExecutor.shutdown()
    }

    override fun onPause() {
        super.onPause()
        detector = null
        imageAnalyzer = null
        camera = null
        preview = null
    }

    override fun onResume() {
        super.onResume()
        // アクティビティが再開されたときにパーミッションを確認
        if (allPermissionsGranted()){
            if (detector == null) {
                cameraExecutor.execute {
                    detector = Detector(baseContext, MODEL_PATH, LABELS_PATH, this)
                }
            }
            startCamera() // パーミッションが許可されていればカメラを開始
        } else {
            // パーミッションが許可されていない場合、リクエストを開始
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera" // ログ用のタグ
        private const val REQUEST_CODE_PERMISSIONS = 10 // パーミッションリクエストのコード
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA // 必要なパーミッションのリスト
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            // 物体が検出されなかった場合、オーバーレイをクリア
            binding.overlay.clear()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onDetect(boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        runOnUiThread {
            // 検出結果をUIに表示
            binding.overlay.apply {
                setResults(boundingBoxes) // 検出されたバウンディングボックスを設定
                invalidate() // オーバーレイを再描画
            }
        }
    }
}
