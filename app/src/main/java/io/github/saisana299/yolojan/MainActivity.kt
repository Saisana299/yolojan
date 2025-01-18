package io.github.saisana299.yolojan

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.WindowManager
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import io.github.saisana299.yolojan.Constants.LABELS_PATH
import io.github.saisana299.yolojan.Constants.MODEL_PATH
import org.mahjong4j.GeneralSituation
import org.mahjong4j.HandsOverFlowException
import org.mahjong4j.IllegalMentsuSizeException
import org.mahjong4j.IllegalShuntsuIdentifierException
import org.mahjong4j.MahjongTileOverFlowException
import org.mahjong4j.PersonalSituation
import org.mahjong4j.Player
import org.mahjong4j.hands.Hands
import org.mahjong4j.tile.Tile
import yolojan.R
import yolojan.databinding.ActivityMainBinding
import java.io.IOException
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
    private var detectorPict: Detector? = null
    private var capturedBitmap: Bitmap? = null
    private var pictBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var pictureExecutor: ExecutorService
    private lateinit var boundingBoxDrawer: BoundingBoxDrawer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 画面OFF禁止
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // フルスクリーン
        setFullscreen()

        // バインディングを初期化
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // カメラ用のスレッドエグゼキュータを作成
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 画像用のスレッドエグゼキュータを作成
        pictureExecutor = Executors.newSingleThreadExecutor()

        // ディテクタを初期化
        cameraExecutor.execute {
            detector = Detector(1, baseContext, MODEL_PATH, LABELS_PATH, this)
            runOnUiThread {
                binding.apply {
                    textView2.visibility = View.GONE
                    textView2.invalidate()
                }
            }
        }

        pictureExecutor.execute {
            detectorPict = Detector(2, baseContext, MODEL_PATH, LABELS_PATH, this)
        }

        // BoundingBoxDrawerの初期化
        boundingBoxDrawer = BoundingBoxDrawer(this)

        // パーミッションが許可されているか確認
        if (allPermissionsGranted()) {
            startCamera() // カメラを開始
        } else {
            // パーミッションをリクエスト
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        bindListeners() // リスナーをバインド
    }

    private fun setFullscreen() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        // システムバーの動作設定
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // ステータスバーとナビゲーションバーを隠す
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun bindListeners() {
        binding.apply {
            floatingActionButton.setOnClickListener {
                takePicture()
            }
            floatingActionButton3.setOnClickListener {
                preview.visibility = View.GONE
                overlay.visibility = View.VISIBLE
                textView4.visibility = View.VISIBLE
                textView5.visibility = View.VISIBLE
                floatingActionButton.visibility = View.VISIBLE
            }
            floatingActionButton2.setOnClickListener {
                resultBitmap?.let { bitmap -> savePicture(bitmap) }
                preview.visibility = View.GONE
                overlay.visibility = View.VISIBLE
                textView4.visibility = View.VISIBLE
                textView5.visibility = View.VISIBLE
                floatingActionButton.visibility = View.VISIBLE
            }
            floatingActionButton4.setOnClickListener {
                popupSettings()
            }
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
            constraintSet.setDimensionRatio(R.id.view_finder, "3:4")
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横向きの場合
            constraintSet.setDimensionRatio(R.id.view_finder, "4:3")
        }
        constraintSet.applyTo(binding.cameraContainer)

        // レイアウト変更2
        val constraintSet2 = ConstraintSet()
        constraintSet2.clone(binding.preview)
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // 縦向きの場合
            constraintSet2.setDimensionRatio(R.id.pic_preview, "3:4")
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // 横向きの場合
            constraintSet2.setDimensionRatio(R.id.pic_preview, "4:3")
        }
        constraintSet2.applyTo(binding.preview)

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
                    .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO))
                    .build()
            )
            .setTargetRotation(rotation)
            .build()

        // 画像解析の設定
        imageAnalyzer = ImageAnalysis.Builder()
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy(AspectRatio.RATIO_4_3, AspectRatioStrategy.FALLBACK_RULE_AUTO))
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

            // 変数にキャッシュ
            capturedBitmap = rotatedBitmap

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

    private fun takePicture() {
        capturedBitmap?.let { bitmap ->
            pictBitmap = bitmap
            detectorPict?.detect(bitmap)
        }
    }

    private fun popupSettings() {
        //
    }

    private fun savePicture(bitmap: Bitmap) {
        val filename = "IMG_${System.currentTimeMillis()}.jpg"

        // MediaStoreに画像を保存するためのContentValuesを作成
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // 保存先のディレクトリ
        }

        // MediaStoreに新しい画像を挿入
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            try {
                // OutputStreamを取得
                val outputStream = contentResolver.openOutputStream(it)
                outputStream?.use { fos -> // nullチェックを追加
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos) // JPEG形式で保存
                } ?: run {
                    Log.e(TAG, "OutputStream is null")
                }

                Log.d(TAG, "Picture saved: $uri")
            } catch (e: IOException) {
                Log.e(TAG, "Error saving picture", e)
            }
        } ?: run {
            Log.e(TAG, "Error creating media store entry")
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
        detectorPict?.close()
        // カメラエグゼキュータをシャットダウン
        cameraExecutor.shutdown()
        pictureExecutor.shutdown()
    }

    override fun onPause() {
        super.onPause()
        detector = null
        detectorPict = null
        imageAnalyzer = null
        camera = null
        preview = null
    }

    override fun onResume() {
        super.onResume()
        // アクティビティが再開されたときにパーミッションを確認
        if (allPermissionsGranted()){
            if (detector == null) {
                binding.apply {
                    textView2.visibility = View.VISIBLE
                    textView2.invalidate()
                }
                cameraExecutor.execute {
                    detector = Detector(1, baseContext, MODEL_PATH, LABELS_PATH, this)
                    runOnUiThread {
                        binding.apply {
                            textView2.visibility = View.GONE
                            textView2.invalidate()
                        }
                    }
                }
            }
            startCamera() // パーミッションが許可されていればカメラを開始
            if(detectorPict == null) {
                pictureExecutor.execute {
                    detectorPict = Detector(2, baseContext, MODEL_PATH, LABELS_PATH, this)
                }
            }
        } else {
            // パーミッションが許可されていない場合、リクエストを開始
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    companion object {
        private const val TAG = "Camera" // ログ用のタグ
        private const val REQUEST_CODE_PERMISSIONS = 10 // パーミッションリクエストのコード
        private val REQUIRED_PERMISSIONS = mutableListOf (
            Manifest.permission.CAMERA, // 必要なパーミッションのリスト
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).toTypedArray()
    }

    override fun onEmptyDetect() {
        runOnUiThread {
            // 物体が検出されなかった場合、オーバーレイをクリア
            binding.overlay.clear()
        }
    }

    private fun getMahjong(boundingBoxes: List<BoundingBox>): Player? {
        try {
            // 役の計算
            val tiles = intArrayOf(
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 0, 0,
                0, 0, 0, 0,
                0, 0, 0
            )
            val mapper = IntMapper()
            var last: Tile? = null
            boundingBoxes.forEach {
                tiles[mapper.getInt(it.cls)]++
                last = Tile.valueOf(mapper.getInt(it.cls))
            }
            // 手牌
            val hands = Hands(tiles, last)
            // 場設定
            val general = GeneralSituation(
                // boolean isFirstRound
                false,
                // boolean isHoutei
                false,
                // Tile bakaze
                Tile.TON,
                // List<Tile> dora
                emptyList(),
                // List<Tile> uradora
                emptyList()
            )
            // 個人設定
            val personal = PersonalSituation(
                // boolean isTsumo
                false,
                // boolean isIppatsu
                false,
                // boolean isReach
                false,
                // boolean isDoubleReach
                false,
                // boolean isChankan
                false,
                // boolean isRinshankaihoh
                false,
                // Tile jikaze
                Tile.TON
            )
            val mahjong = Player(hands, general, personal)

            return mahjong
        } catch (e: Exception) {
            when(e) {
                is HandsOverFlowException,
                is IllegalMentsuSizeException,
                is IllegalShuntsuIdentifierException,
                is MahjongTileOverFlowException -> {
                    e.message?.let { Log.e("mahjong", it) }
                    return null
                }
                else -> throw e
            }
        }
    }

    private fun getYaku(mahjong: Player): Array<String> {
        mahjong.calculate()
        val yaku = mahjong.normalYakuList
        val yakuman = mahjong.yakumanList
        val han = mahjong.han
        val fu = mahjong.fu
        val score = mahjong.score
        var count = 0
        val summary = "" + han + "翻 : " + fu + "符 : " + score.ron + "点"
        var details = ""
        yaku?.forEach {
            details += if(count == 0) it.japanese
            else (", " + it.japanese)
            count++
        }
        count = 0
        yakuman?.forEach {
            details += if(count == 0) it.japanese
            else (", " + it.japanese)
            count++
        }
        return arrayOf(summary, details)
    }

    @SuppressLint("SetTextI18n")
    override fun onDetect(id: Int, boundingBoxes: List<BoundingBox>, inferenceTime: Long) {
        if(id == 1) {
            runOnUiThread {
                val mahjong = getMahjong(boundingBoxes)
                val text = mahjong?.let { getYaku(it) }

                // 検出結果をUIに表示
                binding.apply {
                    overlay.setResults(boundingBoxes) // 検出されたバウンディングボックスを設定
                    overlay.invalidate() // オーバーレイを再描画
                    textView4.text = text?.get(0) ?: ""
                    textView5.text = text?.get(1) ?: ""
                    textView4.invalidate()
                    textView5.invalidate()
                }
            }
        } else if(id == 2) {
            runOnUiThread {
                val mahjong = getMahjong(boundingBoxes)
                val text = mahjong?.let { getYaku(it) }

                pictBitmap?.let { bitmap ->
                    // 画像処理
                    resultBitmap = boundingBoxDrawer.drawBoundingBoxes(
                        bitmap,
                        boundingBoxes,
                        text
                    )
                    binding.apply {
                        floatingActionButton.visibility = View.GONE
                        overlay.visibility = View.INVISIBLE
                        textView4.visibility = View.INVISIBLE
                        textView5.visibility = View.INVISIBLE
                        preview.visibility = View.VISIBLE
                        imageView2.setImageBitmap(resultBitmap)
                        floatingActionButton.invalidate()
                        overlay.invalidate()
                        preview.invalidate()
                        imageView2.invalidate()
                    }
                }
            }
        }
    }
}

