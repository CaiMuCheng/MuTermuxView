package com.mucheng.mutermux.util

import android.content.Context
import android.util.TypedValue

fun getSp(context: Context, value: Number): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value.toFloat(), context.resources.displayMetrics)
}