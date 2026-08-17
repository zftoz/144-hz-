package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class RefreshRateValues(
    val peak: String = "",
    val user: String = "",
    val min: String = ""
)

data class BackupState(
    val exists: Boolean = false,
    val values: RefreshRateValues = RefreshRateValues(),
    val savedAt: String = "",
    val sourceDescription: String = "Заводские значения vivo"
)

data class CommandLog(
    val timestamp: String,
    val command: String,
    val output: String,
    val isSuccess: Boolean
)

class SettingsRepository(
    private val context: Context,
    private val shizukuManager: ShizukuManager
) {
    companion object {
        const val KEY_PEAK = "peak_refresh_rate"
        const val KEY_USER = "user_refresh_rate"
        const val KEY_MIN = "min_refresh_rate"

        // iQOO / vivo unlock value is strictly 1
        const val UNLOCK_VALUE = "1"

        private const val PREFS_NAME = "iqoo_144hz_prefs"
        private const val PREF_BACKUP_EXISTS = "backup_exists"
        private const val PREF_SAVED_PEAK = "saved_peak"
        private const val PREF_SAVED_USER = "saved_user"
        private const val PREF_SAVED_MIN = "saved_min"
        private const val PREF_SAVED_TIME = "saved_time"
        private const val PREF_SOURCE_DESC = "saved_source_desc"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    /**
     * Читает значение ключа из settings system
     */
    suspend fun getSystemSetting(key: String): Pair<String, CommandLog> = withContext(Dispatchers.IO) {
        val cmd = "settings get system $key"
        val output = shizukuManager.runShell(cmd)
        val cleanOutput = output.trim()
        val log = CommandLog(
            timestamp = timeFormat.format(Date()),
            command = cmd,
            output = cleanOutput,
            isSuccess = !cleanOutput.startsWith("Error")
        )
        Pair(cleanOutput, log)
    }

    /**
     * Читает все 3 ключевых параметра
     */
    suspend fun readAllCurrentValues(): Triple<RefreshRateValues, List<CommandLog>, Boolean> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<CommandLog>()
        val (peak, logPeak) = getSystemSetting(KEY_PEAK)
        val (user, logUser) = getSystemSetting(KEY_USER)
        val (min, logMin) = getSystemSetting(KEY_MIN)
        logs.add(logPeak)
        logs.add(logUser)
        logs.add(logMin)

        val hasError = logs.any { !it.isSuccess }
        Triple(
            RefreshRateValues(peak = peak, user = user, min = min),
            logs,
            !hasError
        )
    }

    /**
     * Сохраняет исходные параметры в постоянное хранилище.
     * Защита: если переданные значения уже равны "1" (уже разблокировано),
     * мы не затираем реальный бэкап.
     */
    fun saveOriginalBaseline(values: RefreshRateValues, force: Boolean = false): BackupState {
        val currentBackup = getBackupState()
        if (currentBackup.exists && !force) {
            return currentBackup
        }

        // Если текущие значения уже 1 (уже модифицированы), и бэкапа нет,
        // сохраняем как стандартные заводские null (Smart Switch)
        val isAlreadyModified = values.peak == "1" && values.user == "1" && values.min == "1"
        val cleanValues = if (isAlreadyModified && !force) {
            RefreshRateValues(peak = "null", user = "null", min = "null")
        } else {
            values
        }

        val now = dateTimeFormat.format(Date())
        val desc = if (isAlreadyModified) "Заводские значения (Smart Switch)" else "Исходные параметры устройства"

        prefs.edit()
            .putBoolean(PREF_BACKUP_EXISTS, true)
            .putString(PREF_SAVED_PEAK, cleanValues.peak)
            .putString(PREF_SAVED_USER, cleanValues.user)
            .putString(PREF_SAVED_MIN, cleanValues.min)
            .putString(PREF_SAVED_TIME, now)
            .putString(PREF_SOURCE_DESC, desc)
            .apply()

        return BackupState(
            exists = true,
            values = cleanValues,
            savedAt = now,
            sourceDescription = desc
        )
    }

    /**
     * Получает сохраненную резервную копию исходных параметров
     */
    fun getBackupState(): BackupState {
        val exists = prefs.getBoolean(PREF_BACKUP_EXISTS, false)
        if (!exists) {
            return BackupState(exists = false)
        }
        val peak = prefs.getString(PREF_SAVED_PEAK, "null") ?: "null"
        val user = prefs.getString(PREF_SAVED_USER, "null") ?: "null"
        val min = prefs.getString(PREF_SAVED_MIN, "null") ?: "null"
        val time = prefs.getString(PREF_SAVED_TIME, "") ?: ""
        val desc = prefs.getString(PREF_SOURCE_DESC, "Исходные параметры устройства") ?: "Исходные параметры"

        return BackupState(
            exists = true,
            values = RefreshRateValues(peak = peak, user = user, min = min),
            savedAt = time,
            sourceDescription = desc
        )
    }

    fun clearBackupState() {
        prefs.edit().clear().apply()
    }

    /**
     * Включает 144 Гц:
     * 1. Гарантированно сохраняет исходные параметры (если они еще не сохранены).
     * 2. Выполняет установку значения 1 для всех трех ключей:
     *    settings put system peak_refresh_rate 1
     *    settings put system user_refresh_rate 1
     *    settings put system min_refresh_rate 1
     */
    suspend fun enable144Hz(): Pair<BackupState, List<CommandLog>> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<CommandLog>()

        // 1. Прочитать текущие настройки
        val (currentValues, readLogs, _) = readAllCurrentValues()
        logs.addAll(readLogs)

        // 2. Сохранить исходные значения как бэкап (не затирает, если уже есть)
        val finalBackup = saveOriginalBaseline(currentValues, force = false)

        // 3. Выполнить команды активации 144 Гц со значением "1"
        val commands = listOf(
            "settings put system $KEY_PEAK $UNLOCK_VALUE",
            "settings put system $KEY_USER $UNLOCK_VALUE",
            "settings put system $KEY_MIN $UNLOCK_VALUE"
        )

        for (cmd in commands) {
            val output = shizukuManager.runShell(cmd)
            logs.add(
                CommandLog(
                    timestamp = timeFormat.format(Date()),
                    command = cmd,
                    output = if (output.isEmpty() || output == "null") "OK (Value = 1)" else output,
                    isSuccess = !output.startsWith("Error")
                )
            )
        }

        Pair(finalBackup, logs)
    }

    /**
     * Восстанавливает стандартные / исходные настройки:
     * - Если сохраненные значения есть -> записать их обратно (или удалить, если были "null" / пустыми)
     * - Если сохраненных значений нет -> выполнить settings delete system [ключ]
     */
    suspend fun restoreStandard(): List<CommandLog> = withContext(Dispatchers.IO) {
        val logs = mutableListOf<CommandLog>()
        val backup = getBackupState()

        if (backup.exists) {
            val map = mapOf(
                KEY_PEAK to backup.values.peak,
                KEY_USER to backup.values.user,
                KEY_MIN to backup.values.min
            )

            for ((key, savedVal) in map) {
                val cmd = if (savedVal == "null" || savedVal.isBlank() || savedVal.startsWith("Error")) {
                    "settings delete system $key"
                } else {
                    "settings put system $key $savedVal"
                }

                val output = shizukuManager.runShell(cmd)
                logs.add(
                    CommandLog(
                        timestamp = timeFormat.format(Date()),
                        command = cmd,
                        output = if (output.isEmpty() || output == "null") "Restored -> $savedVal" else output,
                        isSuccess = !output.startsWith("Error")
                    )
                )
            }
        } else {
            val deleteLogs = deleteKeysInternal()
            logs.addAll(deleteLogs)
        }

        logs
    }

    /**
     * Сброс к заводским настройкам vivo (удаление ключей из settings system)
     */
    suspend fun factoryReset(): List<CommandLog> = withContext(Dispatchers.IO) {
        deleteKeysInternal()
    }

    private fun deleteKeysInternal(): List<CommandLog> {
        val logs = mutableListOf<CommandLog>()
        val commands = listOf(
            "settings delete system $KEY_PEAK",
            "settings delete system $KEY_USER",
            "settings delete system $KEY_MIN"
        )

        for (cmd in commands) {
            val output = shizukuManager.runShell(cmd)
            logs.add(
                CommandLog(
                    timestamp = timeFormat.format(Date()),
                    command = cmd,
                    output = if (output.isEmpty() || output == "null") "OK (Deleted)" else output,
                    isSuccess = !output.startsWith("Error")
                )
            )
        }
        return logs
    }
}
