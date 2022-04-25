package com.mucheng.mutermux

import android.content.Context
import android.util.Log
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileWriter
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object ShellExecutor {

    suspend fun execute(environmentPath: String, command: String) =
        suspendCoroutine<Result<String>> {
            thread {
                val result = runCatching {
                    // 删除前后空格
                    // 清除中间的空格
                    // 执行

                    val file = File("$environmentPath/cache.sh")
                    val bw = BufferedWriter(FileWriter(file))
                    bw.write(command)
                    bw.flush()
                    bw.close()

                    val permissionProcess = Runtime.getRuntime().exec("chmod 777 $file")
                    val outPutStream = permissionProcess.outputStream.bufferedWriter()

                    outPutStream.write(file.absolutePath)
                    outPutStream.flush()

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
                        "运行 $command 异常, 错误码: $code"
                    }
                }

                it.resume(result)
            }
        }


}