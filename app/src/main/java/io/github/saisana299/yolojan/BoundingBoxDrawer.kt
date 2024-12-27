package io.github.saisana299.yolojan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import androidx.core.content.ContextCompat
import yolojan.R

class BoundingBoxDrawer(private val context: Context) {

    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()
    private var bounds = Rect()

    init {
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color)
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 20f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 20f

        boxPaint.color = ContextCompat.getColor(context, R.color.bounding_box_color)
        boxPaint.strokeWidth = 4F
        boxPaint.style = Paint.Style.STROKE
    }

    fun drawBoundingBoxes(originalBitmap: Bitmap, boundingBoxes: List<BoundingBox>): Bitmap {
        // 元のビットマップをコピーして新しいビットマップを作成
        val resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(resultBitmap)

        // 元のビットマップの幅と高さを取得
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // 1:1のアスペクト比でボックスの座標を計算
        val size = Math.min(originalWidth, originalHeight)

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

        return resultBitmap
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}