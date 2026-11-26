package com.narezany.shizukutoolbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.narezany.shizukutoolbox.core.ShizukuManager
import com.narezany.shizukutoolbox.core.Shell
import com.narezany.shizukutoolbox.core.Ui
import com.narezany.shizukutoolbox.data.AppEntry
import com.narezany.shizukutoolbox.data.AppsRepo
import kotlinx.coroutines.launch

private data class AppCommand(
    val title: String,
    val command: String,
    val success: String,
    val icon: ImageVector,
    val confirm: String? = null
)

private fun commandsFor(app: AppEntry): List<AppCommand> = buildList {
    add(
        AppCommand(
            title = "Остановить",
            command = "am force-stop ${app.pkg}",
            success = "Приложение остановлено",
            icon = Icons.Filled.StopCircle
        )
    )
    if (app.disabled) {
        add(
            AppCommand(
                title = "Разморозить",
                command = "pm enable ${app.pkg}",
                success = "Приложение включено",
                icon = Icons.Filled.PlayArrow
            )
        )
    } else {
        add(
            AppCommand(
                title = "Заморозить",
                command = "pm disable-user --user 0 ${app.pkg}",
                success = "Приложение заморожено",
                icon = Icons.Filled.AcUnit
            )
        )
    }
    add(
        AppCommand(
            title = "Очистить данные",
            command = "pm clear ${app.pkg}",
            success = "Данные очищены",
            icon = Icons.Filled.DeleteSweep,
            confirm = "Все данные приложения (аккаунты, настройки, кэш) будут удалены."
        )
    )
    add(
        AppCommand(
            title = "Удалить для пользователя",
            command = "pm uninstall --user 0 ${app.pkg}",
            success = "Удалено для этого пользователя",
            icon = Icons.Filled.DeleteForever,
            confirm = "Приложение исчезнет из системы до сброса телефона. Вернуть можно командой cmd package install-existing."
        )
    )
    add(
        AppCommand(
            title = "Сохранить APK",
            command = "p=\$(pm path ${app.pkg} | head -n 1 | cut -d: -f2); " +
                "cp \"\$p\" /sdcard/Download/${app.pkg}.apk && echo \"/sdcard/Download/${app.pkg}.apk\"",
            success = "APK в папке Download",
            icon = Icons.Filled.SaveAlt
        )
    )
    add(
        AppCommand(
            title = "Запретить автозапуск",
            command = "appops set ${app.pkg} BOOT_COMPLETED ignore",
            success = "Автозапуск запрещён",
            icon = Icons.Filled.Block
        )
    )
    add(
        AppCommand(
            title = "Разрешить автозапуск",
            command = "appops set ${app.pkg} BOOT_COMPLETED allow",
            success = "Автозапуск разрешён",
            icon = Icons.Filled.CheckCircle
        )
    )
    add(
        AppCommand(
            title = "Ограничить в фоне",
            command = "am set-standby-bucket ${app.pkg} restricted",
            success = "Приложение ограничено",
            icon = Icons.Filled.Speed
        )
    )
    add(
        AppCommand(
            title = "Снять ограничения",
            command = "am set-standby-bucket ${app.pkg} active",
            success = "Ограничения сняты",
            icon = Icons.Filled.Restore
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen() {
    val scope = rememberCoroutineScope()
    val granted by ShizukuManager.granted.collectAsState()

    var query by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var selected by remember { mutableStateOf<AppEntry?>(null) }
    var pending by remember { mutableStateOf<AppCommand?>(null) }
    var info by remember { mutableStateOf<Pair<String, String>?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(showSystem, granted, reloadKey) {
        loading = true
        apps = if (granted) AppsRepo.load(showSystem) else emptyList()
        loading = false
    }

    fun run(command: AppCommand) {
        scope.launch {
            val result = Shell.exec(command.command)
            Ui.toast(if (result.ok) command.success else "Ошибка: ${result.errorText()}")
            reloadKey++
        }
    }

    fun openInfo(app: AppEntry) {
        scope.launch {
            val text = AppsRepo.dump(app.pkg)
            info = app.label to text
        }
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Поиск приложения") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Системные приложения · ${filtered.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = showSystem, onCheckedChange = { showSystem = it })
        }

        if (loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (!granted) {
            Text(
                text = "Список появится после выдачи разрешения Shizuku.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(20.dp)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(filtered, key = { it.pkg }) { app ->
                    AppRow(app = app, onClick = { selected = app })
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    val current = selected
    if (current != null) {
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Text(
                    text = current.label,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Text(
                    text = current.pkg + if (current.system) " · системное" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
                Spacer(Modifier.height(8.dp))
                commandsFor(current).forEach { command ->
                    ActionRow(
                        icon = command.icon,
                        title = command.title,
                        danger = command.confirm != null,
                        onClick = {
                            selected = null
                            if (command.confirm != null) pending = command else run(command)
                        }
                    )
                }
                ActionRow(
                    icon = Icons.Filled.Info,
                    title = "Сведения о пакете",
                    subtitle = "dumpsys package",
                    onClick = {
                        selected = null
                        openInfo(current)
                    }
                )
            }
        }
    }

    val pendingCommand = pending
    if (pendingCommand != null) {
        ConfirmDialog(
            title = pendingCommand.title,
            text = pendingCommand.confirm.orEmpty(),
            confirmLabel = "Выполнить",
            onConfirm = { run(pendingCommand) },
            onDismiss = { pending = null }
        )
    }

    val infoState = info
    if (infoState != null) {
        OutputDialog(
            title = infoState.first,
            text = infoState.second,
            onDismiss = { info = null }
        )
    }
}

@Composable
private fun AppRow(app: AppEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = app.label.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = app.pkg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (app.disabled) {
            Text(
                text = "заморожено",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error
            )
        } else if (app.system) {
            Text(
                text = "система",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}
