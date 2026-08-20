package com.example.shizuku

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Коллбэк для обработки ответа пользователя при запросе разрешения Shizuku
 */
interface ShizukuPermissionCallback {
    fun onPermissionGranted()
    fun onPermissionDenied()
}

/**
 * Состояние сервиса Shizuku
 */
data class ShizukuState(
    val isRunning: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val version: Int = -1,
    val statusDescription: String = "Проверка статуса..."
)

class ShizukuManager(private val context: Context) {

    companion object {
        const val SHIZUKU_PERMISSION_REQUEST_CODE = 144
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
    }

    private val _state = MutableStateFlow(ShizukuState())
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    private var permissionCallback: ShizukuPermissionCallback? = null

    // 3. Обработка ответа: слушатель результата запроса разрешения
    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            val isGranted = (grantResult == PackageManager.PERMISSION_GRANTED)
            if (isGranted) {
                permissionCallback?.onPermissionGranted()
            } else {
                permissionCallback?.onPermissionDenied()
            }
            updateStatus()
        }
    }

    // Слушатели жизненного цикла связки Binder
    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateStatus()
        if (isPermissionGranted()) {
            permissionCallback?.onPermissionGranted()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        updateStatus()
    }

    init {
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        updateStatus()
    }

    fun cleanup() {
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    // 1. Проверка статуса: запущен ли сервис Shizuku в системе
    fun isRunning(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    // Проверка наличия выданного разрешения
    fun isPermissionGranted(): Boolean {
        if (!isRunning()) return false
        return try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    // 2. Запрос разрешений: диалог через Shizuku API
    fun requestPermission(callback: ShizukuPermissionCallback? = null): Boolean {
        this.permissionCallback = callback
        updateStatus()

        if (isRunning()) {
            return try {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    callback?.onPermissionGranted()
                    updateStatus()
                    true
                } else {
                    Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE)
                    true
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                callback?.onPermissionDenied()
                openShizukuApp()
                false
            }
        } else {
            callback?.onPermissionDenied()
            openShizukuApp()
            return false
        }
    }

    fun updateStatus(): ShizukuState {
        val running = isRunning()
        val permissionGranted = isPermissionGranted()
        val version = try {
            if (running) Shizuku.getVersion() else -1
        } catch (e: Throwable) {
            -1
        }

        val description = when {
            !running -> "Служба Shizuku не запущена (запустите через Wireless ADB / ПК)"
            !permissionGranted -> "Служба Shizuku запущена, требуется предоставить доступ"
            else -> "Shizuku ADB активен (v$version)"
        }

        val newState = ShizukuState(
            isRunning = running,
            isPermissionGranted = permissionGranted,
            version = version,
            statusDescription = description
        )
        _state.value = newState
        return newState
    }

    // 4. Выполнение команды: отправка ADB shell команды через процесс Shizuku Binder
    fun runShell(cmd: String): String {
        if (!isRunning()) {
            return "Error: Shizuku service is not running"
        }
        if (!isPermissionGranted()) {
            return "Error: Shizuku permission not granted"
        }

        return try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", cmd), null, null)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (output.isNotEmpty()) output.append("\n")
                output.append(line)
            }

            val errorOutput = StringBuilder()
            var errLine: String?
            while (errorReader.readLine().also { errLine = it } != null) {
                if (errorOutput.isNotEmpty()) errorOutput.append("\n")
                errorOutput.append(errLine)
            }

            process.waitFor()
            val result = output.toString().trim()
            val errResult = errorOutput.toString().trim()

            if (result.isNotEmpty()) {
                result
            } else if (errResult.isNotEmpty()) {
                "Error: $errResult"
            } else {
                "null"
            }
        } catch (e: Throwable) {
            "Error: ${e.message ?: e.javaClass.simpleName}"
        }
    }

    suspend fun runShellAsync(cmd: String): String = withContext(Dispatchers.IO) {
        runShell(cmd)
    }

    fun openShizukuApp(): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE_NAME)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(marketIntent)
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}
