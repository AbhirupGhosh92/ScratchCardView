package com.example.scratchcardview.customview

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.scratchcardview.R


class ScratchCardView : View {
    private var drawable: Drawable? = null
    private var scratchWidth = 0f
    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private var mPath: Path? = null
    private var mInnerPaint: Paint? = null
    private var mOuterPaint: Paint? = null
    private var mListener: ((scratched : Boolean) -> Unit)? = null
    private var foregroundColour : Int? = null
    private var foregroundDrawable : Int? = null
    private var removePaintOnUp : Boolean = false

    /**
     * We start by defining an OnScratchListener interface. It has a
     * method signature of the method we will implement.
     */
    interface OnScratchListener {
        fun onScratch(scratchCard: ScratchCardView?, visiblePercent: Float)
    }

    /**
     * Then three constructors both with a super() method.
     * @param context
     * @param attrs
     * @param defStyle
     */
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyle: Int
    ) : super(context, attrs, defStyle) {
        resolveAttr(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        resolveAttr(context, attrs)
    }

    constructor(context: Context) : super(context) {
        resolveAttr(context, null)
    }

    /**
     * In our attrs.xml we have defined drawable and scratch width. Let's
     * reference them.
     * @param context
     * @param attrs
     */
    private fun resolveAttr(
        context: Context,
        attrs: AttributeSet?
    ) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ScratchCardView)
        scratchWidth = typedArray.getDimension(R.styleable.ScratchCardView_drawRadius,150.0f)
        foregroundColour = typedArray.getColor(R.styleable.ScratchCardView_foregroundColour,0)
        foregroundDrawable = typedArray.getResourceId(R.styleable.ScratchCardView_foregroundDrawable,0)
        removePaintOnUp = typedArray.getBoolean(R.styleable.ScratchCardView_removePaintOnUp,false)
        typedArray.recycle()
    }

    fun setScratchDrawable(drawable: Drawable?) {
        this.drawable = drawable
    }

    fun setDrawRadius(width: Float) {
        scratchWidth = width
    }

    fun setOnScratchListener(mListener: ((scratched : Boolean) -> Unit)?) {
        this.mListener = mListener
    }

    /**
     * If the size of our ScratchCard changes.
     * @param width - new width
     * @param height - new height
     * @param oldWidth - oldWidth
     * @param oldHeight - old height
     */

    private  fun getBitmap(drawableRes: Int): Bitmap? {
        val drawable = resources.getDrawable(drawableRes)
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onSizeChanged(
        width: Int,
        height: Int,
        oldWidth: Int,
        oldHeight: Int
    ) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        if (bitmap != null) bitmap?.recycle()
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        canvas = Canvas(bitmap!!)
        if (drawable != null) {
            drawable?.setBounds(0, 0, bitmap?.width ?:0, bitmap?.height ?:0)
            drawable?.draw(canvas!!)
        } else {

            if(foregroundColour == null || foregroundColour == 0 )
            {
                if(foregroundDrawable != null && foregroundDrawable !=0)
                {
                   var drawable = getBitmap(foregroundDrawable!!)

                    if(drawable!=null)
                    {
                       canvas?.drawBitmap(drawable,
                           (canvas?.width?.minus(drawable.width))?.div(2)?.toFloat()!!,
                           (canvas?.height?.minus(drawable.height))?.div(2)?.toFloat()!!,mOuterPaint)
                    }
                    else
                    {
                        canvas?.drawColor(context.resources.getColor(R.color.colorPrimary))
                    }
                }
            }
            else
            {
                canvas?.drawColor(foregroundColour!!)
            }
        }
        if (mPath == null) {
            mPath = Path()
        }
        if (mInnerPaint == null) {
            mInnerPaint = Paint()
            mInnerPaint?.isAntiAlias = true
            mInnerPaint?.isDither = true
            mInnerPaint?.style = Paint.Style.STROKE
            mInnerPaint?.isFilterBitmap = true
            mInnerPaint?.strokeJoin = Paint.Join.ROUND
            mInnerPaint?.strokeCap = Paint.Cap.ROUND
            mInnerPaint?.strokeWidth = scratchWidth
            mInnerPaint?.xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        if (mOuterPaint == null) {
            mOuterPaint = Paint()
        }
    }

    private var mLastTouchX = 0f
    private var mLastTouchY = 0f

    /**
     * Now we need to implement a method to allow us handle touch screen
     * motion events.
     * @param event - MotionEvent, an Object used to report movement
     * (mouse, pen, finger, trackball) events.
     * @return
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val currentTouchX = event.x
        val currentTouchY = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                mPath?.reset()
                mPath?.moveTo(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = Math.abs(currentTouchX - mLastTouchX)
                val dy = Math.abs(currentTouchY - mLastTouchY)
                if (dx >= 4 || dy >= 4) {
                    val x1 = mLastTouchX
                    val y1 = mLastTouchY
                    val x2 = (currentTouchX + mLastTouchX) / 2
                    val y2 = (currentTouchY + mLastTouchY) / 2
                    mPath?.quadTo(x1, y1, x2, y2)
                }
            }
            MotionEvent.ACTION_UP -> {
                mPath?.lineTo(currentTouchX, currentTouchY)
                if (mListener != null) {
                    val width = bitmap?.width
                    val height = bitmap?.height
                    val total = width?.times(height ?: 0)
                    var count = 0
                    var i = 0
                    while (i < width ?:0) {
                        var j = 0
                        while (j < height ?:0) {
                            if (bitmap?.getPixel(i, j) == 0x00000000) count++
                            j += 3
                        }
                        i += 3
                    }
                    mListener?.invoke(true)

                    if(removePaintOnUp)
                    {
                        //TODO Remove paint
                    }

                }
            }
        }
        canvas?.drawPath(mPath!!, mInnerPaint!!)
        mLastTouchX = currentTouchX
        mLastTouchY = currentTouchY

        //Invalidate the whole view.
        invalidate()
        return true
    }

    /**
     * Now we implement a method allowing us do the drawing.
     * @param canvas - Canvas Object
     */
    override fun onDraw(canvas: Canvas) {
        canvas.drawBitmap(bitmap!!, 0f, 0f, mOuterPaint)
        super.onDraw(canvas)
    }

    /**
     * Let's override a method to be called when the view is detached from a window.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (bitmap != null) {
            bitmap?.recycle()
            bitmap = null
        }
    }
}