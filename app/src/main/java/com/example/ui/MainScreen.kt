package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RefreshRateValues
import com.example.shizuku.ShizukuState
import com.example.ui.theme.ButtonPrimaryBg
import com.example.ui.theme.ButtonPrimaryText
import com.example.ui.theme.ButtonSecondaryBg
import com.example.ui.theme.ButtonSecondaryBorder
import com.example.ui.theme.ButtonSecondaryText
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusInactiveBg
import com.example.ui.theme.StatusInactiveText
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showShellInput by remember { mutableStateOf(false) }
    var customCommandText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBg,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Minimal Header
            item {
                MinimalHeader(
                    shizukuState = uiState.shizukuState,
                    displayFps = uiState.displayFps,
                    isExecuting = uiState.isExecuting,
                    onHelpClick = { showHelpDialog = true },
                    onToggleShell = { showShellInput = !showShellInput }
                )
            }

            // 2. Shizuku Auth Banner
            if (!uiState.shizukuState.isPermissionGranted) {
                item {
                    ShizukuAuthBanner(
                        shizukuState = uiState.shizukuState,
                        onRequestPermission = { viewModel.requestShizukuPermission() },
                        onOpenShizuku = { viewModel.openShizukuApp() }
                    )
                }
            }

            // 3. Live System Keys Status
            item {
                LiveSettingsCard(
                    currentValues = uiState.currentValues,
                    isPolling = uiState.isPolling,
                    is144Active = uiState.is144HzActive,
                    onTogglePolling = { viewModel.togglePolling(it) },
                    onRefresh = { viewModel.refreshSystemValues() }
                )
            }

            // 4. Primary Action Controls (Vivo/iQOO 144FPS Unlocker commands)
            item {
                ActionControls(
                    isReady = uiState.shizukuState.isPermissionGranted,
                    isExecuting = uiState.isExecuting,
                    is144Active = uiState.is144HzActive,
                    onUnlock144Hz = { viewModel.unlock144Hz() },
                    onRestoreStandard = { viewModel.restoreStandard() },
                    onFactoryReset = { viewModel.factoryReset() }
                )
            }

            // 5. Shell Command Runner (Collapsible)
            if (showShellInput) {
                item {
                    MinimalShellSection(
                        commandText = customCommandText,
                        onCommandChange = { customCommandText = it },
                        isReady = uiState.shizukuState.isPermissionGranted,
                        onRunCommand = {
                            viewModel.runCustomShellCommand(customCommandText)
                            customCommandText = ""
                        }
                    )
                }
            }
        }
    }

    if (showHelpDialog) {
        MinimalHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun MinimalHeader(
    shizukuState: ShizukuState,
    displayFps: Float,
    isExecuting: Boolean,
    onHelpClick: () -> Unit,
    onToggleShell: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "iQOO 144 FPS Unlocker",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )

                    if (isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = TextSecondary
                        )
                    }
                }

                Text(
                    text = "Force 144Hz override (peak/user/min = 1)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Display FPS badge
                val fpsInt = displayFps.toInt()
                if (fpsInt > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = "$fpsInt Hz",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleShell,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Shell",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Справка",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ShizukuAuthBanner(
    shizukuState: ShizukuState,
    onRequestPermission: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shizuku_status_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Доступ Shizuku (ADB API)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = TextPrimary
                )
            }

            Text(
                text = shizukuState.statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimaryBg,
                        contentColor = ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("request_permission_button")
                ) {
                    Text(
                        text = "Запросить доступ",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                OutlinedButton(
                    onClick = onOpenShizuku,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("open_shizuku_button")
                ) {
                    Text(
                        text = "Открыть Shizuku",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

@Composable
fun LiveSettingsCard(
    currentValues: RefreshRateValues,
    isPolling: Boolean,
    is144Active: Boolean,
    onTogglePolling: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_settings_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ПАРАМЕТРЫ СИСТЕМЫ (SETTINGS SYSTEM)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("refresh_keys_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Switch(
                        checked = isPolling,
                        onCheckedChange = onTogglePolling,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ButtonPrimaryText,
                            checkedTrackColor = ButtonPrimaryBg,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceElevated,
                            uncheckedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.testTag("auto_polling_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            MinimalSettingItem(
                keyName = "peak_refresh_rate",
                value = currentValues.peak
            )

            HorizontalDivider(
                color = DarkBorderSubtle,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            MinimalSettingItem(
                keyName = "user_refresh_rate",
                value = currentValues.user
            )

            HorizontalDivider(
                color = DarkBorderSubtle,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            MinimalSettingItem(
                keyName = "min_refresh_rate",
                value = currentValues.min
            )
        }
    }
}

@Composable
fun MinimalSettingItem(
    keyName: String,
    value: String
) {
    val displayValue = if (value.isBlank()) "—" else value
    val isOne = value == "1"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = keyName,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            if (isOne) {
                Text(
                    text = "Принудительно 144 Гц",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = TextPrimary
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isOne) DarkSurfaceVariant else DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isOne) DarkBorder else DarkBorderSubtle
            )
        ) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isOne) TextPrimary else TextSecondary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ActionControls(
    isReady: Boolean,
    isExecuting: Boolean,
    is144Active: Boolean,
    onUnlock144Hz: () -> Unit,
    onRestoreStandard: () -> Unit,
    onFactoryReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Primary Action: Force 144Hz
        Button(
            onClick = onUnlock144Hz,
            enabled = isReady && !isExecuting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("unlock_144hz_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ButtonPrimaryBg,
                contentColor = ButtonPrimaryText,
                disabledContainerColor = DarkSurfaceElevated,
                disabledContentColor = TextMuted
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (is144Active) "144 Гц Активен (Все ключи = 1)" else "Активировать 144 Гц (Force 144Hz)",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // 2. Secondary Action: Reset to Standard
        OutlinedButton(
            onClick = onRestoreStandard,
            enabled = isReady && !isExecuting,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("restore_standard_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = ButtonSecondaryBg,
                contentColor = ButtonSecondaryText,
                disabledContainerColor = DarkSurface,
                disabledContentColor = TextMuted
            ),
            border = androidx.compose.foundation.BorderStroke(1.dp, ButtonSecondaryBorder)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Сбросить на стандарт",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // 3. Delete Keys Action
        TextButton(
            onClick = onFactoryReset,
            enabled = isReady && !isExecuting,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .testTag("factory_reset_button"),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Очистить ключи (settings delete system)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Normal),
                color = TextMuted
            )
        }
    }
}

@Composable
fun MinimalShellSection(
    commandText: String,
    onCommandChange: (String) -> Unit,
    isReady: Boolean,
    onRunCommand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "ADB Shell",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = TextMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commandText,
                    onValueChange = onCommandChange,
                    placeholder = {
                        Text(
                            "settings get system peak_refresh_rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated,
                        focusedBorderColor = TextSecondary,
                        unfocusedBorderColor = DarkBorder
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onRunCommand,
                    enabled = isReady && commandText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ButtonPrimaryBg,
                        contentColor = ButtonPrimaryText
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MinimalHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text(
                text = "О разблокировке 144 Гц на iQOO / vivo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "• На смартфонах vivo / iQOO значение 1 в системных настройках 'peak_refresh_rate', 'user_refresh_rate' и 'min_refresh_rate' переопределяет частоту экрана на максимальную (144 Гц) во всех поддерживаемых приложениях и играх (метод VivoIQOO144FPSUnlocker).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "• Команды выполняются безопасно через Shizuku (ADB API) без необходимости Root-прав.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "• Кнопка 'Сбросить на стандарт' возвращает стандартные настройки или очищает ключи для возврата к заводскому Smart Switch vivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Понятно", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
