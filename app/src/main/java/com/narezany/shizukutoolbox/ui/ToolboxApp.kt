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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.narezany.shizukutoolbox.core.ShizukuManager
import com.narezany.shizukutoolbox.core.Ui

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Главная", Icons.Filled.Bolt),
    TabItem("Приложения", Icons.Filled.Apps),
    TabItem("Терминал", Icons.Filled.Code),
    TabItem("Ещё", Icons.Filled.Settings)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolboxApp() {
    val context = LocalContext.current
    val binder by ShizukuManager.binderAlive.collectAsState()
    val granted by ShizukuManager.granted.collectAsState()
    val version by ShizukuManager.version.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        Ui.messages.collect { message ->
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Shizuku Toolbox", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = when {
                                !binder -> "Shizuku не запущен"
                                !granted -> "нет разрешения"
                                else -> "shell uid 2000 · v$version"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(10.dp)
                            .background(
                                color = if (granted) Color(0xFF4ADE80) else Color(0xFFFF8A80),
                                shape = CircleShape
                            )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                tabs.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (!granted) {
                PermissionBanner(
                    binderAlive = binder,
                    onAction = {
                        if (binder) ShizukuManager.requestPermission()
                        else ShizukuManager.openShizuku(context)
                    }
                )
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when (tab) {
                    0 -> HomeScreen()
                    1 -> AppsScreen()
                    2 -> TerminalScreen()
                    else -> AboutScreen()
                }
            }
        }
    }
}

@Composable
private fun PermissionBanner(binderAlive: Boolean, onAction: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (binderAlive) "Нужно разрешение Shizuku" else "Shizuku не запущен",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = if (binderAlive) {
                    "Нажми «Разрешить» и подтверди доступ в диалоге Shizuku."
                } else {
                    "Установи Shizuku и запусти его через root или беспроводную отладку."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = onAction) {
                    Text(if (binderAlive) "Разрешить" else "Открыть Shizuku")
                }
                Text(
                    text = "Обновить",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable { ShizukuManager.refresh() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
