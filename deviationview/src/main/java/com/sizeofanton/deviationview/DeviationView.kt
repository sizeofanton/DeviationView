package com.sizeofanton.deviationview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.IllegalArgumentException
import kotlin.math.min

class DeviationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttrs: Int = 0
) : View(context, attrs, defStyleAttrs) {

    data class Line(var x1: Float, var y1: Float, var x2: Float, var y2: Float)

    private var orientation: Int = 0

    private var primaryLineWidth = 20.0f
    private var secondaryLineWidth = 10.0f

    private var labels = resources.getStringArray(R.array.labels)

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = secondaryLineWidth
    }
    private val centralPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = primaryLineWidth
    }
    private val frontierPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = secondaryLineWidth
    }
    private val contourPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = secondaryLineWidth
    }
    private val fontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 50f
        style = Paint.Style.STROKE
    }
    private val pointerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = primaryLineWidth
    }

    var pointerPosition = 0
        set(value) {
            field = value
            setPosition(value)
        }

    var pointerVisible = false
        set(value) {
            field = value
            invalidate()
        }

    var contourVisible = false
        set(value) {
            field = value
            invalidate()
        }

    private var backgroundRect = Rect(0,0,0,0)
    private var frontierRect = Rect(0, 0, 0, 0)
    private var centralLine: Line = Line(0f, 0f, 0f, 0f)
    private val marksLines = mutableListOf<Line>()
    private val contourLines = Array<Line>(4){Line(0f, 0f, 0f, 0f)}
    private var textPositions = Array(11){Pair(0f, 0f) }
    private val pointerLine: Line = Line(0f, 0f,0f,0f)

    init {
        if (attrs != null) {
            val fromXml = getContext().obtainStyledAttributes(attrs, R.styleable.DeviationView)
            orientation = fromXml.getInt(R.styleable.DeviationView_orientation, 0)
            backgroundPaint.color = fromXml.getInt(
                R.styleable.DeviationView_scaleBgColor,
                ContextCompat.getColor(getContext(), R.color.scaleBackgroundColor)
            )
            centralPaint.color = fromXml.getInt(
                R.styleable.DeviationView_scaleCentralColor,
                ContextCompat.getColor(getContext(), R.color.scaleCentralColor)
            )
            frontierPaint.color = fromXml.getInt(
                R.styleable.DeviationView_scaleFrontierColor,
                ContextCompat.getColor(getContext(), R.color.scaleFrontierColor)
            )
            contourPaint.color = fromXml.getInt(
                R.styleable.DeviationView_scaleContourColor,
                ContextCompat.getColor(getContext(), R.color.scaleContourColor)
            )
            fontPaint.color = fromXml.getInt(
                R.styleable.DeviationView_scaleFontColor,
                ContextCompat.getColor(getContext(), R.color.scaleFontColor)
            )
            pointerPaint.color = fromXml.getInt(
                R.styleable.DeviationView_scalePointerColor,
                ContextCompat.getColor(getContext(), R.color.scaleContourColor)
            )
            pointerVisible = fromXml.getBoolean(
                R.styleable.DeviationView_scalePointerVisibility,
                false
            )
            contourVisible = fromXml.getBoolean(
                R.styleable.DeviationView_scaleContourVisibility,
                false
            )

            fromXml.recycle()
        }
        if (orientation == 1) labels = labels.reversed().toTypedArray()
    }


    private fun setPosition(pos: Int) {
        if (pos !in -50..50)
            throw IllegalArgumentException("Pointer position must be in -50..50 range")
        val pointerPositionInner = if (orientation == 0) {
            when (pos) {
                0 -> 11.0f / 22.0f
                else -> (11.0f / 22.0f) - (10.0f / 22.0f * pos / 50.0f)
            }
        } else {
            when (pos) {
                0 -> 11.0f / 22.0f
                else -> (11.0f / 22.0f) + (10.0f / 22.0f * pos / 50.0f)
            }
        }

        if (orientation == 0) {
            pointerLine.apply {
                x1 = width.toFloat() / 11
                y1 = pointerPositionInner * height
                x2 = 9.0f * width.toFloat() / 11
                y2 = pointerPositionInner * height
            }
        } else {
            pointerLine.apply {
                x1 = pointerPositionInner * width
                y1 = height.toFloat() / 11
                x2 = pointerPositionInner * width
                y2 = 9f * height / 11
            }
        }

        invalidate()
    }

    fun setPointerColor(color: Int) {
        pointerPaint.color = color
        invalidate()
    }

    fun setPointerColor(a: Int, r: Int, g: Int, b: Int) {
        pointerPaint.color = Color.argb(a, r, g, b)
        invalidate()
    }

    fun setContourColor(color: Int) {
        contourPaint.color = color
        invalidate()
    }

    fun setContourColor(a:Int, r: Int, g: Int, b: Int) {
        contourPaint.color = Color.argb(a, r, g, b)
        invalidate()
    }

    fun setScaleBackgroundColor(color: Int) {
        backgroundPaint.color = color
        invalidate()
    }

    fun setScaleBackgroundColor(a: Int, r: Int, g: Int, b: Int) {
        backgroundPaint.color = Color.argb(a, r, g, b)
        invalidate()
    }

    fun setCentralSectorColor(color: Int) {
        centralPaint.color = color
        invalidate()
    }

    fun setCentralSectorColor(a: Int, r: Int, g: Int, b:Int) {
        centralPaint.color = Color.argb(a, r, g, b)
        invalidate()
    }

    fun setFrontierSectorColor(color: Int) {
        frontierPaint.color = color
        invalidate()
    }

    fun setFrontierSectorColor(a: Int, r:Int, g:Int, b:Int) {
        frontierPaint.color = Color.argb(a, r, g, b)
        invalidate()
    }

    fun setFontColor(color: Int) {
        fontPaint.color = color
        invalidate()
    }

    fun setFontColor(a: Int, r: Int, g: Int, b: Int) {
        fontPaint.color = Color.argb(a, r, g, b)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        recalculateLineWidth()
        calculateRects()
        calculateLines()
        calculateFont()
        super.onSizeChanged(w, h, oldw, oldh)
    }

    private fun calculateRects() {
        backgroundRect = if (orientation == 0) Rect(
            (width.toFloat() / 11).toInt(),
            (height.toFloat() / 22).toInt(),
            (9 * width.toFloat() / 11).toInt(),
            (21 * height.toFloat() / 22).toInt()
        )
        else Rect(
            (width.toFloat() / 22).toInt(),
            (height.toFloat() / 11).toInt(),
            (21 * width.toFloat() / 22).toInt(),
            (9 * height.toFloat() / 11).toInt()
        )

        frontierRect = if (orientation == 0) Rect(
            (width.toFloat() / 11).toInt(),
            (9 * height.toFloat() / 22).toInt(),
            (9 * width.toFloat() / 11).toInt(),
            (13 * height.toFloat() / 22).toInt()
        ) else Rect(
            (9 * width.toFloat() / 22).toInt(),
            (1 * height.toFloat() / 11).toInt(),
            (13 * width.toFloat() / 22).toInt(),
            (9 * height.toFloat() / 11).toInt()
        )
    }

    private fun calculateLines() {
        centralLine = if (orientation == 0 ) Line(
            width.toFloat() / 11,
            11 * height.toFloat() / 22,
            9 * width.toFloat() / 11,
            11 * height.toFloat() / 22
        ) else Line (
            11 * width.toFloat() / 22,
            height.toFloat() / 11,
            11 * width.toFloat() / 22,
            9 * height.toFloat() / 11
        )

        if (marksLines.isNotEmpty()) marksLines.clear()
        if (orientation == 0) {
            for (i in 0..9)
                marksLines.add(
                    Line(
                    width.toFloat() / 11,
                    (2 + 2 * i) * height.toFloat() / 22,
                    2 * width.toFloat() / 11,
                    (2 + 2 * i) * height.toFloat() / 22
                    )
                )
            for (i in 0..9)
                marksLines.add(
                    Line(
                    8 * width.toFloat() / 11,
                    (2 + 2 * i) * height.toFloat() / 22,
                    9 * width.toFloat() / 11,
                    (2 + 2 * i) * height.toFloat() / 22
                    )
                )
            for (i in 1..10)
                marksLines.add(
                    Line(
                    width.toFloat() / 11,
                    (2f * i + 0.5f) * height.toFloat() / 22,
                    1.5f * width.toFloat() / 11,
                    (2f * i + 0.5f) * height.toFloat() / 22
                    )
                )
            for (i in 1..10)
                marksLines.add(
                    Line(
                    width.toFloat() / 11,
                    (2f * i - 0.5f) * height.toFloat() / 22,
                    1.5f * width.toFloat() / 11,
                    (2f * i - 0.5f) * height.toFloat() / 22
                    )
                )
            for (i in 1..10)
                marksLines.add(
                    Line(
                    8.5f * width.toFloat() / 11,
                    (2f * i + 0.5f) * height.toFloat() / 22,
                    9f * width.toFloat() / 11,
                    (2f * i + 0.5f) * height.toFloat() / 22
                    )
                )
            for (i in 1..10)
                marksLines.add(
                    Line(
                    8.5f * width.toFloat() / 11,
                    (2f * i - 0.5f) * height.toFloat() / 22,
                    9f * width.toFloat() / 11,
                    (2f * i - 0.5f) * height.toFloat() / 22
                    )
                )

            contourLines[0] = Line(
                width.toFloat() / 11,
                height.toFloat() / 22,
                9 * width.toFloat() / 11,
                height.toFloat() / 22
            )
            contourLines[1] = Line(
                9 * width.toFloat() / 11,
                height.toFloat() / 22,
                9 * width.toFloat() / 11,
                21 * height.toFloat() / 22
            )
            contourLines[2] = Line(
                9 * width.toFloat() / 11,
                21 * height.toFloat() / 22,
                width.toFloat() / 11,
                21 * height.toFloat() / 22
            )
            contourLines[3] = Line(
                width.toFloat() / 11,
                21 * height.toFloat() / 22,
                width.toFloat() / 11,
                height.toFloat() / 22
            )

        } else {
            for (i in 0..9) {
                marksLines.add(
                    Line(
                    (2 + 2 * i) * width.toFloat() / 22,
                    height.toFloat() / 11,
                    (2 + 2 * i) * width.toFloat() / 22,
                    2f * height.toFloat() / 11
                    )
                )
            }
            for (i in 0..9) {
                marksLines.add(
                    Line(
                    (2 + 2 * i) * width.toFloat() / 22,
                    8f * height.toFloat() / 11,
                    (2 + 2 * i) * width.toFloat() / 22,
                    9f * height.toFloat() / 11
                    )
                )
            }
            for (i in 1..10) {
                marksLines.add(
                    Line(
                    (2f * i - 0.5f) * width.toFloat() / 22,
                    height.toFloat() / 11,
                    (2f * i - 0.5f) * width.toFloat() / 22,
                    1.5f * height.toFloat() / 11
                    )
                )
            }

            for (i in 1..10) {
                marksLines.add(
                    Line(
                    (2f * i - 0.5f) * width.toFloat() / 22,
                    8.5f * height.toFloat() / 11,
                    (2f * i - 0.5f) * width.toFloat() / 22,
                    9f * height.toFloat() / 11
                    )
                )
            }

            for (i in 1..10) {
                marksLines.add(
                    Line(
                    (2f * i + 0.5f) * width.toFloat() / 22,
                    height.toFloat() / 11,
                    (2f * i + 0.5f) * width.toFloat() / 22,
                    1.5f * height.toFloat() / 11
                    )
                )
            }

            for (i in 1..10) {
                marksLines.add(
                    Line(
                    (2f * i + 0.5f) * width.toFloat() / 22,
                    8.5f * height.toFloat() / 11,
                    (2f * i + 0.5f) * width.toFloat() / 22,
                    9f * height.toFloat() / 11
                    )
                )
            }

            contourLines[0] = Line(
                width.toFloat() / 22,
                height.toFloat() / 11,
                21 * width.toFloat() / 22,
                height.toFloat() / 11
            )
            contourLines[1] = Line(
                21 * width.toFloat() / 22,
                height.toFloat() / 11,
                21 * width.toFloat() / 22,
                9 * height.toFloat() / 11
            )
            contourLines[2] = Line(
                21 * width.toFloat() / 22,
                9 * height.toFloat() / 11,
                width.toFloat() / 22,
                9 * height.toFloat() / 11
            )
            contourLines[3] = Line(
                width.toFloat() / 22,
                9 * height.toFloat() / 11,
                width.toFloat() / 22,
                height.toFloat() / 11
            )
        }
    }


    private fun calculateFont(){
        if (orientation == 0) {
            fontPaint.textSize = height / 42f
            for (i in labels.indices)
                textPositions[i] = Pair(9.5f * width / 11, (1f + 2f * i) * height / 22)
        } else {
            fontPaint.textSize = height / 21f
            for (i in labels.indices)
                textPositions[i] = Pair((0.75f + 2f * i) * width / 22, 10f * height / 11)
        }
    }

    private fun recalculateLineWidth() {
        if (orientation == 0) {
            primaryLineWidth = height / 107.5f
            secondaryLineWidth = height / 215.0f
        } else {
            primaryLineWidth = height / 53.75f
            secondaryLineWidth = height / 107.5f
        }
        reloadPaints()
    }


    private fun reloadPaints() {
        backgroundPaint.strokeWidth = secondaryLineWidth
        centralPaint.strokeWidth = primaryLineWidth
        frontierPaint.strokeWidth = secondaryLineWidth
        contourPaint.strokeWidth = secondaryLineWidth
        pointerPaint.strokeWidth = primaryLineWidth
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredWidth = suggestedMinimumWidth + paddingLeft + paddingRight
        val desiredHeight = suggestedMinimumHeight + paddingTop + paddingBottom

        setMeasuredDimension(
            measureDimension(desiredWidth, widthMeasureSpec),
            measureDimension(desiredHeight, heightMeasureSpec)
        )
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)

        return if (specMode == MeasureSpec.EXACTLY) {
            specSize
        } else {
            if (specMode == MeasureSpec.AT_MOST) min(desiredSize, specSize)
            desiredSize
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(backgroundRect, backgroundPaint)
        canvas.drawRect(frontierRect, frontierPaint)
        canvas.drawLine(centralLine.x1, centralLine.y1, centralLine.x2, centralLine.y2, centralPaint)
        for (i in labels.indices)
            canvas.drawText(labels[i], textPositions[i].first, textPositions[i].second, fontPaint)
        marksLines.forEach {
            canvas.drawLine(it.x1, it.y1, it.x2, it.y2, contourPaint)
        }
        if (pointerVisible)
            canvas.drawLine(pointerLine.x1, pointerLine.y1, pointerLine.x2, pointerLine.y2, pointerPaint)
        if (contourVisible)
            contourLines.forEach {
                canvas.drawLine(it.x1, it.y1, it.x2, it.y2, contourPaint)
            }
        super.onDraw(canvas)
    }
}
