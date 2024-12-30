package io.github.saisana299.yolojan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import org.mahjong4j.yaku.normals.MahjongYakuEnum
import yolojan.R

class BoundingBoxDrawer(private val context: Context) {

    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var yakuTextBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var yakuTextPaint = Paint()
    private var bounds = Rect()

    init {
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color)
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 30f

        yakuTextBackgroundPaint.color = Color.BLACK
        yakuTextBackgroundPaint.style = Paint.Style.FILL
        yakuTextBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 30f

        yakuTextPaint.color = Color.WHITE
        yakuTextPaint.style = Paint.Style.FILL
        yakuTextPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color)
        boxPaint.strokeWidth = 6F
        boxPaint.style = Paint.Style.STROKE
    }

    @SuppressLint("DefaultLocale")
    fun drawBoundingBoxes(originalBitmap: Bitmap, boundingBoxes: List<BoundingBox>, yaku: List<MahjongYakuEnum>): Bitmap {
        // アスペクト比に応じたターゲットサイズを設定
        val targetHeight: Int = if (originalBitmap.width > originalBitmap.height) {
            1080
        } else {
            1440
        }

        val scaleFactor = targetHeight.toFloat() / originalBitmap.height
        val scaledWidth = (originalBitmap.width * scaleFactor).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            scaledWidth,
            targetHeight,
            true
        )

        // スケーリングしたビットマップをコピーして新しいビットマップを作成
        val resultBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        // 元のビットマップの幅と高さを取得
        val originalWidth = scaledBitmap.width
        val originalHeight = scaledBitmap.height

        // 1:1のアスペクト比でボックスの座標を計算
        val size = originalWidth.coerceAtMost(originalHeight)

        // 中心を基準にボックスの座標を調整
        val offsetX = (originalWidth - size) / 2
        val offsetY = (originalHeight - size) / 2

        boundingBoxes.forEach {
            // 1:1のアスペクト比でボックスの座標を計算
            val leftBox = (it.x1 * size) + offsetX
            val topBox = (it.y1 * size) + offsetY
            val rightBox = (it.x2 * size) + offsetX
            val bottomBox = (it.y2 * size) + offsetY

            // 名前で色を分ける
            if ("萬" in it.clsName) {
                textBackgroundPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color_man)
                boxPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color_man)
            } else if ("筒" in it.clsName) {
                textBackgroundPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color_pin)
                boxPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color_pin)
            } else if ("索" in it.clsName) {
                textBackgroundPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color_sou)
                boxPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color_sou)
            } else {
                textBackgroundPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color)
                boxPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color)
            }

            // ボックスを描画
            canvas.drawRect(leftBox, topBox, rightBox, bottomBox, boxPaint)
            val drawableText = it.clsName + " " + String.format("%.2f", it.cnf)

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                leftBox,
                topBox - textHeight - BOUNDING_RECT_TEXT_PADDING, // ボックスの上に配置
                leftBox + textWidth + BOUNDING_RECT_TEXT_PADDING,
                topBox,
                textBackgroundPaint
            )
            canvas.drawText(drawableText, leftBox, topBox - BOUNDING_RECT_TEXT_PADDING, textPaint)
        }

        // テキスト描画
        var text = ""
        yaku.forEach {
            text = text + it.japanese + ","
        }
        yakuTextBackgroundPaint.getTextBounds(text, 0, text.length, bounds)
        val textWidth = bounds.width()
        val textHeight = bounds.height()
        // 背景の四角をテキストに合わせて描画
        canvas.drawRect(
            BOUNDING_RECT_TEXT_PADDING.toFloat(),
            (originalHeight - textHeight - 3 * BOUNDING_RECT_TEXT_PADDING).toFloat(), // 背景の高さを調整
            (textWidth + 2 * BOUNDING_RECT_TEXT_PADDING).toFloat(),
            (originalHeight - 2 * BOUNDING_RECT_TEXT_PADDING).toFloat(), // 背景の位置を調整
            yakuTextBackgroundPaint
        )
        // テキストを描画
        canvas.drawText(text,
            BOUNDING_RECT_TEXT_PADDING.toFloat(),
            (originalHeight - 2 * BOUNDING_RECT_TEXT_PADDING - 10).toFloat(), yakuTextPaint) // テキストの位置を調整

        return resultBitmap
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}