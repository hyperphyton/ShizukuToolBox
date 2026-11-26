package com.narezany.shizukutoolbox.data

import com.narezany.shizukutoolbox.core.Shell

data class SystemSnapshot(
    val loaded: Boolean,
    val wifi: Boolean,
    val mobileData: Boolean,
    val bluetooth: Boolean,
    val airplane: Boolean
) {
    companion object {
        val Empty = SystemSnapshot(false, false, false, false, false)
    }
}

object SystemRepo {

    suspend fun load(): SystemSnapshot {
        val r = Shell.exec(
            "settings get global wifi_on; " +
                "settings get global mobile_data; " +
                "settings get global bluetooth_on; " +
                "settings get global airplane_mode_on"
        )
        if (!r.ok) return SystemSnapshot.Empty
        val lines = r.output.lines().map { it.trim() }
        fun flag(index: Int) = lines.getOrNull(index) == "1"
        return SystemSnapshot(
            loaded = true,
            wifi = flag(0),
            mobileData = flag(1),
            bluetooth = flag(2),
            airplane = flag(3)
        )
    }
}
