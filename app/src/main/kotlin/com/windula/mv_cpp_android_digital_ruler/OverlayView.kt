package com.windula.mv_cpp_android_digital_ruler

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Toast

class OverlayView: View {

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var refObjectPXPerCM:Double? = 0.0

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        textSize = 40f
    }
    private val points = mutableListOf<PointF>()
    private var lineDrawn = false

    fun setRefObjectPXPerCM(value:Double){
        refObjectPXPerCM=value
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        points.forEach { point ->
            canvas.drawCircle(point.x, point.y, 10f, pointPaint)
        }
        if (points.size == 2 && !lineDrawn) {
            canvas.drawLine(points[0].x, points[0].y, points[1].x, points[1].y, linePaint)
            lineDrawn = true

            // Calculate the middle point of the line
            val midX = (points[0].x + points[1].x) / 2
            val midY = (points[0].y + points[1].y) / 2

            // Draw text on the line
            canvas.drawText("Text on line", midX, midY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && points.size < 2) {
            points.add(PointF(event.x, event.y))
            invalidate()
            if (points.size == 2) {
                // Get pixel coordinates of the marked points
                val point1 = points[0]
                val point2 = points[1]
                // Do something with the coordinates
                Toast.makeText(context, "point1: ${point1.x} point1: ${point2.x}", Toast.LENGTH_LONG).show()

            }
        }
        return super.onTouchEvent(event)
    }
}
