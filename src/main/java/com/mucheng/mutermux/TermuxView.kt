package com.mucheng.mutermux

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
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
import com.mucheng.mutermux.util.getLineHeight
import com.mucheng.mutermux.util.getSp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.io.File

@Suppress("LeakingThis")
open class TermuxView @JvmOverloads constructor(
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

    private val lineTexts = arrayListOf("Welcome to use MuTermux!")

    private val eventHandler = EventHandler(this)

    private val gestureDetector = createGestureDetector()

    private val cursor = Cursor()

    private val lock = Mutex()

    private var receiver = {}

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        cursor.row += content.length
        // ?????? Shell
        ShellExecutor.setOnShellEventListener(this)
    }

    override fun setBackgroundColor(color: Int) {
        backgroundColor = color
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    //?????????????????????
    private fun createGestureDetector(): GestureDetector {
        return GestureDetector(context, eventHandler).apply {
            setOnDoubleTapListener(eventHandler)
        }
    }

    fun receiveOnDraw(receiver: () -> Unit) {
        this.receiver = receiver
    }

    private var onShellEventListener: OnShellEventListener? = null
    fun addOnShellEventListener(onShellEventListener: OnShellEventListener) {
        this.onShellEventListener = onShellEventListener
    }

    private val buffer = SpannableStringBuilder()

    private var lineCount = 0

    @Suppress("DEPRECATION")
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(backgroundColor)
        buffer.clear()
        buffer.clearSpans()

        val sb = StringBuilder()
        lineTexts.forEach {
            sb.append(">> $it").append("\n")
        }

        val text =
            buffer.append(sb)
                .append(userText).append(pathText).append(content)

        val staticLayout = StaticLayout(
            text, textPaint, width, Layout.Alignment.ALIGN_NORMAL, 1f, 0f, true
        )

        staticLayout.draw(canvas)
        lineCount = staticLayout.lineCount
        receiver()
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
                    val row = content.length

                    if (row > 0) {
                        content.deleteCharAt(content.length - 1)
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
        // ???????????????
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

        // ????????????
        val translation = eventHandler.getScrollTranslation()

        if (content.isEmpty()) {
            lineTexts.add("")
            translation.translateY(getLineHeight(textPaint))
            scrollTo(0, translation.getCurrentY())
            invalidate()
            return
        }

        // ???????????????????????????
        CoroutineScope(Dispatchers.IO).launch {
            lock.lock()
            val cmd = content.toString()
            lineTexts.add(cmd)
            val result = ShellExecutor.execute(environmentPath, cmd)
            if (result.isSuccess) {
                Log.e("clear", "${result.getOrNull()}")
                if (handleText(result.getOrNull() ?: "")) {
                    lineTexts.add(result.getOrNull() ?: "")
                    translation.translateY(getLineHeight(textPaint))
                    post {
                        scrollTo(0, translation.getCurrentY())
                    }
                }

            } else {
                var localMessage = ""
                val cause = result.exceptionOrNull()?.also {
                    it.printStackTrace()
                    localMessage = it.toString()
                }?.cause.toString()
                var error = cause.split(", ").getOrNull(1) ?: ""

                if (error.isEmpty()) {
                    error = localMessage
                }

                lineTexts.add("Error: $error")
                translation.translateY(getLineHeight(textPaint))
                post {
                    scrollTo(0, translation.getCurrentY())
                }
            }
            postInvalidate()
            content.clear()
            lock.unlock()
        }

    }

    private fun handleText(text: String): Boolean {
        // ???????????? clear ??????
        if (text == "\u001B[2J\u001B[H") {
            val welcomeText = lineTexts[0]
            lineTexts.clear()
            lineTexts.add(welcomeText)
            eventHandler.getScrollTranslation().resetY()
            post {
                // ??????
                scrollTo(0, 0)
                invalidate()
            }
            return false
        }

        return true
    }

    // ??????????????????
    override fun onShellEvent(command: String, params: Array<String>): ShellExecutor.ShellEvent {
        when (command) {

            "cd" -> {
                if (params.isEmpty() || params.size > 1) {
                    return ShellExecutor.ShellEvent.FALLACIOUS
                }

                // ????????????
                val path = params[0]
                updatePathText(path)
                return ShellExecutor.ShellEvent.INTERCEPTED
            }

        }

        return onShellEventListener?.onShellEvent(command, params)
            ?: ShellExecutor.ShellEvent.SUCCESSFUL
    }

    private fun updatePathText(path: String) {
        if (path.startsWith("..")) {
            environmentPath = File(environmentPath).parent ?: "/storage/emulated/0"
            pathText = SpannableStringBuilder("$environmentPath $ ").apply {
                setSpan(ForegroundColorSpan(Color.parseColor("#ad91e9")), 0, length, 0)
            }
            invalidate()
            return
        }

        if (path.startsWith("/")) {
            environmentPath = path
            pathText = SpannableStringBuilder("$environmentPath $ ").apply {
                setSpan(ForegroundColorSpan(Color.parseColor("#ad91e9")), 0, length, 0)
            }
            invalidate()
            return
        }

        val enviFile = File("$environmentPath/$path")
        if (enviFile.isDirectory) {
            environmentPath = enviFile.absolutePath
            pathText = SpannableStringBuilder("$environmentPath $ ").apply {
                setSpan(ForegroundColorSpan(Color.parseColor("#ad91e9")), 0, length, 0)
            }
            invalidate()
            return
        }

    }

    fun getMaxHeight(): Int {
        return (getLineHeight(textPaint) * lineCount - height / 2).toInt()
    }

}