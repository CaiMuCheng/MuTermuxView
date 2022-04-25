package com.mucheng.mutermux.interfaces

interface OnShellEventListener {
    fun onShellEvent(command: String, params: Array<String>): Boolean
}