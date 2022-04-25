package com.mucheng.mutermux

class LineContent(text: String) {

    private val content = StringBuilder(text)

    private var hasBefore = false

    fun setHasBefore(hasBefore: Boolean) {
        this.hasBefore = hasBefore
    }

    fun hasBefore() = hasBefore

    fun getBuffer() = content

    fun getBufferSize() = content.length

}