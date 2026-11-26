package com.narezany.shizukutoolbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.narezany.shizukutoolbox.core.ShizukuManager
import com.narezany.shizukutoolbox.core.Shell
import com.narezany.shizukutoolbox.core.Ui
import com.narezany.shizukutoolbox.data.SystemRepo
import com.narezany.shizukutoolbox.data.SystemSnapshot
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class QuickAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val command: String,
    val confirm: String? = null
)

private data class ActionGroup(val title: String, val actions: List<QuickAction>)

private fun actionGroups(pkg: String): List<ActionGroup> = listOf(
    ActionGroup(
        title = "Питание и оболочка",
        actions = listOf(
            QuickAction(
                "Перезагрузка", "обычная", Icons.Filled.RestartAlt, "reboot",
                "Телефон сейчас перезагрузится."
            ),
            QuickAction(
                "Recovery", "меню восстановления", Icons.Filled.SettingsBackupRestore,
                "reboot recovery", "Телефон перезагрузится в recovery."
            ),
            QuickAction(
                "Выключение", "полное выключение", Icons.Filled.PowerSettingsNew,
                "reboot -p", "Телефон выключится."
            ),
            QuickAction(
                "Перезапуск SystemUI", "если пропала шторка", Icons.Filled.Refresh,
                "pkill -f com.android.systemui"
            ),
            QuickAction(
                "Скриншот", "в папку Pictures", Icons.Filled.PhotoCamera,
                "screencap -p /sdcard/Pictures/ST_\$(date +%Y%m%d_%H%M%S).png && ls -t /sdcard/Pictures | head -n 1"
            ),
            QuickAction(
                "Запись экрана 20 сек", "в папку Movies", Icons.Filled.Videocam,
                "screenrecord --time-limit 20 /sdcard/Movies/REC_\$(date +%Y%m%d_%H%M%S).mp4"
            )
        )
    ),
    ActionGroup(
        title = "Оптимизация",
        actions = listOf(
            QuickAction(
                "Остановить фоновые", "am kill-all", Icons.Filled.LayersClear,
                "am kill-all"
            ),
            QuickAction(
                "Очистить кэш приложений", "pm trim-caches", Icons.Filled.CleaningServices,
                "pm trim-caches 128G"
            ),
            QuickAction(
                "Усыпить систему", "Doze прямо сейчас", Icons.Filled.BatteryStd,
                "dumpsys deviceidle force-idle"
            ),
            QuickAction(
                "Разбудить из Doze", "снять принудительный idle", Icons.Filled.Bolt,
                "dumpsys deviceidle unforce"
            ),
            QuickAction(
                "Отключить анимации", "интерфейс станет быстрее", Icons.Filled.Speed,
                "settings put global window_animation_scale 0.0; " +
                    "settings put global transition_animation_scale 0.0; " +
                    "settings put global animator_duration_scale 0.0"
            ),
            QuickAction(
                "Вернуть анимации", "масштаб 1.0", Icons.Filled.Speed,
                "settings put global window_animation_scale 1.0; " +
                    "settings put global transition_animation_scale 1.0; " +
                    "settings put global animator_duration_scale 1.0"
            ),
            QuickAction(
                "Сбросить статистику батареи", "dumpsys batterystats", Icons.Filled.BatteryAlert,
                "dumpsys batterystats --reset"
            )
        )
    ),
    ActionGroup(
        title = "Экран и система",
        actions = listOf(
            QuickAction(
                "Сбросить разрешение и DPI", "wm size reset + wm density reset",
                Icons.Filled.Smartphone, "wm size reset; wm density reset"
            ),
            QuickAction(
                "Настройки разработчика", "скрытые пункты Android", Icons.Filled.DeveloperMode,
                "am start -a android.settings.APPLICATION_DEVELOPMENT_SETTINGS"
            ),
            QuickAction(
                "Выдать себе WRITE_SECURE_SETTINGS",
                "приложению станут доступны системные настройки",
                Icons.Filled.VerifiedUser,
                "pm grant $pkg android.permission.WRITE_SECURE_SETTINGS"
            )
        )
    )
)

@Composable
fun HomeScreen() {
    val scope = rememberCoroutineScope()
    val granted by ShizukuManager.granted.collectAsState()

    var confirm by remember { mutableStateOf<QuickAction?>(null) }
    var snapshot by remember { mutableStateOf(SystemSnapshot.Empty) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    LaunchedEffect(refreshKey, granted) {
        snapshot = if (granted) SystemRepo.load() else SystemSnapshot.Empty
    }

    fun exec(action: QuickAction) {
        if (!granted) {
            Ui.toast("Сначала дай разрешение Shizuku")
            return
        }
        scope.launch {
            busy = true
            val result = Shell.exec(action.command)
            busy = false
            if (result.ok) {
                val line = result.short()
                Ui.toast(line.ifBlank { "Готово" })
            } else {
                Ui.toast("Ошибка: ${result.errorText()}")
            }
            refreshKey++
        }
    }

    fun toggle(key: String, value: Boolean) {
        if (!granted) {
            Ui.toast("Сначала дай разрешение Shizuku")
            return
        }
        val command = when (key) {
            "wifi" -> if (value) "svc wifi enable" else "svc wifi disable"
            "data" -> if (value) "svc data enable" else "svc data disable"
            "bt" -> if (value) "svc bluetooth enable" else "svc bluetooth disable"
            "air" -> if (value) "cmd connectivity airplane-mode enable" else "cmd connectivity airplane-mode disable"
            else -> return
        }
        scope.launch {
            val result = Shell.exec(command)
            Ui.toast(if (result.ok) "Готово" else "Ошибка: ${result.errorText()}")
            delay(600)
            refreshKey++
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { StatusCard(granted) }
        item { TogglesCard(snapshot) { key, value -> toggle(key, value) } }
        if (busy) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        actionGroups(ShizukuManager.appContext.packageName).forEach { group ->
            item { SectionTitle(group.title) }
            item {
                ToolCard {
                    group.actions.forEach { action ->
                        ActionRow(
                            icon = action.icon,
                            title = action.title,
                            subtitle = action.subtitle,
                            danger = action.confirm != null,
                            onClick = {
                                if (action.confirm != null) confirm = action else exec(action)
                            }
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    val pending = confirm
    if (pending != null) {
        ConfirmDialog(
            title = pending.title,
            text = pending.confirm.orEmpty(),
            confirmLabel = "Выполнить",
            onConfirm = { exec(pending) },
            onDismiss = { confirm = null }
        )
    }
}

@Composable
private fun StatusCard(granted: Boolean) {
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (granted) "Доступ через Shizuku активен" else "Нет доступа к shell",
                style = MaterialTheme.typography.titleSmall
            )
        }
        Text(
            text = "Команды выполняются от пользователя shell (uid 2000): как в adb, только с телефона.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TogglesCard(snapshot: SystemSnapshot, onToggle: (String, Boolean) -> Unit) {
    ToolCard {
        Text(
            text = "Переключатели",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ToggleRow(Icons.Filled.Wifi, "Wi-Fi", snapshot.wifi, snapshot.loaded) {
            onToggle("wifi", it)
        }
        ToggleRow(Icons.Filled.DataUsage, "Мобильные данные", snapshot.mobileData, snapshot.loaded) {
            onToggle("data", it)
        }
        ToggleRow(Icons.Filled.Bluetooth, "Bluetooth", snapshot.bluetooth, snapshot.loaded) {
            onToggle("bt", it)
        }
        ToggleRow(Icons.Filled.AirplanemodeActive, "Авиарежим", snapshot.airplane, snapshot.loaded) {
            onToggle("air", it)
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            enabled = enabled
        )
    }
}
