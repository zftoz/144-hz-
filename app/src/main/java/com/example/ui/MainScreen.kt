package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.data.BackupState
import com.example.data.CommandLog
import com.example.data.RefreshRateValues
import com.example.shizuku.ShizukuState
import com.example.ui.theme.AdbBlue
import com.example.ui.theme.AdbBlueContainer
import com.example.ui.theme.ChipBackground
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightOutlineSecondary
import com.example.ui.theme.PurpleOnPrimaryContainer
import com.example.ui.theme.PurplePrimary
import com.example.ui.theme.PurplePrimaryContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.flow.collectLatest

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showHelpDialog by remember { mutableStateOf(false) }
    var customCommandText by remember { mutableStateOf("") }
    var isTerminalExpanded by remember { mutableStateOf(true) }

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
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. Header with ADB Status and Display FPS
            item {
                MinimalHeader(
                    shizukuState = uiState.shizukuState,
                    displayFps = uiState.displayFps,
                    is144Active = uiState.is144HzActive,
                    isExecuting = uiState.isExecuting,
                    onHelpClick = { showHelpDialog = true },
                    onRefresh = { viewModel.refreshAll() }
                )
            }

            // 2. Current Settings Card with Preserved Original Values
            item {
                CurrentSettingsCard(
                    currentValues = uiState.currentValues,
                    backupState = uiState.backupState,
                    isPolling = uiState.isPolling,
                    onTogglePolling = { viewModel.togglePolling(it) },
                    onRefresh = { viewModel.refreshSystemValues() }
                )
            }

            // 3. Preserved Original Baseline Card
            item {
                OriginalBaselineCard(
                    backupState = uiState.backupState,
                    onRecaptureBaseline = { viewModel.forceCaptureCurrentAsBaseline() }
                )
            }

            // 4. Shizuku Permissions Banner
            item {
                ShizukuPermissionBanner(
                    shizukuState = uiState.shizukuState,
                    onRequestPermission = { viewModel.requestShizukuPermission() },
                    onOpenShizuku = { viewModel.openShizukuApp() }
                )
            }

            // 5. Action Buttons (Clean Rounded-Full buttons with explicit Value=1 logic)
            item {
                MinimalActionButtons(
                    isReady = uiState.shizukuState.isPermissionGranted,
                    isExecuting = uiState.isExecuting,
                    is144Active = uiState.is144HzActive,
                    onUnlock144Hz = { viewModel.unlock144Hz() },
                    onRestoreStandard = { viewModel.restoreStandard() },
                    onFactoryReset = { viewModel.factoryReset() }
                )
            }

            // 6. ADB Shell Command Terminal
            item {
                AdbCommandTerminal(
                    logs = uiState.logs,
                    isExpanded = isTerminalExpanded,
                    onToggleExpand = { isTerminalExpanded = !isTerminalExpanded },
                    onClearLogs = { viewModel.clearLogs() },
                    customCommand = customCommandText,
                    onCustomCommandChange = { customCommandText = it },
                    onRunCustomCommand = {
                        viewModel.runCustomShellCommand(customCommandText)
                        customCommandText = ""
                    },
                    onPresetClick = { presetCmd ->
                        customCommandText = presetCmd
                        viewModel.runCustomShellCommand(presetCmd)
                    },
                    onCopyLog = { viewModel.copyToClipboard(it) }
                )
            }
        }
    }

    if (showHelpDialog) {
        ShizukuHelpDialog(onDismiss = { showHelpDialog = false })
    }
}

@Composable
fun MinimalHeader(
    shizukuState: ShizukuState,
    displayFps: Float,
    is144Active: Boolean,
    isExecuting: Boolean,
    onHelpClick: () -> Unit,
    onRefresh: () -> Unit
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Display Lab",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                if (isExecuting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PurplePrimary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // ADB Status Badge Pill
                AdbStatusPill(shizukuState = shizukuState)

                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Справка",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "iQOO 144Hz Frequency Override (Flag = 1)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Current display refresh rate indicator
            val fpsInt = displayFps.toInt()
            if (fpsInt > 0) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (is144Active) SuccessGreen.copy(alpha = 0.15f) else PurplePrimaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "$fpsInt Hz",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        ),
                        color = if (is144Active) SuccessGreen else PurplePrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AdbStatusPill(shizukuState: ShizukuState) {
    val isReady = shizukuState.isRunning && shizukuState.isPermissionGranted

    val infiniteTransition = rememberInfiniteTransition(label = "adbPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    val bgColor = if (isReady) AdbBlueContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isReady) AdbBlue else MaterialTheme.colorScheme.onSurfaceVariant
    val label = if (isReady) "ADB ACTIVE" else if (shizukuState.isRunning) "PERM NEEDED" else "NO ADB"

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        modifier = Modifier.testTag("shizuku_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isReady) AdbBlue.copy(alpha = dotAlpha) else WarningAmber
                    )
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = textColor
            )
        }
    }
}

@Composable
fun CurrentSettingsCard(
    currentValues: RefreshRateValues,
    backupState: BackupState,
    isPolling: Boolean,
    onTogglePolling: (Boolean) -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_settings_card"),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline),
            width = 1.dp
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Section Title with Icon Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PurplePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "CURRENT SETTINGS",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "System Settings (Value 1 = 144Hz Unlocked)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Switch(
                        checked = isPolling,
                        onCheckedChange = onTogglePolling,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PurplePrimary,
                            checkedTrackColor = PurplePrimaryContainer
                        ),
                        modifier = Modifier.testTag("auto_polling_switch")
                    )
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("refresh_keys_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Обновить ключи",
                            tint = PurplePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 1. peak_refresh_rate
            MinimalSettingRowWithOriginal(
                keyName = "peak_refresh_rate",
                currentVal = currentValues.peak,
                originalVal = if (backupState.exists) backupState.values.peak else null
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // 2. user_refresh_rate
            MinimalSettingRowWithOriginal(
                keyName = "user_refresh_rate",
                currentVal = currentValues.user,
                originalVal = if (backupState.exists) backupState.values.user else null
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // 3. min_refresh_rate
            MinimalSettingRowWithOriginal(
                keyName = "min_refresh_rate",
                currentVal = currentValues.min,
                originalVal = if (backupState.exists) backupState.values.min else null
            )
        }
    }
}

@Composable
fun MinimalSettingRowWithOriginal(
    keyName: String,
    currentVal: String,
    originalVal: String?
) {
    val displayCurrent = if (currentVal.isBlank()) "—" else currentVal
    val isCurrentOne = currentVal == "1"
    val isNullOrDeleted = currentVal.lowercase() == "null" || currentVal.isBlank()

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = keyName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (originalVal != null) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Original: ${if (originalVal.isBlank() || originalVal == "null") "Default" else originalVal}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isCurrentOne) PurplePrimaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isCurrentOne) "Unlocked (1)" else if (isNullOrDeleted) "Default" else "Active",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (isCurrentOne) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isCurrentOne) PurplePrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = displayCurrent,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = if (isCurrentOne) PurplePrimary else if (isNullOrDeleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
            )

            if (isCurrentOne) {
                Text(
                    text = "➜ 144 Hz Force Override",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        color = PurplePrimary
                    )
                )
            }
        }
    }
}

@Composable
fun OriginalBaselineCard(
    backupState: BackupState,
    onRecaptureBaseline: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Исходные заводские значения (Baseline)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (backupState.exists) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SuccessGreen.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Preserved",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = SuccessGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (backupState.exists) {
                Text(
                    text = "Зафиксированы до изменений (${backupState.savedAt}):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MinimalBaselinePill("peak", backupState.values.peak, Modifier.weight(1f))
                    MinimalBaselinePill("user", backupState.values.user, Modifier.weight(1f))
                    MinimalBaselinePill("min", backupState.values.min, Modifier.weight(1f))
                }
            } else {
                Text(
                    text = "Исходные значения будут сохранены автоматически перед первым изменением на 1, чтобы вы всегда могли вернуться к ним.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun MinimalBaselinePill(key: String, value: String, modifier: Modifier = Modifier) {
    val display = if (value.isBlank() || value == "null") "Default" else value
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = display,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ShizukuPermissionBanner(
    shizukuState: ShizukuState,
    onRequestPermission: () -> Unit,
    onOpenShizuku: () -> Unit
) {
    val isReady = shizukuState.isRunning && shizukuState.isPermissionGranted

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PurplePrimaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shizuku_status_card")
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(PurplePrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = PurpleOnPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "SHIZUKU PERMISSIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = PurpleOnPrimaryContainer
                )

                Text(
                    text = if (isReady) {
                        "Authorized via ADB. Ready to execute shell commands for system settings modification."
                    } else {
                        shizukuState.statusDescription
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                if (!isReady) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (shizukuState.isRunning && !shizukuState.isPermissionGranted) {
                        Button(
                            onClick = onRequestPermission,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurplePrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("request_permission_button")
                        ) {
                            Text("Дать доступ Shizuku", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    } else {
                        Button(
                            onClick = onOpenShizuku,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AdbBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("open_shizuku_button")
                        ) {
                            Text("Открыть Shizuku", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalActionButtons(
    isReady: Boolean,
    isExecuting: Boolean,
    is144Active: Boolean,
    onUnlock144Hz: () -> Unit,
    onRestoreStandard: () -> Unit,
    onFactoryReset: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Primary Button: Unlock 144 Hz Mode (Sets value to 1)
        Button(
            onClick = onUnlock144Hz,
            enabled = isReady && !isExecuting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("unlock_144hz_button"),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurplePrimary,
                contentColor = Color.White
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (is144Active) "144 Hz Mode Active (Value = 1)" else "Unlock 144 Hz Mode",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // 2. Secondary Outlined Button: Reset to System Default (Restores preserved original values)
        OutlinedButton(
            onClick = onRestoreStandard,
            enabled = isReady && !isExecuting,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("restore_standard_button"),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = PurplePrimary
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(LightOutlineSecondary),
                width = 1.dp
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Reset to System Default",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        // 3. Factory Reset Action (Delete override keys)
        TextButton(
            onClick = onFactoryReset,
            enabled = isReady && !isExecuting,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("factory_reset_button"),
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = "Сбросить ключи (settings delete system)",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Helper explanation note
        Text(
            text = "Resetting will restore preserved baseline values or delete override keys to activate vivo Smart Switch.",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun AdbCommandTerminal(
    logs: List<CommandLog>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClearLogs: () -> Unit,
    customCommand: String,
    onCustomCommandChange: (String) -> Unit,
    onRunCustomCommand: () -> Unit,
    onPresetClick: (String) -> Unit,
    onCopyLog: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = PurplePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "ADB Shell Terminal (${logs.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    if (logs.isNotEmpty()) {
                        TextButton(onClick = onClearLogs) {
                            Text("Очистить", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    TextButton(onClick = onToggleExpand) {
                        Text(
                            if (isExpanded) "Свернуть" else "Развернуть",
                            style = MaterialTheme.typography.labelSmall,
                            color = PurplePrimary
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    // Quick ADB Presets
                    Text(
                        text = "Быстрые команды ADB:",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AdbPresetChip("get peak", "settings get system peak_refresh_rate", onPresetClick)
                        AdbPresetChip("put peak 1", "settings put system peak_refresh_rate 1", onPresetClick)
                        AdbPresetChip("get user", "settings get system user_refresh_rate", onPresetClick)
                        AdbPresetChip("put user 1", "settings put system user_refresh_rate 1", onPresetClick)
                        AdbPresetChip("dumpsys display", "dumpsys display | grep -E 'mDefaultModeId|mActiveModeId|mSupportedModes'", onPresetClick)
                        AdbPresetChip("delete all", "settings delete system peak_refresh_rate && settings delete system user_refresh_rate && settings delete system min_refresh_rate", onPresetClick)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom command input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = customCommand,
                            onValueChange = onCustomCommandChange,
                            placeholder = { Text("settings get system ...", style = MaterialTheme.typography.bodySmall) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PurplePrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )

                        Button(
                            onClick = onRunCustomCommand,
                            colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(50.dp)
                        ) {
                            Text("Run", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Log output container
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF1E1B24),
                        border = CardDefaults.outlinedCardBorder().copy(width = 0.8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        if (logs.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "История команд пуста. Нажмите любую кнопку или введите команду.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCAC4D0)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(logs) { log ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onCopyLog("${log.command}\n${log.output}") }
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "[${log.timestamp}] $ ${log.command}",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                color = if (log.isSuccess) Color(0xFFD0BCFF) else Color(0xFFFFB4AB)
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Скопировать",
                                                tint = Color(0xFF79747E),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                        Text(
                                            text = "↳ ${log.output}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = FontFamily.Monospace
                                            ),
                                            color = if (log.isSuccess) Color(0xFFA5D6A7) else Color(0xFFFFB4AB),
                                            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdbPresetChip(
    title: String,
    cmd: String,
    onClick: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick(cmd) }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ShizukuHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                shape = RoundedCornerShape(50)
            ) {
                Text("Понятно", fontWeight = FontWeight.Bold)
            }
        },
        title = {
            Text(
                "Разблокировка 144 Гц на iQOO / vivo",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "• На устройствах iQOO / vivo для разблокировки 144 Гц ключи peak_refresh_rate, user_refresh_rate и min_refresh_rate должны иметь значение 1.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• Установка других значений (например, 144.0) не снимает ограничение OriginOS / FuntouchOS.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• Приложение автоматически сохраняет исходные значения перед изменением, чтобы вы могли вернуть стандартный режим в один клик.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "• Встроенный ADB терминал позволяет выполнять любые команды Shizuku напрямую.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )
}
