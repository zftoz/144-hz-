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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainScreen
import com.example.ui.MainViewModel
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class OsMode(val title: String, val subtitle: String) {
    ORIGIN_OS("iQOO / OriginOS / vivo", "Shizuku ADB Override (144 Hz)"),
    HYPER_OS("Xiaomi / HyperOS / MIUI", "App Info Shortcut (Powerkeeper & Joyose)")
}

private const val PREFS_NAME = "fps_unlocker_prefs"
private const val KEY_OS_MODE = "key_os_mode"
private const val KEY_AUTO_LAUNCH = "key_auto_launch"
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
                                currentMode = mode
                                prefs.edit().putString(KEY_OS_MODE, mode.name).apply()
                            }
                        )
                    },
                    containerColor = DarkBg,
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AnimatedContent(
                            targetState = currentMode,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "mode_switch"
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

@Composable
fun OsModeTopBar(
    currentMode: OsMode,
    onSelectMode: (OsMode) -> Unit
) {
    Surface(
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // OriginOS / iQOO tab
            val isOrigin = currentMode == OsMode.ORIGIN_OS
            Surface(
                onClick = { onSelectMode(OsMode.ORIGIN_OS) },
                shape = RoundedCornerShape(10.dp),
                color = if (isOrigin) ButtonPrimaryBg else DarkSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("tab_origin_os")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = if (isOrigin) ButtonPrimaryText else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "iQOO / OriginOS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isOrigin) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isOrigin) ButtonPrimaryText else TextSecondary
                    )
                }
            }

            // HyperOS tab
            val isHyper = currentMode == OsMode.HYPER_OS
            Surface(
                onClick = { onSelectMode(OsMode.HYPER_OS) },
                shape = RoundedCornerShape(10.dp),
                color = if (isHyper) ButtonPrimaryBg else DarkSurface,
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("tab_hyper_os")
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = null,
                        tint = if (isHyper) ButtonPrimaryText else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HyperOS / MIUI",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isHyper) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = if (isHyper) ButtonPrimaryText else TextSecondary
                    )
                }
            }
        }
    }
}

/**
 * HyperOS / Xiaomi screen
 */
@Composable
fun HyperOsScreen() {
    val context = LocalContext.current
    val isPowerkeeperInstalled = remember { isPackageInstalled(context, PACKAGE_POWERKEEPER) }
    val isJoyoseInstalled = remember { isPackageInstalled(context, PACKAGE_JOYOSE) }

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var autoLaunchState by remember { mutableStateOf(prefs.getBoolean(KEY_AUTO_LAUNCH, false)) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = "HyperOS FPS Unlocker",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp
                    ),
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Быстрый сброс системных ограничений FPS в HyperOS / MIUI",
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
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ИНСТРУКЦИЯ ПО СБРОСУ",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = TextMuted
                        )
                    }

                    Text(
                        text = "После нажатия выберите 'Очистить данные' -> 'Очистить всё' для разблокировки FPS.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp
                        ),
                        color = TextPrimary
                    )

                    Text(
                        text = "Очистка кэша и данных службы питания снимает системное ограничение частоты кадров в играх и восстанавливает максимальную герцовку дисплея.",
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = TextSecondary
                    )
                }
            }
        }

        // Action 1: Powerkeeper
        item {
            Button(
                onClick = { openAppDetails(context, PACKAGE_POWERKEEPER, "Powerkeeper") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .testTag("open_powerkeeper_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ButtonPrimaryBg,
                    contentColor = ButtonPrimaryText
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = "Открыть настройки Питания",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
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
                        shape = RoundedCornerShape(6.dp),
                        color = if (isPowerkeeperInstalled) DarkSurfaceVariant else DarkSurfaceElevated
                    ) {
                        Text(
                            text = if (isPowerkeeperInstalled) "MIUI" else "—",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = if (isPowerkeeperInstalled) TextPrimary else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        // Action 2: Joyose
        item {
            OutlinedButton(
                onClick = { openAppDetails(context, PACKAGE_JOYOSE, "Joyose") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("open_joyose_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = ButtonSecondaryBg,
                    contentColor = ButtonSecondaryText
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, ButtonSecondaryBorder)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = TextSecondary
                        )
                        Column {
                            Text(
                                text = "Открыть настройки Joyose",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
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
                        shape = RoundedCornerShape(6.dp),
                        color = DarkSurfaceElevated
                    ) {
                        Text(
                            text = if (isJoyoseInstalled) "GAME" else "—",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            ),
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
