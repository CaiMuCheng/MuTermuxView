package com.mucheng.mutermux.interfaces

import com.mucheng.mutermux.ShellExecutor

interface OnShellEventListener {
    fun onShellEvent(command: String, params: Array<String>): ShellExecutor.ShellEvent
}