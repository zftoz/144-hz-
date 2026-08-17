package com.example.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.hardware.display.DisplayManager
import android.view.Display
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BackupState
import com.example.data.CommandLog
import com.example.data.RefreshRateValues
import com.example.data.SettingsRepository
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class MainUiState(
    val shizukuState: ShizukuState = ShizukuState(),
    val currentValues: RefreshRateValues = RefreshRateValues(peak = "—", user = "—", min = "—"),
    val backupState: BackupState = BackupState(),
    val displayFps: Float = 0f,
    val isExecuting: Boolean = false,
    val isPolling: Boolean = true,
    val logs: List<CommandLog> = emptyList(),
    val is144HzActive: Boolean = false
)

sealed interface UiEvent {
    data class ShowToast(val message: String) : UiEvent
    data class ShowSnackbar(val message: String) : UiEvent
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val shizukuManager = ShizukuManager(application)
    private val repository = SettingsRepository(application, shizukuManager)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var pollingJob: Job? = null

    init {
        // Listen for Shizuku state changes
        viewModelScope.launch {
            shizukuManager.state.collect { sState ->
                _uiState.update { it.copy(shizukuState = sState) }
                if (sState.isRunning && sState.isPermissionGranted) {
                    refreshSystemValues()
                }
            }
        }

        // Load initial backup state
        val backup = repository.getBackupState()
        _uiState.update { it.copy(backupState = backup) }

        // Initial check and start polling
        refreshAll()
        startPolling()
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
        shizukuManager.cleanup()
    }

    fun startPolling() {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (isActive) {
                if (_uiState.value.shizukuState.isPermissionGranted) {
                    refreshSystemValues(silent = true)
                }
                updateDisplayFps()
                delay(3000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun togglePolling(enable: Boolean) {
        _uiState.update { it.copy(isPolling = enable) }
        if (enable) startPolling() else stopPolling()
    }

    fun refreshAll() {
        val sState = shizukuManager.updateStatus()
        _uiState.update { 
            it.copy(
                shizukuState = sState,
                backupState = repository.getBackupState()
            ) 
        }
        updateDisplayFps()
        if (sState.isPermissionGranted) {
            refreshSystemValues()
        }
    }

    fun updateDisplayFps() {
        val context = getApplication<Application>()
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val display = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)
        val fps = display?.refreshRate ?: 0f
        _uiState.update { it.copy(displayFps = fps) }
    }

    fun refreshSystemValues(silent: Boolean = false) {
        viewModelScope.launch {
            if (!_uiState.value.shizukuState.isPermissionGranted) {
                return@launch
            }
            val (values, logs, _) = repository.readAllCurrentValues()
            
            // Если бэкап еще не записан, и значения не "1", автоматически фиксируем исходные
            val currentBackup = repository.getBackupState()
            val finalBackup = if (!currentBackup.exists && (values.peak != "1" || values.user != "1")) {
                repository.saveOriginalBaseline(values, force = false)
            } else {
                currentBackup
            }

            val is144 = values.peak == "1" && values.user == "1" && values.min == "1"

            _uiState.update { state ->
                val newLogs = if (!silent) {
                    (logs + state.logs).take(60)
                } else {
                    state.logs
                }
                state.copy(
                    currentValues = values,
                    logs = newLogs,
                    backupState = finalBackup,
                    is144HzActive = is144
                )
            }
        }
    }

    fun requestShizukuPermission() {
        shizukuManager.requestPermission()
    }

    fun openShizukuApp() {
        shizukuManager.openShizukuApp()
    }

    /**
     * Unlock 144 Hz Mode:
     * Sets value to 1 for all refresh rate keys (iQOO / vivo unlock method).
     * Preserves original system baseline.
     */
    fun unlock144Hz() {
        viewModelScope.launch {
            if (!_uiState.value.shizukuState.isPermissionGranted) {
                _events.emit(UiEvent.ShowSnackbar("Сначала предоставьте доступ Shizuku"))
                return@launch
            }

            _uiState.update { it.copy(isExecuting = true) }

            val (backup, logs) = repository.enable144Hz()

            // Re-read current values
            val (newValues, readLogs, _) = repository.readAllCurrentValues()
            val is144 = newValues.peak == "1" && newValues.user == "1" && newValues.min == "1"

            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    backupState = backup,
                    currentValues = newValues,
                    is144HzActive = is144,
                    logs = (logs + readLogs + state.logs).take(60)
                )
            }
            updateDisplayFps()
            _events.emit(UiEvent.ShowSnackbar("Установлено значение 1 (144 Гц разблокированы). Исходные параметры сохранены!"))
        }
    }

    /**
     * Restore System Default / Original Values
     */
    fun restoreStandard() {
        viewModelScope.launch {
            if (!_uiState.value.shizukuState.isPermissionGranted) {
                _events.emit(UiEvent.ShowSnackbar("Сначала предоставьте доступ Shizuku"))
                return@launch
            }

            _uiState.update { it.copy(isExecuting = true) }
            val hadBackup = _uiState.value.backupState.exists

            val logs = repository.restoreStandard()

            // Re-read current values
            val (newValues, readLogs, _) = repository.readAllCurrentValues()
            val is144 = newValues.peak == "1" && newValues.user == "1" && newValues.min == "1"

            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    currentValues = newValues,
                    is144HzActive = is144,
                    logs = (logs + readLogs + state.logs).take(60)
                )
            }
            updateDisplayFps()

            val msg = if (hadBackup) {
                "Исходные настройки восстановлены из бэкапа."
            } else {
                "Ключи удалены. Включен заводской Smart Switch vivo."
            }
            _events.emit(UiEvent.ShowSnackbar(msg))
        }
    }

    /**
     * Factory Reset (Delete keys to restore vivo Smart Switch)
     */
    fun factoryReset() {
        viewModelScope.launch {
            if (!_uiState.value.shizukuState.isPermissionGranted) {
                _events.emit(UiEvent.ShowSnackbar("Сначала предоставьте доступ Shizuku"))
                return@launch
            }

            _uiState.update { it.copy(isExecuting = true) }
            val logs = repository.factoryReset()

            // Re-read
            val (newValues, readLogs, _) = repository.readAllCurrentValues()

            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    currentValues = newValues,
                    is144HzActive = false,
                    logs = (logs + readLogs + state.logs).take(60)
                )
            }
            updateDisplayFps()
            _events.emit(UiEvent.ShowSnackbar("Все ключи удалены. Заводской алгоритм vivo восстановлен."))
        }
    }

    /**
     * Re-capture current values as the new baseline
     */
    fun forceCaptureCurrentAsBaseline() {
        val current = _uiState.value.currentValues
        val newBackup = repository.saveOriginalBaseline(current, force = true)
        _uiState.update { it.copy(backupState = newBackup) }
        viewModelScope.launch {
            _events.emit(UiEvent.ShowSnackbar("Текущие параметры сохранены как новая база!"))
        }
    }

    fun runCustomShellCommand(cmd: String) {
        if (cmd.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true) }
            val output = shizukuManager.runShell(cmd)
            val log = CommandLog(
                timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                command = cmd,
                output = output,
                isSuccess = !output.startsWith("Error")
            )
            _uiState.update { state ->
                state.copy(
                    isExecuting = false,
                    logs = (listOf(log) + state.logs).take(60)
                )
            }
            // If modified settings, refresh values
            if (cmd.contains("settings put") || cmd.contains("settings delete")) {
                refreshSystemValues(silent = false)
            }
        }
    }

    fun copyToClipboard(text: String, label: String = "ADB Output") {
        val clipboard = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard?.setPrimaryClip(clip)
        viewModelScope.launch {
            _events.emit(UiEvent.ShowSnackbar("Скопировано в буфер: $text"))
        }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logs = emptyList()) }
    }
}
