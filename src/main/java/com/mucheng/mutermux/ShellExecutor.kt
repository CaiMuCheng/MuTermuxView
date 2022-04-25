package com.mucheng.mutermux

import android.content.Context
import android.util.Log
import com.mucheng.mutermux.interfaces.OnShellEventListener
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ShellExecutor {

    private var onShellEventListener: OnShellEventListener? = null

    fun setOnShellEventListener(onShellEventListener: OnShellEventListener) {
        this.onShellEventListener = onShellEventListener
    }

    suspend fun execute(environmentPath: String, command: String) =
        suspendCoroutine<Result<String>> {
            thread {
                val result = runCatching {
                    if (command.isEmpty()) {
                        return@runCatching ""
                    }
                    // 删除前后空格
                    // 清除中间的空格
                    // 执行
                    val commands =
                        command.trim().replace("\\s+".toRegex(), "#").split("#").toTypedArray()
                    val func = commands[0]
                    val parameters = if (command.length > 1) {
                        val list = commands.toMutableList()
                        list.removeAt(0)
                        list.toTypedArray()
                    } else {
                        emptyArray()
                    }

                    val permissionProcess =
                        Runtime.getRuntime().exec(commands, arrayOf(), File(environmentPath))

                    val code = permissionProcess.waitFor()

                    val input = permissionProcess.inputStream
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    val buffer = ByteArray(1024)
                    var flag: Int
                    input.buffered().use {
                        while (input.read(buffer).also { flag = it } != -1) {
                            byteArrayOutputStream.write(buffer, 0, flag)
                            byteArrayOutputStream.flush()
                        }
                    }

                    if (code == 0) {
                        String(byteArrayOutputStream.toByteArray())
                    } else {
                        val result = onShellEventListener?.onShellEvent(func, parameters) ?: false
                        if (result) {
                            ""
                        } else {
                            "运行 $command 异常, 错误码: $code"
                        }
                    }
                }

                it.resume(result)
            }
        }


}