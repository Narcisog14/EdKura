package com.example.edkura.Jiankai.jiankaiUI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.Path


class CustomCanvasView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    // 在这里定义你的画笔
    private val paint = Paint().apply {
        color = Color.rgb(193, 255, 193)  // 设置画笔颜色
        style = Paint.Style.FILL  // 填充样式
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val height = height.toFloat()
        val viewWidth = width.toFloat()
        val rectWidth = viewWidth * 0.1f
        val sideHeight = height *0.05f
        val squareHeight = height*0.05f
        val squareWidth = viewWidth*0.24f
        // 画一个矩形
        canvas.drawRect(0f, height, rectWidth, 0f, paint)
        // 画一个路径（自定义图形）
        val path = Path().apply {
            val cornerRadius = squareWidth*0.3f  // 你设置的圆角半径

            // 开始于左上角的圆角部分 (向上延伸)
            moveTo(rectWidth, squareHeight - cornerRadius)
            quadTo(
                rectWidth, squareHeight,
                rectWidth + cornerRadius, squareHeight
            )

            // 右上角的圆角
            lineTo(rectWidth + squareWidth - cornerRadius, squareHeight)
            quadTo(
                rectWidth + squareWidth, squareHeight,
                rectWidth + squareWidth, squareHeight + cornerRadius
            )

            // 右下角的圆角
            lineTo(rectWidth + squareWidth, squareHeight + squareWidth - cornerRadius)
            quadTo(
                rectWidth + squareWidth, squareHeight + squareWidth,
                rectWidth + squareWidth - cornerRadius, squareHeight + squareWidth
            )

            // 左下角的圆角 (向下延伸)
            lineTo(rectWidth + cornerRadius, squareHeight + squareWidth)
            quadTo(
                rectWidth, squareHeight + squareWidth,
                rectWidth, squareHeight + squareWidth + cornerRadius
            )

            // 连接回起点

        }
        canvas.drawPath(path, paint)
    }
}