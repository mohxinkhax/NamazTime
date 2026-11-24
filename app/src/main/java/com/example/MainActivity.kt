package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.PrayerType
import com.example.ui.ColorThemeSheet
import com.example.ui.DashboardScreen
import com.example.ui.GlobalSettingsSheet
import com.example.ui.LocationPickerSheet
import com.example.ui.PrayerEditSheet
import com.example.ui.PrayerViewModel
import com.example.ui.theme.NamazTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: PrayerViewModel = viewModel()
            val state by viewModel.uiState.collectAsStateWithLifecycle()

            var editingPrayer by remember { mutableStateOf<PrayerType?>(null) }
            var showColorSheet by remember { mutableStateOf(false) }
            var showLocationSheet by remember { mutableStateOf(false) }
            var showSettingsSheet by remember { mutableStateOf(false) }

            val notificationLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted) {
                    viewModel.refreshAlarms()
                }
            }

            // Keep screen always on when enabled (default ON for all-day table/tablet clock)
            LaunchedEffect(state.settings.keepScreenOn) {
                if (state.settings.keepScreenOn) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Gently request POST_NOTIFICATIONS on Android 13+ once on launch if not yet granted
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val hasPerm = ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    if (!hasPerm) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            NamazTimeTheme(
                preset = state.settings.themePreset,
                activeAccentColor = state.activeAccentColor
            ) {
                DashboardScreen(
                    state = state,
                    onLocationClick = {
                        viewModel.clearLocationMessage()
                        showLocationSheet = true
                    },
                    onColorThemeClick = { showColorSheet = true },
                    onSettingsClick = { showSettingsSheet = true },
                    onPrayerCardClick = { prayer -> editingPrayer = prayer },
                    onTogglePrayerAlarm = { prayer -> viewModel.togglePrayerAlarm(prayer) },
                    onStopAzanClick = { viewModel.stopSoftAzan() }
                )

                editingPrayer?.let { prayer ->
                    PrayerEditSheet(
                        prayerType = prayer,
                        state = state,
                        autoTimeHourMin = viewModel.getAutoCalculatedTime(prayer),
                        onSave = { adj -> viewModel.updatePrayerAdjustment(prayer, adj) },
                        onResetAuto = { viewModel.resetPrayerToAuto(prayer) },
                        onPreviewTone = { tone, vol, label ->
                            viewModel.previewSoftAzan(tone, vol, label)
                        },
                        onStopTone = { viewModel.stopSoftAzan() },
                        onDismiss = { editingPrayer = null }
                    )
                }

                if (showColorSheet) {
                    ColorThemeSheet(
                        state = state,
                        onSelectPreset = { viewModel.selectThemePreset(it) },
                        onToggleDynamicColors = { viewModel.setDynamicPrayerColors(it) },
                        onDismiss = { showColorSheet = false }
                    )
                }

                if (showLocationSheet) {
                    LocationPickerSheet(
                        state = state,
                        onDetectGps = { viewModel.detectCurrentLocation() },
                        onSelectCity = { viewModel.selectCity(it) },
                        onDismiss = { showLocationSheet = false }
                    )
                }

                if (showSettingsSheet) {
                    GlobalSettingsSheet(
                        state = state,
                        onCalculationMethodChange = { viewModel.updateCalculationMethod(it) },
                        onAsrMethodChange = { viewModel.updateAsrMethod(it) },
                        onHijriOffsetChange = { viewModel.updateHijriOffset(it) },
                        on24HourToggle = { viewModel.set24HourFormat(it) },
                        onKeepScreenOnToggle = { viewModel.setKeepScreenOn(it) },
                        onDefaultToneChange = { viewModel.updateDefaultAzanTone(it) },
                        onAzanVolumeChange = { viewModel.updateSoftAzanVolume(it) },
                        onPreviewTone = { tone, vol, label ->
                            viewModel.previewSoftAzan(tone, vol, label)
                        },
                        onStopTone = { viewModel.stopSoftAzan() },
                        onRefreshAlarms = { viewModel.refreshAlarms() },
                        onDismiss = { showSettingsSheet = false }
                    )
                }
            }
        }
    }
}
