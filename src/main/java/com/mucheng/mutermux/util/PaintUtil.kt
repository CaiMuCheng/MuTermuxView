package com.mucheng.mutermux.util

import android.graphics.Paint

// 计算文本的高度
fun getLineHeight(paint: Paint): Float {
    val fontMetrics = paint.fontMetrics
    return fontMetrics.descent - fontMetrics.ascent
}