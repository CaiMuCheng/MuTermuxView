package com.mucheng.mutermux

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.*
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import com.mucheng.mutermux.interfaces.OnShellEventListener
import com.mucheng.mutermux.util.getSp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

class TermuxView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), OnShellEventListener {

    private var backgroundColor = Color.BLACK

    private val inputConnection = TermuxInputConnection(this, true)

    private val userText = SpannableStringBuilder("@user ~ ").apply {
        setSpan(ForegroundColorSpan(Color.parseColor("#497ce3")), 0, length, 0)
    }

    @SuppressLint("SdCardPath")
    private var pathText = SpannableStringBuilder("/sdcard $ ").apply {
        setSpan(ForegroundColorSpan(Color.parseColor("#ad91e9")), 0, length, 0)
    }

    private var content = StringBuilder()

    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = getSp(context, 16)
        color = Color.WHITE
    }

    private val lineTexts = arrayListOf("")

    private val eventHandler = EventHandler(this)

    private val gestureDetector = createGestureDetector()

    private val cursor = Cursor()

    private val lock = Mutex()

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        cursor.row += content.length
        // 注册 Shell
        ShellExecutor.setOnShellEventListener(this)
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

    private val buffer = SpannableStringBuilder()

    @Suppress("DEPRECATION")
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(backgroundColor)
        buffer.clear()
        buffer.clearSpans()

        val text =
            buffer.append(StringBuilder(lineTexts.joinToString(separator = "\n")).also {
                if (it.isNotEmpty()) it.append(
                    "\n"
                )
            }).append(userText).append(pathText).append(content)
        val staticLayout = StaticLayout(
            text, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true
        )
        staticLayout.draw(canvas)
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
                        cursor.row--
                        content.deleteCharAt(cursor.row)
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
        updatePathText(path)
        environmentPath = path
    }

    fun commitText(text: String) {
        val row = cursor.row

        if (text != "\n") {

            if (row < content.length) {
                content.insert(row, text)
                cursor.row += text.length
                invalidate()
                return
            }

            cursor.row += text.length
            content.append(text)
            invalidate()
            return
        }

        // 按回车了应执行代码
        CoroutineScope(Dispatchers.IO).launch {
            lock.lock()
            val cmd = content.toString()
            lineTexts.add(cmd)
            Log.e("cont", content.toString())
            val result = ShellExecutor.execute(environmentPath, cmd)
            if (result.isSuccess) {
                lineTexts.add("Success: ${result.getOrNull()}")
            } else {
                lineTexts.add("Error: ${result.exceptionOrNull()?.message}")
            }
            postInvalidate()
            content.clear()
            lock.unlock()
        }

    }

    override fun onShellEvent(command: String, params: Array<String>): Boolean {
        when (command) {

            "cd" -> {
                if (params.isEmpty() || params.size > 1) {
                    return false
                }

                // 获取参数
                val path = params[0]
                Log.e("Text", "$command $path")
                updatePathText(path)
                return true
            }

        }

        return false
    }

    private fun updatePathText(path: String) {
        pathText = SpannableStringBuilder("$path $ ").apply {
            setSpan(ForegroundColorSpan(Color.parseColor("#ad91e9")), 0, length, 0)
        }

        invalidate()
    }

}