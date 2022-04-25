package com.mucheng.mutermux

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.mucheng.mutermux.util.getLineHeight
import com.mucheng.mutermux.util.getSp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class TermuxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var backgroundColor = Color.BLACK

    private val inputConnection = TermuxInputConnection(this, true)

    private val userColor = Color.parseColor("#497ce3")

    private val userText = "@user ~ "

    private val userPaint = Paint().apply {
        isAntiAlias = true
        textSize = getSp(context, 16)
        color = userColor
    }

    private val pathColor = Color.parseColor("#ad91e9")

    @SuppressLint("SdCardPath")
    private val pathText = "/sdcard $ "

    private val pathPaint = Paint().apply {
        isAntiAlias = true
        textSize = getSp(context, 16)
        color = pathColor
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = getSp(context, 16)
        color = Color.WHITE
    }

    private val lineContent = arrayListOf(
        LineContent("echo 'emm'").also { it.setHasBefore(true) }
    )

    private val eventHandler = EventHandler(this)

    private val gestureDetector = createGestureDetector()

    private val cursor = Cursor()

    private val lock = Mutex()

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        cursor.row += lineContent[cursor.index].getBufferSize()
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    //创建手势检测器
    private fun createGestureDetector(): GestureDetector {
        return GestureDetector(context, eventHandler).apply {
            setOnDoubleTapListener(eventHandler)
        }
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(backgroundColor)
        val lineCount = lineContent.size

        (0 until lineCount).asSequence().map { lineContent[it] }
            .forEachIndexed { index, lineContent ->
                val line = index + 1

                var width = 0f
                if (lineContent.hasBefore()) {
                    canvas.drawText(userText, 0f, line * getLineHeight(userPaint), userPaint)
                    width += userPaint.measureText(userText)
                    canvas.drawText(
                        pathText,
                        width,
                        line * getLineHeight(pathPaint),
                        pathPaint
                    )
                    width += pathPaint.measureText(pathText)
                }

                // 绘制代码
                canvas.drawText(
                    lineContent.getBuffer().toString(),
                    width,
                    line * getLineHeight(textPaint),
                    textPaint
                )
            }

    }

    override fun onCheckIsTextEditor(): Boolean {
        return true
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo?): InputConnection {
        outAttrs?.inputType = EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
        return inputConnection
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_ENTER -> commitText("\n")

                KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_FORWARD_DEL -> {
                    val index = cursor.index
                    val row = cursor.row

                    if (row > 0) {
                        val lineContent = lineContent[index]
                        val buffer = lineContent.getBuffer()
                        cursor.row--
                        buffer.deleteCharAt(cursor.row)
                        invalidate()
                        return super.onKeyDown(keyCode, event)
                    }

                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    fun setTextColor(simpleColor: Int) {
        textPaint.color = simpleColor
    }

    fun setTypeface(typeface: Typeface) {
        userPaint.typeface = typeface
        pathPaint.typeface = typeface
        textPaint.typeface = typeface
    }

    fun showSoftInput() {
        // 显示软键盘
        requestFocus()
        val inputMethodManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    private var environmentPath = ""

    fun setEnvironmentPath(path: String) {
        environmentPath = path
    }

    fun commitText(text: String) {
        val index = cursor.index
        val row = cursor.row
        val lineContent = lineContent[index]

        if (text != "\n") {
            val buffer = lineContent.getBuffer()

            if (row < lineContent.getBufferSize()) {
                buffer.insert(row, text)
                cursor.row += text.length
                invalidate()
                return
            }

            cursor.row += text.length
            buffer.append(text)
            invalidate()
            return
        }

        // 按回车了应执行代码
        CoroutineScope(Dispatchers.IO).launch {
            lock.lock()
            val result = ShellExecutor.execute(environmentPath, lineContent.getBuffer().toString())
            if (result.isSuccess) {
                Log.e("Text", "${result.getOrNull()}")
            } else {
                Log.e("Error", "${result.exceptionOrNull()}")
            }
            lock.unlock()
        }
    }

}