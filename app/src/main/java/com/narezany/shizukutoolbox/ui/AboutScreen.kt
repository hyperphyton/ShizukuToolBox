package com.narezany.shizukutoolbox.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.narezany.shizukutoolbox.core.ShizukuManager
import com.narezany.shizukutoolbox.core.Shell
import kotlinx.coroutines.launch

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val binder by ShizukuManager.binderAlive.collectAsState()
    val granted by ShizukuManager.granted.collectAsState()
    val version by ShizukuManager.version.collectAsState()

    var info by remember { mutableStateOf<Pair<String, String>?>(null) }

    fun diag(title: String, command: String) {
        scope.launch {
            val result = Shell.exec(command)
            info = title to result.output.ifBlank { result.errorText() }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            InfoCard {
                Text("Статус Shizuku", style = MaterialTheme.typography.titleSmall)
                Bullet(if (binder) "Служба запущена, версия $version" else "Служба не запущена")
                Bullet(if (granted) "Разрешение выдано" else "Разрешение не выдано")
                Bullet("Команды выполняются от shell (uid 2000)")
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        if (binder && !granted) ShizukuManager.requestPermission()
                        else ShizukuManager.openShizuku(context)
                    }) {
                        Text(if (!binder) "Открыть Shizuku" else if (!granted) "Разрешить" else "Настройки Shizuku")
                    }
                    OutlinedButton(onClick = { ShizukuManager.refresh() }) {
                        Text("Обновить")
                    }
                }
            }
        }

        item { SectionTitle("Диагностика") }
        item {
            ToolCard {
                ActionRow(
                    icon = Icons.Filled.BatteryFull,
                    title = "Батарея",
                    subtitle = "dumpsys battery",
                    onClick = { diag("Батарея", "dumpsys battery | head -n 30") }
                )
                ActionRow(
                    icon = Icons.Filled.Memory,
                    title = "Память",
                    subtitle = "cat /proc/meminfo",
                    onClick = { diag("Память", "cat /proc/meminfo | head -n 8") }
                )
                ActionRow(
                    icon = Icons.Filled.SdCard,
                    title = "Накопитель",
                    subtitle = "df -h /data /storage/emulated/0",
                    onClick = { diag("Накопитель", "df -h /data /storage/emulated/0") }
                )
                ActionRow(
                    icon = Icons.Filled.Wifi,
                    title = "Сеть",
                    subtitle = "ip -brief addr",
                    onClick = { diag("Сеть", "ip -brief addr") }
                )
                ActionRow(
                    icon = Icons.Filled.Speed,
                    title = "Нагрузка",
                    subtitle = "top -n 1 -b",
                    onClick = { diag("Нагрузка", "top -n 1 -b | head -n 14") }
                )
                ActionRow(
                    icon = Icons.Filled.Android,
                    title = "Устройство",
                    subtitle = "модель, версия Android",
                    onClick = {
                        diag(
                            "Устройство",
                            "getprop ro.product.manufacturer; getprop ro.product.model; " +
                                "getprop ro.build.version.release; getprop ro.build.version.sdk; " +
                                "getprop ro.build.display.id"
                        )
                    }
                )
            }
        }

        item { SectionTitle("Как запустить Shizuku") }
        item {
            InfoCard {
                Bullet("Установи Shizuku из Google Play или с github.com/RikkaApps/Shizuku.")
                Bullet("Включи «Беспроводную отладку» в настройках разработчика.")
                Bullet("Открой Shizuku → «Запустить» (через беспроводную отладку или root).")
                Bullet("Вернись сюда и нажми «Разрешить».")
                Spacer(Modifier.height(6.dp))
                OutlinedButton(onClick = { ShizukuManager.openShizuku(context) }) {
                    Text("Открыть Shizuku")
                }
            }
        }

        item { SectionTitle("Осторожно") }
        item {
            InfoCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚠️  ", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Приложение выполняет системные команды от shell",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Bullet("Удаление, очистка данных, заморозка и перезагрузка необратимы.")
                Bullet("Не замораживай системные пакеты, без которых не грузится оболочка.")
                Bullet("Всё, что делает приложение, можно повторить командами в разделе «Терминал».")
            }
        }

        item { SectionTitle("О приложении") }
        item {
            InfoCard {
                Text("Shizuku Toolbox 1.0", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Неофициальный клиент для Shizuku. Не связан с RikkaApps. " +
                        "Исходники открыты, собирай сам.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "shizuku.rikka.app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("↗", color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }

    val state = info
    if (state != null) {
        OutputDialog(
            title = state.first,
            text = state.second,
            onDismiss = { info = null }
        )
    }
}
