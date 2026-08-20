package com.example

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
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
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class OsMode(val title: String, val subtitle: String) {
    ORIGIN_OS("iQOO / OriginOS", "Shizuku ADB Override (144 Hz)"),
    HYPER_OS("HyperOS / MIUI", "Powerkeeper & Joyose Reset")
}

private const val PREFS_NAME = "fps_unlocker_prefs"
private const val KEY_OS_MODE = "key_os_mode"
const val PACKAGE_POWERKEEPER = "com.miui.powerkeeper"
const val PACKAGE_JOYOSE = "com.xiaomi.joyose"

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedModeName = prefs.getString(KEY_OS_MODE, OsMode.ORIGIN_OS.name)
        val initialMode = try {
            OsMode.valueOf(savedModeName ?: OsMode.ORIGIN_OS.name)
        } catch (e: Exception) {
            OsMode.ORIGIN_OS
        }

        setContent {
            MyApplicationTheme {
                var currentMode by remember { mutableStateOf(initialMode) }

                Scaffold(
                    topBar = {
                        OsModeTopBar(
                            currentMode = currentMode,
                            onSelectMode = { mode ->
                                if (currentMode != mode) {
                                    currentMode = mode
                                    prefs.edit().putString(KEY_OS_MODE, mode.name).apply()
                                }
                            }
                        )
                    },
                    containerColor = DarkBg,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentMode,
                            transitionSpec = {
                                if (targetState == OsMode.HYPER_OS) {
                                    (slideInHorizontally(
                                        animationSpec = spring(stiffness = 500f),
                                        initialOffsetX = { it / 3 }
                                    ) + fadeIn(animationSpec = tween(220))) togetherWith
                                            (slideOutHorizontally(
                                                animationSpec = spring(stiffness = 500f),
                                                targetOffsetX = { -it / 3 }
                                            ) + fadeOut(animationSpec = tween(180)))
                                } else {
                                    (slideInHorizontally(
                                        animationSpec = spring(stiffness = 500f),
                                        initialOffsetX = { -it / 3 }
                                    ) + fadeIn(animationSpec = tween(220))) togetherWith
                                            (slideOutHorizontally(
                                                animationSpec = spring(stiffness = 500f),
                                                targetOffsetX = { it / 3 }
                                            ) + fadeOut(animationSpec = tween(180)))
                                }
                            },
                            label = "os_mode_transition"
                        ) { mode ->
                            when (mode) {
                                OsMode.ORIGIN_OS -> {
                                    MainScreen(viewModel = viewModel)
                                }
                                OsMode.HYPER_OS -> {
                                    HyperOsScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Modern Segmented Capsule Selector with smooth animated indicator
 */
@Composable
fun OsModeTopBar(
    currentMode: OsMode,
    onSelectMode: (OsMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Surface(
            color = DarkSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OsTabItem(
                    title = "iQOO / vivo",
                    badge = "144 Hz",
                    icon = Icons.Default.Bolt,
                    isSelected = currentMode == OsMode.ORIGIN_OS,
                    testTag = "tab_origin_os",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(OsMode.ORIGIN_OS) }
                )

                OsTabItem(
                    title = "HyperOS",
                    badge = "MIUI",
                    icon = Icons.Default.PhoneAndroid,
                    isSelected = currentMode == OsMode.HYPER_OS,
                    testTag = "tab_hyper_os",
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectMode(OsMode.HYPER_OS) }
                )
            }
        }
    }
}

@Composable
fun OsTabItem(
    title: String,
    badge: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) DarkSurfaceElevated else Color.Transparent,
        animationSpec = spring(stiffness = 600f),
        label = "tabBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) TextPrimary else TextMuted,
        animationSpec = spring(stiffness = 600f),
        label = "tabText"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) DarkBorder else Color.Transparent,
        animationSpec = spring(stiffness = 600f),
        label = "tabBorder"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(42.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AccentPrimary else TextMuted,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    letterSpacing = (-0.2).sp
                ),
                color = contentColor
            )
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) DarkSurfaceVariant else DarkSurfaceElevated,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isSelected) AccentPrimary else TextMuted,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                )
            }
        }
    }
}

/**
 * HyperOS / Xiaomi screen with enhanced sleek card design
 */
@Composable
fun HyperOsScreen() {
    val context = LocalContext.current
    val isPowerkeeperInstalled = remember { isPackageInstalled(context, PACKAGE_POWERKEEPER) }
    val isJoyoseInstalled = remember { isPackageInstalled(context, PACKAGE_JOYOSE) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "HyperOS FPS Unlock",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = "MIUI / Xiaomi",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Сброс системных ограничений частоты кадров и термоконтроля",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        // Instructions Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = DarkSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = AccentPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = "ИНСТРУКЦИЯ ПО РАЗБЛОКИРОВКЕ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = TextSecondary
                        )
                    }

                    Text(
                        text = "1. Нажмите на сервис ниже для перехода в настройки приложения.\n" +
                                "2. Выберите пункт «Очистить данные» → «Очистить всё».\n" +
                                "3. Запустите игру/приложение — лимит FPS будет снят.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Normal,
                            lineHeight = 22.sp
                        ),
                        color = TextPrimary
                    )
                }
            }
        }

        // Action 1: Powerkeeper
        item {
            Card(
                onClick = { openAppDetails(context, PACKAGE_POWERKEEPER, "Powerkeeper") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_powerkeeper_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = AccentPrimary
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Питание и производительность",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = PACKAGE_POWERKEEPER,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = TextMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPowerkeeperInstalled) DarkSurfaceVariant else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle)
                    ) {
                        Text(
                            text = if (isPowerkeeperInstalled) "Открыть" else "Не найден",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = if (isPowerkeeperInstalled) TextPrimary else TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Action 2: Joyose
        item {
            Card(
                onClick = { openAppDetails(context, PACKAGE_JOYOSE, "Joyose") },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_joyose_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = DarkSurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SportsEsports,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                    tint = TextSecondary
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Игровой сервис Joyose",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = TextPrimary
                            )
                            Text(
                                text = PACKAGE_JOYOSE,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                ),
                                color = TextMuted
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isJoyoseInstalled) DarkSurfaceVariant else DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle)
                    ) {
                        Text(
                            text = if (isJoyoseInstalled) "Открыть" else "Не найден",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = if (isJoyoseInstalled) TextPrimary else TextMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

fun openAppDetails(context: Context, packageName: String, serviceDisplayName: String) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Сервис $serviceDisplayName не найден", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Сервис $serviceDisplayName не найден", Toast.LENGTH_SHORT).show()
    }
}

fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (e: Exception) {
        false
    }
}
