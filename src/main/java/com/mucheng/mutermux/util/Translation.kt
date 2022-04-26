package com.mucheng.mutermux.util

class Translation {

    private var x = 0f

    private var y = 0f

    private var maxPositionX = 0f

    private var maxPositionY = 0f

    fun reset() {
        x = 0f
        y = 0f
    }

    fun resetX() {
        x = 0f
    }

    fun resetY() {
        y = 0f
    }

    fun setMaxPositionX(x: Int) {
        maxPositionX = x.toFloat()
    }

    fun setMaxPositionY(y: Int) {
        maxPositionY = y.toFloat()
    }

    fun translateX(offset: Float) {
        //小判断，防止坐标越界
        if (x + offset < 0) {
            x = 0f
            return
        }

        if (x + offset > maxPositionX) {
            if (offset < 0) {
                y += offset
            }
            return
        }

        x += offset
    }

    fun getCurrentX() = x.toInt()

    fun translateY(offset: Float) {
        //小判断，防止坐标越界
        if (y + offset < 0) {
            y = 0f
            return
        }
        if (y + offset > maxPositionY) {
            if (offset < 0) {
                y += offset
            }
            return
        }
        y += offset
        return
    }

    fun getCurrentY() = y.toInt()

}