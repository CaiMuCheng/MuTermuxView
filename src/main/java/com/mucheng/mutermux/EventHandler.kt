package com.mucheng.mutermux

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import com.mucheng.mutermux.util.Translation

class EventHandler(private val termuxView: TermuxView) : GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {

    private val translation by lazy {
        Translation().apply {
            termuxView.receiveOnDraw {
                setMaxPositionY(termuxView.getMaxHeight())
            }
        }
    }

    fun getScrollTranslation() = translation

    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {

    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        translation.translateY(p3)
        termuxView.scrollTo(translation.getCurrentX(), translation.getCurrentY())
        return true
    }

    override fun onLongPress(p0: MotionEvent?) {

    }

    override fun onFling(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(p0: MotionEvent?): Boolean {
        termuxView.showSoftInput()
        return false
    }

    override fun onDoubleTap(p0: MotionEvent?): Boolean {
        return false
    }

    override fun onDoubleTapEvent(p0: MotionEvent?): Boolean {
        return false
    }
}