package com.sizeofanton.deviationview

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.min

class DeviationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttrs: Int = 0
) : View(context, attrs, defStyleAttrs) {

    data class Line(
        var x1: Float,
        var y1: Float,
        var x2: Float,
        var y2: Float
    )

    companion object {
        const val UPPER_MAX = 50
        const val LOWER_MAX = -50
        const val CENTER = 0

        private const val VERTICAL_ORIENTATION = 0
        private const val HORIZONTAL_ORIENTATION = 1

        private const val DEFAULT_PRIMARY_LINE_WIDTH = 20.0f
        private const val DEFAULT_SECONDARY_LINE_WIDTH = 10.0f

        private const val DEFAULT_TEXT_SIZE = 50.0f
        private const val POSITION_PROPORTION = 50.0f

        private const val VERTICAL_TOTAL = 22.0f
        private const val VERTICAL_CENTRAL = VERTICAL_TOTAL / 2
        private const val VERTICAL_AVAILABLE = VERTICAL_CENTRAL - 1
        private const val VERTICAL_BACKGROUND = VERTICAL_TOTAL - 1
        private const val VERTICAL_INNER_LIMIT_TOP = VERTICAL_AVAILABLE - 1
        private const val VERTICAL_INNER_LIMIT_BOTTOM = VERTICAL_CENTRAL + 2

        private const val HORIZONTAL_TOTAL = 11.0f
        private const val HORIZONTAL_END = HORIZONTAL_TOTAL - 3.0f
        private const val HORIZONTAL_INNER_LIMIT_LEFT = 9.0f
        private const val HORIZONTAL_INNER_LIMIT_RIGHT = 13.0f
        private const val HORIZONTAL_INNER_LIMIT_TOP = 1.0f
        private const val HORIZONTAL_INNER_LIMIT_BOTTOM = 9.0f

        private const val NUMBER_OF_MARKS = 10
        private const val MARK_BASE_PROPORTION = 2f
        private const val MARK_LARGE_OFFSET = 2f
        private const val MARK_SMALL_OFFSET = 0.5f
        private const val LEFT_LARGE_MARK_X1 = 1
        private const val LEFT_LARGE_MARK_X2 = 2
        private const val RIGHT_LARGE_MARK_X1 = 8
        private const val RIGHT_LARGE_MARK_X2 = 9
        private const val LEFT_SMALL_MARK_X1 = 1.0f
        private const val LEFT_SMALL_MARK_X2 = 1.5f
        private const val RIGHT_SMALL_MARK_X1 = 8.5f
        private const val RIGHT_SMALL_MARK_X2 = 9f

        private const val VERTICAL_PRIMARY_LINE_PROPORTION = 107.5f
        private const val VERTICAL_SECONDARY_LINE_PROPORTION = 215.0f
        private const val HORIZONTAL_PRIMARY_LINE_PROPORTION = 53.75f
        private const val HORIZONTAL_SECONDARY_LINE_PROPORTION = 107.5f

        private const val VERTICAL_FONT_PROPORTION = 42.0f
        private const val HORIZONTAL_FONT_PROPORTION = 21.0f
        private const val VERTICAL_FONT_X_POSITION = 9.5f
        private const val VERTICAL_FONT_Y_POSITION = 3f
        private const val HORIZONTAL_FONT_X_POSITION = 2.75f
        private const val HORIZONTAL_FONT_Y_POSITION = 10f

    }

    private var orientation: Int = VERTICAL_ORIENTATION

    private var primaryLineWidth = DEFAULT_PRIMARY_LINE_WIDTH
    private var secondaryLineWidth = DEFAULT_SECONDARY_LINE_WIDTH

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
        textSize = DEFAULT_TEXT_SIZE
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

    private var contourVisible = false
        set(value) {
            field = value
            invalidate()
        }

    private var backgroundRect = Rect(0,0,0,0)
    private var frontierRect = Rect(0, 0, 0, 0)
    private var centralLine: Line = Line(0f, 0f, 0f, 0f)
    private val marksLines = mutableListOf<Line>()
    private val contourLines = Array<Line>(4){ Line(0f, 0f, 0f, 0f) }
    private var textPositions = Array(11){ Pair(0f, 0f) }
    private val pointerLine: Line = Line(0f, 0f,0f,0f)

    init {
        if (attrs != null) {
            val fromXml = getContext().obtainStyledAttributes(attrs, R.styleable.DeviationView)
            orientation = fromXml.getInt(R.styleable.DeviationView_orientation, VERTICAL_ORIENTATION)
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
        if (orientation == HORIZONTAL_ORIENTATION) labels = labels.reversed().toTypedArray()
    }


    private fun setPosition(pos: Int) {
        if (pos < LOWER_MAX) {
            setPosition(LOWER_MAX)
            return
        }
        else if (pos > UPPER_MAX) {
            setPosition(UPPER_MAX)
            return
        }
        val pointerPositionInner = if (orientation == VERTICAL_ORIENTATION) {
            when (pos) {
                CENTER -> VERTICAL_CENTRAL / VERTICAL_TOTAL
                else -> (VERTICAL_CENTRAL / VERTICAL_TOTAL) -
                        (VERTICAL_AVAILABLE / VERTICAL_TOTAL * pos / POSITION_PROPORTION)
            }
        } else {
            when (pos) {
                CENTER -> VERTICAL_CENTRAL / VERTICAL_TOTAL
                else -> (VERTICAL_CENTRAL / VERTICAL_TOTAL) +
                        (VERTICAL_AVAILABLE / VERTICAL_TOTAL * pos / POSITION_PROPORTION)
            }
        }

        if (orientation == VERTICAL_ORIENTATION) {
            pointerLine.apply {
                x1 = width.toFloat() / VERTICAL_TOTAL
                y1 = pointerPositionInner * height
                x2 = HORIZONTAL_END * width.toFloat() / HORIZONTAL_TOTAL
                y2 = pointerPositionInner * height
            }
        } else {
            pointerLine.apply {
                x1 = pointerPositionInner * width
                y1 = height.toFloat() / HORIZONTAL_TOTAL
                x2 = pointerPositionInner * width
                y2 = HORIZONTAL_END * height / HORIZONTAL_TOTAL
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
        backgroundRect = if (orientation == VERTICAL_ORIENTATION) Rect(
            (width.toFloat() / HORIZONTAL_TOTAL).toInt(),
            (height.toFloat() / VERTICAL_TOTAL).toInt(),
            (HORIZONTAL_END * width.toFloat() / HORIZONTAL_TOTAL).toInt(),
            (VERTICAL_BACKGROUND * height.toFloat() / VERTICAL_TOTAL).toInt()
        )
        else Rect(
            (width.toFloat() / VERTICAL_TOTAL).toInt(),
            (height.toFloat() / HORIZONTAL_TOTAL).toInt(),
            (VERTICAL_BACKGROUND * width.toFloat() / VERTICAL_TOTAL).toInt(),
            (HORIZONTAL_END * height.toFloat() / HORIZONTAL_TOTAL).toInt()
        )

        frontierRect = if (orientation == VERTICAL_ORIENTATION) Rect(
            (width.toFloat() / HORIZONTAL_TOTAL).toInt(),
            (VERTICAL_INNER_LIMIT_TOP * height.toFloat() / VERTICAL_TOTAL).toInt(),
            (HORIZONTAL_END * width.toFloat() / HORIZONTAL_TOTAL).toInt(),
            (VERTICAL_INNER_LIMIT_BOTTOM * height.toFloat() / VERTICAL_TOTAL).toInt()
        ) else Rect(
            (HORIZONTAL_INNER_LIMIT_LEFT * width.toFloat() / VERTICAL_TOTAL).toInt(),
            (HORIZONTAL_INNER_LIMIT_TOP * height.toFloat() / HORIZONTAL_TOTAL).toInt(),
            (HORIZONTAL_INNER_LIMIT_RIGHT * width.toFloat() / VERTICAL_TOTAL).toInt(),
            (HORIZONTAL_INNER_LIMIT_BOTTOM * height.toFloat() / HORIZONTAL_TOTAL).toInt()
        )
    }

    private fun calculateLines() {
        centralLine = if (orientation == VERTICAL_ORIENTATION) Line(
            width.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_CENTRAL * height.toFloat() / VERTICAL_TOTAL,
            HORIZONTAL_END * width.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_CENTRAL * height.toFloat() / VERTICAL_TOTAL
        ) else Line (
            VERTICAL_CENTRAL * width.toFloat() / VERTICAL_TOTAL,
            height.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_CENTRAL * width.toFloat() / VERTICAL_TOTAL,
            HORIZONTAL_END * height.toFloat() / HORIZONTAL_TOTAL
        )

        if (marksLines.isNotEmpty()) marksLines.clear()
        if (orientation == VERTICAL_ORIENTATION) calculateVerticalMarkLines()
        else calculateHorizontalMarkLines()
    }

    private fun calculateLargeMarkOffset(i: Int) = MARK_BASE_PROPORTION * i + MARK_LARGE_OFFSET
    private fun calculateOddSmallMarkOffset(i: Int) = MARK_BASE_PROPORTION * i + MARK_SMALL_OFFSET
    private fun calculateEvenSmallMarkOffset(i: Int) = MARK_BASE_PROPORTION * i - MARK_SMALL_OFFSET
    private fun calculateVerticalMarkLines() {
        for (i in 0 until NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    LEFT_LARGE_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateLargeMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL,
                    LEFT_LARGE_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateLargeMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL
                )
            )
        for (i in 0 until NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    RIGHT_LARGE_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateLargeMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL,
                    RIGHT_LARGE_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateLargeMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL
                )
            )
        for (i in 1..NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    LEFT_SMALL_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateOddSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL,
                    LEFT_SMALL_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateOddSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL
                )
            )
        for (i in 1..NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    LEFT_SMALL_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateEvenSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL,
                    LEFT_SMALL_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateEvenSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL
                )
            )
        for (i in 1..NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    RIGHT_SMALL_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateOddSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL,
                    RIGHT_SMALL_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateOddSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL
                )
            )
        for (i in 1..NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    RIGHT_SMALL_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateEvenSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL,
                    RIGHT_SMALL_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL,
                    calculateEvenSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL
                )
            )

        contourLines[0] = Line(
            width.toFloat() / HORIZONTAL_TOTAL,
            height.toFloat() / VERTICAL_TOTAL,
            HORIZONTAL_END * width.toFloat() / HORIZONTAL_TOTAL,
            height.toFloat() / VERTICAL_TOTAL
        )
        contourLines[1] = Line(
            HORIZONTAL_END * width.toFloat() / HORIZONTAL_TOTAL,
            height.toFloat() / VERTICAL_TOTAL,
            HORIZONTAL_END * width.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_BACKGROUND * height.toFloat() / VERTICAL_TOTAL
        )
        contourLines[2] = Line(
            HORIZONTAL_END * width.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_BACKGROUND * height.toFloat() / VERTICAL_TOTAL,
            width.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_BACKGROUND * height.toFloat() / VERTICAL_TOTAL
        )
        contourLines[3] = Line(
            width.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_BACKGROUND * height.toFloat() / VERTICAL_TOTAL,
            width.toFloat() / HORIZONTAL_TOTAL,
            height.toFloat() / VERTICAL_TOTAL
        )
    }

    private fun calculateHorizontalMarkLines() {
        for (i in 0 until NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateLargeMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    LEFT_LARGE_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL,
                    calculateLargeMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    LEFT_LARGE_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL
                )
            )
        }
        for (i in 0 until NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateLargeMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    RIGHT_LARGE_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL,
                    calculateLargeMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    RIGHT_LARGE_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL
                )
            )
        }
        for (i in 1..NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateEvenSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    LEFT_SMALL_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL,
                    calculateEvenSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    LEFT_SMALL_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL
                )
            )
        }

        for (i in 1..NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateEvenSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    RIGHT_SMALL_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL,
                    calculateEvenSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    RIGHT_SMALL_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL
                )
            )
        }

        for (i in 1..NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateOddSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    LEFT_SMALL_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL,
                    calculateOddSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    LEFT_SMALL_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL
                )
            )
        }

        for (i in 1..NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateOddSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    RIGHT_SMALL_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL,
                    calculateOddSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL,
                    RIGHT_SMALL_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL
                )
            )
        }

        contourLines[0] = Line(
            width.toFloat() / VERTICAL_TOTAL,
            height.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_BACKGROUND * width.toFloat() / VERTICAL_TOTAL,
            height.toFloat() / HORIZONTAL_TOTAL
        )
        contourLines[1] = Line(
            VERTICAL_BACKGROUND * width.toFloat() / VERTICAL_TOTAL,
            height.toFloat() / HORIZONTAL_TOTAL,
            VERTICAL_BACKGROUND * width.toFloat() / VERTICAL_TOTAL,
            HORIZONTAL_END * height.toFloat() / HORIZONTAL_TOTAL
        )
        contourLines[2] = Line(
            VERTICAL_BACKGROUND * width.toFloat() / VERTICAL_TOTAL,
            HORIZONTAL_END * height.toFloat() / HORIZONTAL_TOTAL,
            width.toFloat() / VERTICAL_TOTAL,
            HORIZONTAL_END * height.toFloat() / HORIZONTAL_TOTAL
        )
        contourLines[3] = Line(
            width.toFloat() / VERTICAL_TOTAL,
            HORIZONTAL_END * height.toFloat() / HORIZONTAL_TOTAL,
            width.toFloat() / VERTICAL_TOTAL,
            height.toFloat() / HORIZONTAL_TOTAL
        )
    }

    private fun calculateFont(){
        if (orientation == VERTICAL_ORIENTATION) {
            fontPaint.textSize = height / VERTICAL_FONT_PROPORTION
            for (i in labels.indices)
                textPositions[i] = calculateVerticalFontPos(i)
        } else {
            fontPaint.textSize = height / HORIZONTAL_FONT_PROPORTION
            for (i in labels.indices)
                textPositions[i] = calculateHorizontalFontPos(i)
        }
    }

    private fun calculateVerticalFontPos(i: Int): Pair<Float, Float> =
        Pair(VERTICAL_FONT_X_POSITION * width / HORIZONTAL_TOTAL,
            (VERTICAL_FONT_Y_POSITION * i) * height / VERTICAL_TOTAL)

    private fun calculateHorizontalFontPos(i: Int): Pair<Float, Float> =
        Pair((HORIZONTAL_FONT_X_POSITION * i) * width / VERTICAL_TOTAL,
            HORIZONTAL_FONT_Y_POSITION * height / HORIZONTAL_TOTAL)

    private fun recalculateLineWidth() {
        if (orientation == VERTICAL_ORIENTATION) {
            primaryLineWidth = height / VERTICAL_PRIMARY_LINE_PROPORTION
            secondaryLineWidth = height / VERTICAL_SECONDARY_LINE_PROPORTION
        } else {
            primaryLineWidth = height / HORIZONTAL_PRIMARY_LINE_PROPORTION
            secondaryLineWidth = height / HORIZONTAL_SECONDARY_LINE_PROPORTION
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
