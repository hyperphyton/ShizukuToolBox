package com.narezany.shizukutoolbox.data

import com.narezany.shizukutoolbox.core.ShizukuManager
import com.narezany.shizukutoolbox.core.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppEntry(
    val pkg: String,
    val label: String,
    val system: Boolean,
    val disabled: Boolean
)

object AppsRepo {

    suspend fun load(includeSystem: Boolean): List<AppEntry> = withContext(Dispatchers.IO) {
        if (!ShizukuManager.isAvailable()) return@withContext emptyList()

        val pm = ShizukuManager.appContext.packageManager

        val all = parse(Shell.exec("pm list packages").output)
        val system = parse(Shell.exec("pm list packages -s").output)
        val disabled = parse(Shell.exec("pm list packages -d").output)

        all.asSequence()
            .filter { includeSystem || it !in system }
            .map { pkg ->
                val label = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                }.getOrDefault(pkg)
                AppEntry(
                    pkg = pkg,
                    label = label,
                    system = pkg in system,
                    disabled = pkg in disabled
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    suspend fun dump(pkg: String): String {
        val r = Shell.exec("dumpsys package $pkg | head -n 120")
        return r.output.ifBlank { r.errorText() }
    }

    private fun parse(text: String): Set<String> =
        text.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("package:") }
            .map { it.removePrefix("package:") }
            .filter { it.isNotBlank() }
            .toSet()
}
