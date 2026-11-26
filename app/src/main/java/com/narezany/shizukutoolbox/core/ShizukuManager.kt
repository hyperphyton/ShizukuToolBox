package com.narezany.shizukutoolbox.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

/**
 * Единая точка работы с Shizuku: состояние binder'а, разрешение и запросы.
 */
object ShizukuManager {

    private const val REQUEST_CODE = 4821
    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    private val _binderAlive = MutableStateFlow(false)
    val binderAlive: StateFlow<Boolean> = _binderAlive.asStateFlow()

    private val _granted = MutableStateFlow(false)
    val granted: StateFlow<Boolean> = _granted.asStateFlow()

    private val _version = MutableStateFlow("")
    val version: StateFlow<String> = _version.asStateFlow()

    lateinit var appContext: Context
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        appContext = context.applicationContext

        Shizuku.addBinderReceivedListenerSticky {
            refresh()
        }
        Shizuku.addBinderDeadListener {
            _binderAlive.value = false
            _granted.value = false
        }
        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE) {
                val ok = grantResult == PackageManager.PERMISSION_GRANTED
                _granted.value = ok
                Ui.toast(if (ok) "Разрешение Shizuku получено" else "В разрешении отказано")
            }
        }
        refresh()
    }

    fun isAvailable(): Boolean = runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun refresh() {
        val alive = isAvailable()
        _binderAlive.value = alive
        if (!alive) {
            _granted.value = false
            _version.value = ""
            return
        }
        _version.value = runCatching { Shizuku.getVersion().toString() }.getOrDefault("?")
        _granted.value = runCatching {
            !Shizuku.isPreV11() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)
    }

    fun requestPermission() {
        if (!isAvailable()) {
            Ui.toast("Shizuku не запущен")
            return
        }
        runCatching { Shizuku.requestPermission(REQUEST_CODE) }
            .onFailure { Ui.toast("Не удалось запросить разрешение: ${it.message}") }
    }

    fun openShizuku(context: Context = appContext) {
        val launch = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(launch) }
                .onFailure { Ui.toast("Не удалось открыть Shizuku") }
        } else {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://shizuku.rikka.app/download/")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(web) }
                .onFailure { Ui.toast("Shizuku не установлен") }
        }
    }
}
