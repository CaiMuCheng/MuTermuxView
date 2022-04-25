package com.mucheng.mutermux

import android.view.GestureDetector
import android.view.MotionEvent

class EventHandler(private val termuxView: TermuxView) : GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {

    override fun onDown(p0: MotionEvent?): Boolean {
        return true
    }

    override fun onShowPress(p0: MotionEvent?) {

    }

    override fun onSingleTapUp(p0: MotionEvent?): Boolean {
        return false
    }

    override fun onScroll(p0: MotionEvent?, p1: MotionEvent?, p2: Float, p3: Float): Boolean {
        return false
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