package com.narezany.shizukutoolbox.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.narezany.shizukutoolbox.core.Shell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val MAX_BUFFER = 120_000

private val presets = listOf(
    "pm list packages -3",
    "dumpsys battery",
    "ip addr show wlan0",
    "df -h /data",
    "top -n 1 -b | head -n 15",
    "wm size",
    "settings get global device_name",
    "getprop ro.build.version.release",
    "logcat -d -t 80"
)

class TerminalController(private val scope: CoroutineScope) {

    private val buffer = StringBuilder()

    private val _output = MutableStateFlow("")
    val output: StateFlow<String> = _output.asStateFlow()

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _history = MutableStateFlow<List<String>>(emptyList())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    fun run(command: String) {
        val cmd = command.trim()
        if (cmd.isEmpty() || _running.value) return

        _running.value = true
        buffer.append("$ ").append(cmd).append('\n')
        trimBuffer()
        _output.value = buffer.toString()
        _history.value = (listOf(cmd) + _history.value.filter { it != cmd }).take(20)

        scope.launch {
            try {
                var streamed = false
                val result = Shell.execStream(cmd) { line ->
                    streamed = true
                    buffer.append(line).append('\n')
                    trimBuffer()
                    _output.value = buffer.toString()
                }
                if (!streamed && result.output.isNotBlank()) {
                    buffer.append(result.output.trimEnd()).append('\n')
                }
                if (result.code != 0) {
                    buffer.append("[код ").append(result.code).append("]").append('\n')
                }
                _output.value = buffer.toString()
            } catch (t: Throwable) {
                buffer.append("Ошибка: ").append(t.message ?: t.toString()).append('\n')
                _output.value = buffer.toString()
            } finally {
                _running.value = false
            }
        }
    }

    fun clear() {
        if (_running.value) return
        buffer.setLength(0)
        _output.value = ""
    }

    private fun trimBuffer() {
        if (buffer.length > MAX_BUFFER) {
            buffer.delete(0, buffer.length - MAX_BUFFER)
        }
    }
}

@Composable
fun TerminalScreen() {
    val scope = rememberCoroutineScope()
    val controller = remember { TerminalController(scope) }
    val output by controller.output.collectAsState()
    val running by controller.running.collectAsState()
    val history by controller.history.collectAsState()

    var input by remember { mutableStateOf("") }
    val scroll = rememberScrollState()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(output) {
        scroll.scrollTo(scroll.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (history.isEmpty()) {
                presets.take(5).forEach { preset ->
                    AssistChip(
                        onClick = { input = preset },
                        label = { Text(preset.take(22)) }
                    )
                }
            } else {
                history.take(5).forEach { item ->
                    AssistChip(
                        onClick = { input = item },
                        label = { Text(item.take(22)) }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(12.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = output.ifBlank { "Вывод команд появится здесь.\nКоманды идут от shell (uid 2000)." },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("shell-команда") },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    controller.clear()
                    input = ""
                }
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Очистить")
            }
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = {
                    val cmd = input
                    input = ""
                    keyboard?.hide()
                    controller.run(cmd)
                },
                enabled = !running
            ) {
                if (running) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Filled.Send, contentDescription = "Выполнить")
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
