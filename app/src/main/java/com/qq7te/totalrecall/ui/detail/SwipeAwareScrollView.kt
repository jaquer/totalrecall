package com.qq7te.totalrecall.ui.detail

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ScrollView
import kotlin.math.abs

class SwipeAwareScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ScrollView(context, attrs) {

    var onSwipeLeft: (() -> Unit)? = null
    var onSwipeRight: (() -> Unit)? = null

    private val swipeThreshold = (context.resources.displayMetrics.density * 80).toInt()
    private val swipeVelocityThreshold = 200

    private var swipeConsumed = false

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            e1 ?: return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            if (abs(diffX) > abs(diffY)
                && abs(diffX) > swipeThreshold
                && abs(velocityX) > swipeVelocityThreshold
            ) {
                swipeConsumed = true
                if (diffX < 0) onSwipeLeft?.invoke() else onSwipeRight?.invoke()
                return true
            }
            return false
        }
    })

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) swipeConsumed = false
        gestureDetector.onTouchEvent(ev)
        if (swipeConsumed) {
            // Cancel children so click listeners don't fire after a swipe
            val cancel = MotionEvent.obtain(ev).also { it.action = MotionEvent.ACTION_CANCEL }
            super.dispatchTouchEvent(cancel)
            cancel.recycle()
            return true
        }
        return super.dispatchTouchEvent(ev)
    }
}
