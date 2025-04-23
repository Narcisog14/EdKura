package com.example.edkura.Jiankai.jiankaiUI

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.Path
import android.view.GestureDetector
import android.view.MotionEvent
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.ViewConfiguration
import androidx.core.content.ContextCompat
import com.example.edkura.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*


class CustomCanvasView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.rgb(193, 255, 193)  // 绿色
        style = Paint.Style.FILL
    }

    private val gestureDetector = GestureDetector(context, GestureListener())
    private var rectWidthFactor = 0.05f  // 初始宽度 5%
    private var maxRectWidthFactor = 0.7f  // 最大宽度 70%
    private var rectWidth = 0f
    private var isRectOpen = false  // 标志：矩形是否展开
    private var initialTouchX = 0f  // 记录触摸起始位置
    private var initialTouchY = 0f  // 记录触摸起始Y位置
    private var touchStartedInRect = false // 记录触摸是否开始于矩形内部
    private var isCurrentlyScrolling = false // 标记当前是否正在滑动中
    private var isExpandingRect = false // 标记是否正在展开矩形的过程中
    private var isDraggingRect = false // 标记是否正在拖拽矩形
    private var overlayAlpha = 0 // 遮罩层透明度 (0-255)
    private var touchMoved = false // 标记触摸是否移动过
    private var isVerticalScroll = false // 标记是否为垂直滑动
    private var isHorizontalScroll = false // 标记是否为水平滑动

    // 滑动方向判断的阈值
    private val TOUCH_SLOP = ViewConfiguration.get(context).scaledTouchSlop

    // 动画相关
    private var animator: ValueAnimator? = null

    // 矩形是否打开时的遮罩画布
    private val overlayPaint = Paint().apply {
        color = Color.BLACK  // 黑色，透明度会动态调整
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val height = height.toFloat()
        val viewWidth = width.toFloat()  // 使用布局的宽度
        rectWidth = viewWidth * rectWidthFactor  // 矩形宽度依据视图宽度进行动态调整



        // 绘制矩形
        canvas.drawRect(0f, 0f, rectWidth, height, paint)

        // 绘制透明度渐变的遮罩层
        if (overlayAlpha > 0) {
            overlayPaint.alpha = overlayAlpha
            canvas.drawRect(rectWidth, 0f, viewWidth, height, overlayPaint)
        }
        // 绘制固定路径图形
        drawFixedPath(canvas, rectWidth)
    }

    // 改进的触摸事件处理
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 取消任何正在进行的动画
                animator?.cancel()

                initialTouchX = event.x
                initialTouchY = event.y
                touchStartedInRect = event.x < rectWidth
                isCurrentlyScrolling = false // 重置滑动标记
                touchMoved = false // 重置移动标记
                isDraggingRect = false // 重置拖拽标记
                isVerticalScroll = false // 重置垂直滑动标记
                isHorizontalScroll = false // 重置水平滑动标记

                // 如果矩形已打开且点击在矩形外部，且不在展开过程中，则关闭矩形并消费事件
                if (isRectOpen && !isExpandingRect && event.x > rectWidth && !isCurrentlyScrolling) {
                    return true // 先不进行任何动作，等待ACTION_UP或滑动事件
                }

                // 当矩形没有打开且点击在矩形内部，或点击在固定路径图形内，才拦截事件
                return touchStartedInRect || isInsideFixedPath(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> {
                // 检测移动方向和距离
                val dx = event.x - initialTouchX
                val dy = event.y - initialTouchY

                // 如果尚未确定滑动方向
                if (!isVerticalScroll && !isHorizontalScroll) {
                    // 确定滑动方向
                    if (Math.abs(dx) > TOUCH_SLOP || Math.abs(dy) > TOUCH_SLOP) {
                        touchMoved = true

                        // 如果水平方向距离大于垂直方向，则认为是水平滑动
                        if (Math.abs(dx) > Math.abs(dy)) {
                            isHorizontalScroll = true
                            isCurrentlyScrolling = true
                        } else {
                            isVerticalScroll = true
                            // 如果是垂直滑动，且不是在矩形内部开始的触摸，则不处理该事件
                            if (!touchStartedInRect && !isRectOpen) {
                                return false
                            }
                        }
                    }
                }

                // 如果已确定为垂直滑动，则不处理事件，让父视图处理
                if (isVerticalScroll) {
                    return false
                }

                // 如果已确定为水平滑动
                if (isHorizontalScroll) {
                    // 向右滑动且矩形未打开
                    if (dx > 10 && !isRectOpen) {
                        isExpandingRect = true
                        isDraggingRect = true

                        // 根据滑动距离计算矩形宽度因子，限制最大值为70%
                        rectWidthFactor = (0.05f + dx / width).coerceIn(0.05f, maxRectWidthFactor)

                        // 计算当前展开比例，动态调整遮罩透明度
                        val openProgress = ((rectWidthFactor - 0.05f) / (maxRectWidthFactor - 0.05f)).coerceIn(0f, 1f)
                        overlayAlpha = (openProgress * 100).toInt() // 最大透明度为100
                        notifyRectWidthChanged()

                        invalidate()
                        return true
                    }

                    // 向左滑动且矩形已打开
                    if (dx < -20 && isRectOpen) {
                        isDraggingRect = true

                        // 根据滑动距离动态调整矩形宽度
                        val newWidth = (maxRectWidthFactor + dx / width).coerceIn(0.05f, maxRectWidthFactor)

                        // 如果用户已经滑动很远（接近关闭），标记矩形为关闭状态
                        if (newWidth <= 0.1f) {
                            rectWidthFactor = 0.05f
                            overlayAlpha = 0
                            isRectOpen = false
                        } else {
                            rectWidthFactor = newWidth

                            // 计算关闭进度，动态调整遮罩透明度
                            val closeProgress = ((rectWidthFactor - 0.05f) / (maxRectWidthFactor - 0.05f)).coerceIn(0f, 1f)
                            overlayAlpha = (closeProgress * 100).toInt()
                            notifyRectWidthChanged()
                        }

                        invalidate()
                        return true
                    }
                }

                // 如果矩形已打开且触摸开始于矩形内部，则处理手势
                if (isRectOpen && touchStartedInRect) {
                    return gestureDetector.onTouchEvent(event)
                }

                // 默认不拦截
                return false
            }

            MotionEvent.ACTION_UP -> {
                // 如果正在拖拽矩形
                if (isDraggingRect) {
                    val openThreshold = maxRectWidthFactor / 3  // 打开阈值
                    val closeThreshold = maxRectWidthFactor * 2 / 3  // 关闭阈值

                    if (!isRectOpen) {
                        // 当前是关闭状态，判断是否要打开
                        if (rectWidthFactor > openThreshold) {
                            animateRectangle(rectWidthFactor, maxRectWidthFactor, overlayAlpha, 100)
                            isRectOpen = true
                        } else {
                            animateRectangle(rectWidthFactor, 0.05f, overlayAlpha, 0)
                            isRectOpen = false
                        }
                    } else {
                        // 当前是打开状态，判断是否要关闭
                        if (rectWidthFactor < closeThreshold) {
                            animateRectangle(rectWidthFactor, 0.05f, overlayAlpha, 0)
                            isRectOpen = false
                        } else {
                            animateRectangle(rectWidthFactor, maxRectWidthFactor, overlayAlpha, 100)
                            isRectOpen = true
                        }
                    }

                    isDraggingRect = false
                    isExpandingRect = false
                    return true
                }

                // 如果手指没有移动过且是有效的点击事件
                if (!touchMoved && Math.abs(event.x - initialTouchX) < 10 && Math.abs(event.y - initialTouchY) < 10) {
                    // 点击固定路径图形区域
                    if (isInsideFixedPath(event.x, event.y)) {
                        if (!isRectOpen) {
                            animateRectangle(rectWidthFactor, maxRectWidthFactor, overlayAlpha, 100)
                            isRectOpen = true
                            return true
                        }
                    }
                    // 如果矩形已打开且点击在遮罩区域且不在展开过程中
                    else if (isRectOpen && !isExpandingRect && event.x > rectWidth && !touchStartedInRect) {
                        animateRectangle(rectWidthFactor, 0.05f, overlayAlpha, 0)
                        isRectOpen = false
                        return true
                    }
                }

                isCurrentlyScrolling = false
                touchMoved = false
                isDraggingRect = false
                isExpandingRect = false
                isVerticalScroll = false
                isHorizontalScroll = false

                // 如果矩形已打开且点击在矩形内部，则消费事件
                if (isRectOpen && touchStartedInRect) {
                    return true
                }

                // 点击在固定路径图形区域也消费事件
                if (isInsideFixedPath(event.x, event.y)) {
                    return true
                }

                // 其他情况，不拦截事件，让父视图处理
                return false
            }

            MotionEvent.ACTION_CANCEL -> {
                isCurrentlyScrolling = false
                isExpandingRect = false
                touchMoved = false
                isDraggingRect = false
                isVerticalScroll = false
                isHorizontalScroll = false
                invalidate()
                return false
            }
        }

        // 如果矩形已打开且触摸开始于矩形内部，则处理手势
        if (isRectOpen && touchStartedInRect) {
            return gestureDetector.onTouchEvent(event)
        }

        // 其他情况，不拦截事件，让父视图处理
        return false
    }

    // 添加同时控制矩形宽度和遮罩透明度的动画效果
    private fun animateRectangle(fromWidth: Float, toWidth: Float, fromAlpha: Int, toAlpha: Int) {
        animator?.cancel()

        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200 // 动画持续时间200毫秒
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val fraction = animation.animatedValue as Float
                // 更新矩形宽度
                rectWidthFactor = fromWidth + (toWidth - fromWidth) * fraction
                // 更新遮罩层透明度
                overlayAlpha = fromAlpha + ((toAlpha - fromAlpha) * fraction).toInt()
                invalidate()
                notifyRectWidthChanged()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // 动画结束后，如果是展开到最大宽度，设置isRectOpen为true
                    if (toWidth >= maxRectWidthFactor) {
                        isRectOpen = true
                    } else if (toWidth <= 0.05f) {
                        isRectOpen = false
                    }
                    isExpandingRect = false
                    isDraggingRect = false
                    // 确保最终值准确
                    rectWidthFactor = toWidth
                    overlayAlpha = toAlpha
                    notifyRectWidthChanged()
                    invalidate()
                }
            })
            start()
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 != null && !isDraggingRect) {
                // 检查是水平还是垂直快划
                val distanceX = e2.x - e1.x
                val distanceY = e2.y - e1.y

                // 如果垂直速度大于水平速度，则认为是垂直快划，不处理
                if (Math.abs(velocityY) > Math.abs(velocityX) && Math.abs(distanceY) > Math.abs(distanceX)) {
                    return false
                }
            }
            return false
        }

        override fun onScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            // 如果已经在直接拖拽模式下，让主触摸事件处理器处理
            if (isDraggingRect) {
                return false
            }

            if (e1 != null) {
                // 检查是水平滑动还是垂直滑动
                if (Math.abs(distanceY) > Math.abs(distanceX) * 1.5) {
                    // 明显的垂直滑动，不拦截事件
                    isVerticalScroll = true
                    return false
                }

                val distance = e2.x - e1.x  // 计算滑动距离

                // 标记为滑动状态和已移动状态
                isCurrentlyScrolling = true
                touchMoved = true
                isHorizontalScroll = true

                // 如果矩形已打开，且在矩形内部开始滑动，处理向左滑动（关闭矩形）
                if (isRectOpen && touchStartedInRect) {
                    if (distance < -20) {  // 向左滑动超过阈值
                        isDraggingRect = true

                        // 根据滑动距离动态调整矩形宽度
                        rectWidthFactor = (maxRectWidthFactor + distance / width).coerceIn(0.05f, maxRectWidthFactor)

                        // 计算关闭进度，动态调整遮罩透明度
                        val closeProgress = ((rectWidthFactor - 0.05f) / (maxRectWidthFactor - 0.05f)).coerceIn(0f, 1f)
                        overlayAlpha = (closeProgress * 100).toInt()

                        invalidate()
                        notifyRectWidthChanged()
                        return true
                    }
                }
                // 矩形已打开且在遮罩区域开始滑动，处理向左滑动关闭矩形
                else if (isRectOpen && !touchStartedInRect) {
                    if (distance < -20) {  // 向左滑动超过阈值
                        isDraggingRect = true

                        // 根据滑动距离动态调整矩形宽度
                        rectWidthFactor = (maxRectWidthFactor + distance / width).coerceIn(0.05f, maxRectWidthFactor)

                        // 计算关闭进度，动态调整遮罩透明度
                        val closeProgress = ((rectWidthFactor - 0.05f) / (maxRectWidthFactor - 0.05f)).coerceIn(0f, 1f)
                        overlayAlpha = (closeProgress * 100).toInt()
                        notifyRectWidthChanged()
                        invalidate()
                        return true
                    }
                }
                // 矩形未打开
                else if (!isRectOpen) {
                    if (distance > 20) {  // 向右滑动超过阈值
                        isDraggingRect = true
                        isExpandingRect = true

                        // 根据滑动距离计算矩形宽度因子，限制最大值为70%
                        rectWidthFactor = (0.05f + distance / width).coerceIn(0.05f, maxRectWidthFactor)

                        // 计算当前展开比例，动态调整遮罩透明度
                        val openProgress = ((rectWidthFactor - 0.05f) / (maxRectWidthFactor - 0.05f)).coerceIn(0f, 1f)
                        overlayAlpha = (openProgress * 100).toInt() // 最大透明度为100
                        notifyRectWidthChanged()
                        invalidate()
                        return true
                    }
                }
            }
            return false
        }

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }

    // 重写dispatchTouchEvent来更好地控制事件分发
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // 当ACTION_DOWN时，始终接收事件以便判断是否要处理
        if (event.action == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return super.dispatchTouchEvent(event)
        }

        // 如果已确定为垂直滑动，让父视图拦截
        if (isVerticalScroll) {
            parent?.requestDisallowInterceptTouchEvent(false)
            return super.dispatchTouchEvent(event)
        }

        // 如果已确定为水平滑动，阻止父视图拦截
        if (isHorizontalScroll) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return super.dispatchTouchEvent(event)
        }

        return super.dispatchTouchEvent(event)
    }

    // 绘制固定路径图形
    private fun drawFixedPath(canvas: Canvas, rectWidth: Float) {
        val height = height.toFloat()
        val squareHeight = height * 0.05f
        val squareWidth = width * 0.15f
        val cornerRadius = squareWidth * 0.3f

        val path = Path().apply {
            moveTo(rectWidth-1, squareHeight - cornerRadius)
            quadTo(rectWidth, squareHeight, rectWidth + cornerRadius, squareHeight)

            lineTo(rectWidth + squareWidth - cornerRadius, squareHeight)
            quadTo(rectWidth + squareWidth, squareHeight, rectWidth + squareWidth, squareHeight + cornerRadius)

            lineTo(rectWidth + squareWidth, squareHeight + squareWidth - cornerRadius)
            quadTo(rectWidth + squareWidth, squareHeight + squareWidth, rectWidth + squareWidth - cornerRadius, squareHeight + squareWidth)

            lineTo(rectWidth + cornerRadius, squareHeight + squareWidth)
            quadTo(rectWidth-1, squareHeight + squareWidth, rectWidth, squareHeight + squareWidth + cornerRadius)
        }
        canvas.drawPath(path, Paint().apply { color = Color.TRANSPARENT })  // 如果你希望路径不被填充，可以使用透明颜色

// 加载图案
        val drawable = ContextCompat.getDrawable(context, R.drawable.profile_icon)
        val bitmap = (drawable as BitmapDrawable).bitmap

// 计算缩放因子
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

// 将 squareWidth 减小，确保图像区域更小
        val adjustedWidth = squareWidth * 0.8f  // 例如，减小 20% 的大小

        val scaleX = adjustedWidth / bitmapWidth
        val scaleY = adjustedWidth / bitmapHeight
        val scale = minOf(scaleX, scaleY) // 选择合适的缩放比例

// 使用 Matrix 缩放图片
        val matrix = Matrix().apply {
            setScale(scale, scale) // 缩放比例
        }

// 计算图片居中偏移量
        val scaledBitmapWidth = bitmapWidth * scale
        val scaledBitmapHeight = bitmapHeight * scale

        val offsetX = (squareWidth - scaledBitmapWidth) / 2
        val offsetY = (squareWidth - scaledBitmapHeight) / 2

// 更新目标区域的位置，使图片居中
        val centeredBitmapRect = RectF(
            rectWidth + offsetX,
            squareHeight + offsetY,
            rectWidth + offsetX + scaledBitmapWidth,
            squareHeight + offsetY + scaledBitmapHeight
        )

// 绘制缩放后的图片
        canvas.drawPath(path, paint)
        canvas.drawBitmap(bitmap, null, centeredBitmapRect, null)

    }

    // 判断触摸点是否在固定路径图形区域内
    private fun isInsideFixedPath(x: Float, y: Float): Boolean {
        val height = height.toFloat()
        val squareHeight = height * 0.05f
        val squareWidth = width * 0.15f
        return x > rectWidth && x < rectWidth + squareWidth && y > squareHeight && y < squareHeight + squareWidth
    }

    // 提供重置矩形的方法
    fun resetRectangle() {
        animateRectangle(rectWidthFactor, 0.05f, overlayAlpha, 0)
    }

    fun getCurrentRectWidth(): Float {
        return rectWidthFactor
    }

    // Optional: Add a listener to notify when rectWidth changes
    interface OnRectWidthChangeListener {
        fun onRectWidthChanged(rectWidth: Float)
    }

    private var rectWidthChangeListener: OnRectWidthChangeListener? = null

    fun setOnRectWidthChangeListener(listener: OnRectWidthChangeListener) {
        rectWidthChangeListener = listener
    }

    // Call this whenever rectWidth changes (in onDraw, animateRectangle, etc.)
    private fun notifyRectWidthChanged() {
        rectWidthChangeListener?.onRectWidthChanged(rectWidth)
    }
}
