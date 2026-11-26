package com.narezany.shizukutoolbox.core

import android.os.ParcelFileDescriptor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.server.IRemoteProcess
import moe.shizuku.server.IShizukuService
import rikka.shizuku.Shizuku

data class ShellResult(
    val code: Int,
    val output: String
) {
    val ok: Boolean get() = code == 0

    /** Первая непустая строка вывода, обрезанная под snackbar. */
    fun short(): String =
        output.trim().lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().take(180)

    fun errorText(): String =
        output.trim().lineSequence().firstOrNull { it.isNotBlank() }?.take(180) ?: "код $code"
}

/**
 * Запуск произвольных shell-команд от имени пользователя shell (uid 2000) через Shizuku.
 *
 * В Shizuku API 13 `Shizuku.newProcess` приватный, поэтому идём тем же путём, что и сама
 * библиотека: берём binder службы Shizuku и вызываем `IShizukuService.newProcess`.
 * stderr склеивается с stdout, чтобы не было взаимных блокировок на буферах.
 */
object Shell {

    private const val MAX_OUTPUT = 400_000

    suspend fun exec(command: String): ShellResult =
        withContext(Dispatchers.IO) { run(command, null) }

    suspend fun execStream(command: String, onLine: (String) -> Unit): ShellResult =
        withContext(Dispatchers.IO) { run(command, onLine) }

    private fun service(): IShizukuService? {
        val binder = Shizuku.getBinder() ?: return null
        return IShizukuService.Stub.asInterface(binder)
    }

    private fun run(command: String, onLine: ((String) -> Unit)?): ShellResult {
        val svc = service() ?: return ShellResult(-1, "Shizuku не запущен")

        var process: IRemoteProcess? = null
        return try {
            val p = svc.newProcess(arrayOf("sh", "-c", "$command 2>&1"), null, null)
            process = p

            val sb = StringBuilder()
            ParcelFileDescriptor.AutoCloseInputStream(p.inputStream)
                .bufferedReader()
                .useLines { lines ->
                    for (line in lines) {
                        if (sb.length < MAX_OUTPUT) sb.append(line).append('\n')
                        onLine?.invoke(line)
                    }
                }

            val code = p.waitFor()
            ShellResult(code, sb.toString())
        } catch (t: Throwable) {
            ShellResult(-1, t.message ?: t.toString())
        } finally {
            runCatching { process?.destroy() }
        }
    }
}
