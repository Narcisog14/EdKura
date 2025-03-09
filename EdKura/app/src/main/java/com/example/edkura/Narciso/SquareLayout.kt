package com.example.edkura.Narciso

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout

class SquareLayout : LinearLayout {
    // Desired width-to-height ratio - 1.0 for square
    private val mScale = 1.0

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        val newWidth: Int
        val newHeight: Int

        if (width > (mScale * height + 0.5)) {
            newWidth = (mScale * height + 0.5).toInt()
            newHeight = height
        } else {
            newWidth = width
            newHeight = (width / mScale + 0.5).toInt()
        }

        super.onMeasure(
            MeasureSpec.makeMeasureSpec(newWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(newHeight, MeasureSpec.EXACTLY)
        )
    }
}