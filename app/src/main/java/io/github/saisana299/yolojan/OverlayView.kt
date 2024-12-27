package io.github.saisana299.yolojan

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import yolojan.R


class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>()
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        results = listOf()
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 20f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 20f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 4F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            val left = it.x1 * width
            val top = it.y1 * height
            val right = it.x2 * width
            val bottom = it.y2 * height

            // 名前で色を分ける
            if("萬" in it.clsName) {
                textBackgroundPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color_man)
                boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color_man)
            } else if("筒" in it.clsName) {
                textBackgroundPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color_pin)
                boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color_pin)
            } else if("索" in it.clsName) {
                textBackgroundPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color_sou)
                boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color_sou)
            } else {
                textBackgroundPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
                boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
            }


            canvas.drawRect(left, top, right, bottom, boxPaint)
            val drawableText = it.clsName + " " + String.format("%.2f", it.cnf)

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top - textHeight - BOUNDING_RECT_TEXT_PADDING, // ボックスの上に配置
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top,
                textBackgroundPaint
            )
            canvas.drawText(drawableText, left, top - BOUNDING_RECT_TEXT_PADDING, textPaint)

        }
    }

    fun setResults(boundingBoxes: List<BoundingBox>) {
        results = boundingBoxes
        invalidate()
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}