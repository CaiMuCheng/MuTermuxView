package com.mucheng.mutermux

import android.view.inputmethod.BaseInputConnection

class TermuxInputConnection(private val editor: TermuxView, fullEditor: Boolean) :
    BaseInputConnection(editor, fullEditor) {

    override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
        editor.commitText(text!! as String)
        return super.commitText(text, newCursorPosition)
    }

}