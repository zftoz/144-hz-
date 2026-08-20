package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RefreshRateValues
import com.example.shizuku.ShizukuState
import com.example.ui.theme.AccentPrimary
import com.example.ui.theme.AccentPrimaryGlow
import com.example.ui.theme.AccentSuccess
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header & Live FPS Gauge
            item {
                MinimalHeader(
                    shizukuState = uiState.shizukuState,
                    displayFps = uiState.displayFps,
                    isExecuting = uiState.isExecuting,
                    onHelpClick = { showHelpDialog = true },
                    onToggleShell = { showShellInput = !showShellInput }
                )
            }

            // 2. Shizuku Status Banner (Polished & non-obtrusive)
            item {
                ShizukuAuthBanner(
                    shizukuState = uiState.shizukuState,
                    onRequestPermission = { viewModel.requestShizukuPermission() },
                    onOpenShizuku = { viewModel.openShizukuApp() }
                )
            }

            // 3. Primary 144Hz Hero Action Hub
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

            // 4. Live System Parameters Monitor
            item {
                LiveSettingsCard(
                    currentValues = uiState.currentValues,
                    isPolling = uiState.isPolling,
                    is144Active = uiState.is144HzActive,
                    onTogglePolling = { viewModel.togglePolling(it) },
                    onRefresh = { viewModel.refreshSystemValues() }
                )
            }

            // 5. Collapsible ADB Terminal
            item {
                CollapsibleShellSection(
                    isExpanded = showShellInput,
                    onToggleExpand = { showShellInput = !showShellInput },
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "iQOO 144 FPS Unlock",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )

                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = AccentPrimary
                    )
                }
            }

            Text(
                text = "Переопределение частоты экрана через Shizuku",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val fpsInt = displayFps.toInt()
            if (fpsInt > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (fpsInt >= 120) AccentSuccess else AccentPrimary)
                        )
                        Text(
                            text = "$fpsInt Hz",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            color = TextPrimary
                        )
                    }
                }
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

@Composable
fun ShizukuAuthBanner(
    shizukuState: ShizukuState,
    onRequestPermission: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    val isGranted = shizukuState.isPermissionGranted
    val isRunning = shizukuState.isRunning

    val statusBorderColor by animateColorAsState(
        targetValue = when {
            isGranted -> DarkBorder
            isRunning -> DarkBorder
            else -> DarkBorderSubtle
        },
        animationSpec = spring(stiffness = 500f),
        label = "statusBorder"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusBorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shizuku_status_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isGranted) StatusActiveBg else DarkSurfaceElevated,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (isGranted) AccentSuccess else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Text(
                        text = if (isGranted) "Служба Shizuku активна" else "Статус службы Shizuku",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isGranted) StatusActiveBg else DarkSurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isGranted) DarkBorder else DarkBorderSubtle
                    )
                ) {
                    Text(
                        text = if (isGranted) "ГОТОВО" else if (isRunning) "ТРЕБУЕТСЯ ДОСТУП" else "ОФФЛАЙН",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isGranted) AccentSuccess else if (isRunning) AccentPrimary else TextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = shizukuState.statusDescription,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            if (!isGranted) {
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
                            .height(40.dp)
                            .testTag("request_permission_button")
                    ) {
                        Text(
                            text = "Запросить доступ",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenShizuku,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
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
    val buttonBgColor by animateColorAsState(
        targetValue = if (is144Active) DarkSurfaceVariant else ButtonPrimaryBg,
        animationSpec = spring(stiffness = 500f),
        label = "unlockBtnBg"
    )
    val buttonTextColor by animateColorAsState(
        targetValue = if (is144Active) AccentSuccess else ButtonPrimaryText,
        animationSpec = spring(stiffness = 500f),
        label = "unlockBtnText"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "УПРАВЛЕНИЕ ЧАСТОТОЙ (144 ГЦ)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = TextMuted
                )

                if (is144Active) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusActiveBg
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(AccentSuccess)
                            )
                            Text(
                                text = "144 Hz Force Active",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp
                                ),
                                color = AccentSuccess
                            )
                        }
                    }
                }
            }

            // 1. Primary Action: Force 144Hz Button
            Button(
                onClick = onUnlock144Hz,
                enabled = isReady && !isExecuting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("unlock_144hz_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = buttonBgColor,
                    contentColor = buttonTextColor,
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
                        modifier = Modifier.size(20.dp),
                        tint = if (is144Active) AccentSuccess else ButtonPrimaryText
                    )
                    Text(
                        text = if (is144Active) "144 Гц Активирован (Все ключи = 1)" else "Активировать 144 Гц",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.2).sp
                        )
                    )
                }
            }

            // 2. Secondary Action: Reset to Standard
            OutlinedButton(
                onClick = onRestoreStandard,
                enabled = isReady && !isExecuting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
                        modifier = Modifier.size(18.dp),
                        tint = TextSecondary
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
                    .height(36.dp)
                    .testTag("factory_reset_button"),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Очистить ключи (settings delete system)",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp
                    ),
                    color = TextMuted
                )
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
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ПАРАМЕТРЫ СИСТЕМЫ (SYSTEM SETTINGS)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
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
                            checkedTrackColor = AccentPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkSurfaceElevated,
                            uncheckedBorderColor = DarkBorder
                        ),
                        modifier = Modifier.testTag("auto_polling_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                color = TextSecondary
            )
            if (isOne) {
                Text(
                    text = "Принудительно 144 Гц",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = AccentSuccess
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isOne) StatusActiveBg else DarkSurfaceElevated,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isOne) DarkBorder else DarkBorderSubtle
            )
        ) {
            Text(
                text = displayValue,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                color = if (isOne) AccentSuccess else TextPrimary,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun CollapsibleShellSection(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
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
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "ADB Shell Команды",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextSecondary
                    )
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(150)) + slideInVertically(
                    animationSpec = spring(stiffness = 600f),
                    initialOffsetY = { -15 }
                ),
                exit = fadeOut(animationSpec = tween(120)) + slideOutVertically(
                    animationSpec = spring(stiffness = 600f),
                    targetOffsetY = { -15 }
                )
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                                focusedBorderColor = AccentPrimary,
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
                text = "Разблокировка 144 Гц на iQOO / vivo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "• На смартфонах vivo / iQOO значение 1 в системных настройках 'peak_refresh_rate', 'user_refresh_rate' и 'min_refresh_rate' переопределяет частоту экрана на максимальную (144 Гц) во всех поддерживаемых приложениях и играх.",
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
                Text("Понятно", color = AccentPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}
