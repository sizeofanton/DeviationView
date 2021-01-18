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

    data class Line(
        var x1: Float,
        var y1: Float,
        var x2: Float,
        var y2: Float
    )

    companion object {
        const val POSITION_UPPER_MAX = 50
        const val POSITION_LOWER_MAX = -50
        const val POSITION_CENTER = 0

        private const val VERTICAL_ORIENTATION = 0
        private const val HORIZONTAL_ORIENTATION = 1

        private const val DEFAULT_PRIMARY_LINE_WIDTH = 20.0f
        private const val DEFAULT_SECONDARY_LINE_WIDTH = 10.0f

        private const val DEFAULT_TEXT_SIZE = 50.0f
        private const val POSITION_PROPORTION = 50.0f

        // TODO better naming
        private const val VERTICAL_TOTAL_PARTS = 22.0f // TODO
        private const val VERTICAL_CENTRAL_PART = VERTICAL_TOTAL_PARTS / 2 // TODO
        private const val VERTICAL_AVAILABLE_PARTS = VERTICAL_CENTRAL_PART - 1 // TODO
        private const val VERTICAL_BACKGROUND_LIMIT = VERTICAL_TOTAL_PARTS - 1 // TODO
        private const val VERTICAL_FRONTIER_LIMIT_TOP = VERTICAL_AVAILABLE_PARTS - 1
        private const val VERTICAL_FRONTIER_LIMIT_BOTTOM = VERTICAL_CENTRAL_PART + 2

        private const val HORIZONTAL_TOTAL_PARTS = 11.0f // TODO
        private const val HORIZONTAL_END_PART = 9.0f // TODO
        private const val HORIZONTAL_FRONTIER_LIMIT_LEFT = 9.0f
        private const val HORIZONTAL_FRONTIER_LIMIT_RIGHT = 13.0f
        private const val HORIZONTAL_FRONTIER_LIMIT_TOP = 1.0f
        private const val HORIZONTAL_FRONTIER_LIMIT_BOTTOM = 9.0f

        private const val NUMBER_OF_MARKS = 10
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
    private val contourLines = Array<Line>(4){Line(0f, 0f, 0f, 0f)}
    private var textPositions = Array(11){Pair(0f, 0f) }
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
        if (pos !in -POSITION_LOWER_MAX..POSITION_UPPER_MAX)
            throw IllegalArgumentException("Pointer position must be in " +
                    "$POSITION_LOWER_MAX..$POSITION_UPPER_MAX range")
        val pointerPositionInner = if (orientation == VERTICAL_ORIENTATION) {
            when (pos) {
                POSITION_CENTER -> VERTICAL_CENTRAL_PART / VERTICAL_TOTAL_PARTS
                else -> (VERTICAL_CENTRAL_PART / VERTICAL_TOTAL_PARTS) -
                        (VERTICAL_AVAILABLE_PARTS / VERTICAL_TOTAL_PARTS * pos / POSITION_PROPORTION)
            }
        } else {
            when (pos) {
                POSITION_CENTER -> VERTICAL_CENTRAL_PART / VERTICAL_TOTAL_PARTS
                else -> (VERTICAL_CENTRAL_PART / VERTICAL_TOTAL_PARTS) +
                        (VERTICAL_AVAILABLE_PARTS / VERTICAL_TOTAL_PARTS * pos / POSITION_PROPORTION)
            }
        }

        if (orientation == VERTICAL_ORIENTATION) {
            pointerLine.apply {
                x1 = width.toFloat() / VERTICAL_TOTAL_PARTS
                y1 = pointerPositionInner * height
                x2 = HORIZONTAL_END_PART * width.toFloat() / HORIZONTAL_TOTAL_PARTS
                y2 = pointerPositionInner * height
            }
        } else {
            pointerLine.apply {
                x1 = pointerPositionInner * width
                y1 = height.toFloat() / HORIZONTAL_TOTAL_PARTS
                x2 = pointerPositionInner * width
                y2 = HORIZONTAL_END_PART * height / HORIZONTAL_TOTAL_PARTS
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
            (width.toFloat() / HORIZONTAL_TOTAL_PARTS).toInt(),
            (height.toFloat() / VERTICAL_TOTAL_PARTS).toInt(),
            (HORIZONTAL_END_PART * width.toFloat() / HORIZONTAL_TOTAL_PARTS).toInt(),
            (VERTICAL_BACKGROUND_LIMIT * height.toFloat() / VERTICAL_TOTAL_PARTS).toInt()
        )
        else Rect(
            (width.toFloat() / VERTICAL_TOTAL_PARTS).toInt(),
            (height.toFloat() / HORIZONTAL_TOTAL_PARTS).toInt(),
            (VERTICAL_BACKGROUND_LIMIT * width.toFloat() / VERTICAL_TOTAL_PARTS).toInt(),
            (HORIZONTAL_END_PART * height.toFloat() / HORIZONTAL_TOTAL_PARTS).toInt()
        )

        frontierRect = if (orientation == VERTICAL_ORIENTATION) Rect(
            (width.toFloat() / HORIZONTAL_TOTAL_PARTS).toInt(),
            (VERTICAL_FRONTIER_LIMIT_TOP * height.toFloat() / VERTICAL_TOTAL_PARTS).toInt(),
            (HORIZONTAL_END_PART * width.toFloat() / HORIZONTAL_TOTAL_PARTS).toInt(),
            (VERTICAL_FRONTIER_LIMIT_BOTTOM * height.toFloat() / VERTICAL_TOTAL_PARTS).toInt()
        ) else Rect(
            (HORIZONTAL_FRONTIER_LIMIT_LEFT * width.toFloat() / VERTICAL_TOTAL_PARTS).toInt(),
            (HORIZONTAL_FRONTIER_LIMIT_TOP * height.toFloat() / HORIZONTAL_TOTAL_PARTS).toInt(),
            (HORIZONTAL_FRONTIER_LIMIT_RIGHT * width.toFloat() / VERTICAL_TOTAL_PARTS).toInt(),
            (HORIZONTAL_FRONTIER_LIMIT_BOTTOM * height.toFloat() / HORIZONTAL_TOTAL_PARTS).toInt()
        )
    }

    private fun calculateLines() {
        centralLine = if (orientation == VERTICAL_ORIENTATION) Line(
            width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_CENTRAL_PART * height.toFloat() / VERTICAL_TOTAL_PARTS,
            HORIZONTAL_END_PART * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_CENTRAL_PART * height.toFloat() / VERTICAL_TOTAL_PARTS
        ) else Line (
            VERTICAL_CENTRAL_PART * width.toFloat() / VERTICAL_TOTAL_PARTS,
            height.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_CENTRAL_PART * width.toFloat() / VERTICAL_TOTAL_PARTS,
            HORIZONTAL_END_PART * height.toFloat() / HORIZONTAL_TOTAL_PARTS
        )

        if (marksLines.isNotEmpty()) marksLines.clear()
        if (orientation == VERTICAL_ORIENTATION) calculateVerticalMarkLines()
        else calculateHorizontalMarkLines()
    }

    private fun calculateLargeMarkOffset(i: Int) = 2 + 2 * i
    private fun calculateOddSmallMarkOffset(i: Int) = 2f * i + 0.5f
    private fun calculateEvenSmallMarkOffset(i: Int) = 2f * i - 0.5f
    private fun calculateVerticalMarkLines() {
        for (i in 0 until NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    LEFT_LARGE_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateLargeMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_LARGE_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateLargeMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS
                )
            )
        for (i in 0 until NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    RIGHT_LARGE_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateLargeMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_LARGE_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateLargeMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS
                )
            )
        for (i in 1..NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    LEFT_SMALL_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateOddSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_SMALL_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateOddSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS
                )
            )
        for (i in 1..NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    LEFT_SMALL_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateEvenSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_SMALL_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateEvenSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS
                )
            )
        for (i in 1..NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    RIGHT_SMALL_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateOddSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_SMALL_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateOddSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS
                )
            )
        for (i in 1..NUMBER_OF_MARKS)
            marksLines.add(
                Line(
                    RIGHT_SMALL_MARK_X1 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateEvenSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_SMALL_MARK_X2 * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateEvenSmallMarkOffset(i) * height.toFloat() / VERTICAL_TOTAL_PARTS
                )
            )

        contourLines[0] = Line(
            width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            height.toFloat() / VERTICAL_TOTAL_PARTS,
            HORIZONTAL_END_PART * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            height.toFloat() / VERTICAL_TOTAL_PARTS
        )
        contourLines[1] = Line(
            HORIZONTAL_END_PART * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            height.toFloat() / VERTICAL_TOTAL_PARTS,
            HORIZONTAL_END_PART * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_BACKGROUND_LIMIT * height.toFloat() / VERTICAL_TOTAL_PARTS
        )
        contourLines[2] = Line(
            HORIZONTAL_END_PART * width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_BACKGROUND_LIMIT * height.toFloat() / VERTICAL_TOTAL_PARTS,
            width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_BACKGROUND_LIMIT * height.toFloat() / VERTICAL_TOTAL_PARTS
        )
        contourLines[3] = Line(
            width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_BACKGROUND_LIMIT * height.toFloat() / VERTICAL_TOTAL_PARTS,
            width.toFloat() / HORIZONTAL_TOTAL_PARTS,
            height.toFloat() / VERTICAL_TOTAL_PARTS
        )
    }

    private fun calculateHorizontalMarkLines() {
        for (i in 0 until NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateLargeMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_LARGE_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateLargeMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_LARGE_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL_PARTS
                )
            )
        }
        for (i in 0 until NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateLargeMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_LARGE_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateLargeMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_LARGE_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL_PARTS
                )
            )
        }
        for (i in 1..NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateEvenSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_SMALL_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateEvenSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_SMALL_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL_PARTS
                )
            )
        }

        for (i in 1..NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateEvenSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_SMALL_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateEvenSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_SMALL_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL_PARTS
                )
            )
        }

        for (i in 1..NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateOddSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_SMALL_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateOddSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    LEFT_SMALL_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL_PARTS
                )
            )
        }

        for (i in 1..NUMBER_OF_MARKS) {
            marksLines.add(
                Line(
                    calculateOddSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_SMALL_MARK_X1 * height.toFloat() / HORIZONTAL_TOTAL_PARTS,
                    calculateOddSmallMarkOffset(i) * width.toFloat() / VERTICAL_TOTAL_PARTS,
                    RIGHT_SMALL_MARK_X2 * height.toFloat() / HORIZONTAL_TOTAL_PARTS
                )
            )
        }

        contourLines[0] = Line(
            width.toFloat() / VERTICAL_TOTAL_PARTS,
            height.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_BACKGROUND_LIMIT * width.toFloat() / VERTICAL_TOTAL_PARTS,
            height.toFloat() / HORIZONTAL_TOTAL_PARTS
        )
        contourLines[1] = Line(
            VERTICAL_BACKGROUND_LIMIT * width.toFloat() / VERTICAL_TOTAL_PARTS,
            height.toFloat() / HORIZONTAL_TOTAL_PARTS,
            VERTICAL_BACKGROUND_LIMIT * width.toFloat() / VERTICAL_TOTAL_PARTS,
            HORIZONTAL_END_PART * height.toFloat() / HORIZONTAL_TOTAL_PARTS
        )
        contourLines[2] = Line(
            VERTICAL_BACKGROUND_LIMIT * width.toFloat() / VERTICAL_TOTAL_PARTS,
            HORIZONTAL_END_PART * height.toFloat() / HORIZONTAL_TOTAL_PARTS,
            width.toFloat() / VERTICAL_TOTAL_PARTS,
            HORIZONTAL_END_PART * height.toFloat() / HORIZONTAL_TOTAL_PARTS
        )
        contourLines[3] = Line(
            width.toFloat() / VERTICAL_TOTAL_PARTS,
            HORIZONTAL_END_PART * height.toFloat() / HORIZONTAL_TOTAL_PARTS,
            width.toFloat() / VERTICAL_TOTAL_PARTS,
            height.toFloat() / HORIZONTAL_TOTAL_PARTS
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

    // TODO Magic numbers
    private fun calculateVerticalFontPos(i: Int): Pair<Float, Float> =
        Pair(9.5f * width / 11, (1f + 2f * i) * height / 22)

    // TODO Magic numbers
    private fun calculateHorizontalFontPos(i: Int): Pair<Float, Float> =
        Pair((0.75f + 2f * i) * width / 22, 10f * height / 11)

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
