package com.qq7te.totalrecall.ui.detail

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * Custom ImageView that supports pinch-to-zoom and pan gestures.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val initialMatrix = Matrix()

    // Touch event modes
    private enum class Mode {
        NONE, DRAG
    }

    private var mode = Mode.NONE
    private val start = PointF()
    private var minScale = 1f
    private var maxScale = 5f
    private var currentScale = 1f

    private val scaleGestureDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    // Callback for single tap events
    var onSingleTapListener: (() -> Unit)? = null

    init {
        // Start with FIT_CENTER so image is fully visible
        scaleType = ScaleType.FIT_CENTER

        scaleGestureDetector = ScaleGestureDetector(context, ScaleListener())
        gestureDetector = GestureDetector(context, GestureListener())
    }

    override fun setImageURI(uri: android.net.Uri?) {
        super.setImageURI(uri)
        // Switch to MATRIX mode after image is loaded for zoom support
        post {
            if (drawable != null) {
                initializeMatrix()
            }
        }
    }

    override fun setImageDrawable(drawable: android.graphics.drawable.Drawable?) {
        super.setImageDrawable(drawable)
        // Switch to MATRIX mode after image is loaded for zoom support
        post {
            if (drawable != null) {
                initializeMatrix()
            }
        }
    }

    private fun initializeMatrix() {
        // Get the fit-center matrix that was computed by the ImageView
        val fitMatrix = imageMatrix
        if (fitMatrix != null) {
            matrix.set(fitMatrix)
            savedMatrix.set(fitMatrix)
            initialMatrix.set(fitMatrix) // Store for reset

            // Derive minScale from the fit-center matrix so we never zoom out smaller than fit
            val values = FloatArray(9)
            fitMatrix.getValues(values)
            minScale = values[Matrix.MSCALE_X]
            maxScale = minScale * 5f
            currentScale = minScale
        }
        // Now switch to MATRIX mode for zoom control
        scaleType = ScaleType.MATRIX
        imageMatrix = matrix
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleGestureDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                mode = Mode.DRAG
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                savedMatrix.set(matrix)
                mode = Mode.NONE
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = Mode.NONE
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == Mode.DRAG && !scaleGestureDetector.isInProgress) {
                    matrix.set(savedMatrix)
                    val dx = event.x - start.x
                    val dy = event.y - start.y
                    matrix.postTranslate(dx, dy)
                    clampTranslation()
                }
            }
        }

        // Apply the matrix transformation
        imageMatrix = matrix
        return true
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            val newScale = (currentScale * detector.scaleFactor).coerceIn(minScale, maxScale)
            val actualFactor = newScale / currentScale
            currentScale = newScale
            matrix.postScale(actualFactor, actualFactor, detector.focusX, detector.focusY)
            clampTranslation()
            imageMatrix = matrix
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            onSingleTapListener?.invoke()
            return true
        }
    }

    /**
     * Clamps the matrix translation so the image never shows empty space inside the view.
     * When the image fits within the view (at min scale), it stays centered.
     */
    private fun clampTranslation() {
        val d = drawable ?: return
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()

        val values = FloatArray(9)
        matrix.getValues(values)
        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]
        var transX = values[Matrix.MTRANS_X]
        var transY = values[Matrix.MTRANS_Y]

        val imageWidth = d.intrinsicWidth * scaleX
        val imageHeight = d.intrinsicHeight * scaleY

        transX = if (imageWidth <= viewWidth) {
            (viewWidth - imageWidth) / 2f
        } else {
            transX.coerceIn(viewWidth - imageWidth, 0f)
        }

        transY = if (imageHeight <= viewHeight) {
            (viewHeight - imageHeight) / 2f
        } else {
            transY.coerceIn(viewHeight - imageHeight, 0f)
        }

        values[Matrix.MTRANS_X] = transX
        values[Matrix.MTRANS_Y] = transY
        matrix.setValues(values)
    }

    /**
     * Reset the zoom to the original fit-to-screen scale
     */
    fun resetZoom() {
        matrix.set(initialMatrix)
        currentScale = minScale
        imageMatrix = matrix
        invalidate()
    }
}
