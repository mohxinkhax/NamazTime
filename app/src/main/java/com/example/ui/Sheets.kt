package com.example.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alarm.PrayerAlarmScheduler
import com.example.domain.CityCatalog
import com.example.model.AsrJuristicMethod
import com.example.model.CalculationMethod
import com.example.model.CityLocation
import com.example.model.DarkThemeColorPreset
import com.example.model.PrayerAdjustment
import com.example.model.PrayerType
import com.example.model.SoftAzanTone
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerEditSheet(
    prayerType: PrayerType,
    state: PrayerUiState,
    autoTimeHourMin: Pair<Int, Int>,
    onSave: (PrayerAdjustment) -> Unit,
    onResetAuto: () -> Unit,
    onPreviewTone: (SoftAzanTone, Float, String) -> Unit,
    onStopTone: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentAdj = state.settings.adjustments[prayerType] ?: PrayerAdjustment()
    var alarmEnabled by remember(prayerType, currentAdj) { mutableStateOf(currentAdj.alarmEnabled) }
    var useCustomFixedTime by remember(prayerType, currentAdj) { mutableStateOf(currentAdj.isCustomFixedTime) }
    var offsetMinutes by remember(prayerType, currentAdj) { mutableIntStateOf(currentAdj.offsetMinutes) }
    var customHour by remember(prayerType, currentAdj) {
        mutableIntStateOf(currentAdj.customHour ?: autoTimeHourMin.first)
    }
    var customMinute by remember(prayerType, currentAdj) {
        mutableIntStateOf(currentAdj.customMinute ?: autoTimeHourMin.second)
    }
    var selectedTone by remember(prayerType, currentAdj) {
        mutableStateOf(currentAdj.azanTone ?: state.settings.defaultAzanTone)
    }
    var reminderLeadMinutes by remember(prayerType, currentAdj) {
        mutableIntStateOf(currentAdj.reminderBeforeMinutes)
    }
    var useCustomVolume by remember(prayerType, currentAdj) {
        mutableStateOf(currentAdj.customVolume != null)
    }
    var customVolume by remember(prayerType, currentAdj) {
        mutableStateOf(currentAdj.customVolume ?: state.settings.softAzanVolume)
    }

    val prayerColor = if (state.settings.dynamicPrayerColors) {
        prayerType.defaultColor
    } else {
        Color(state.settings.themePreset.accentHex)
    }

    fun persistChanges(
        alarm: Boolean = alarmEnabled,
        fixed: Boolean = useCustomFixedTime,
        offset: Int = offsetMinutes,
        hour: Int = customHour,
        minute: Int = customMinute,
        tone: SoftAzanTone = selectedTone,
        lead: Int = reminderLeadMinutes,
        customVolEnabled: Boolean = useCustomVolume,
        vol: Float = customVolume
    ) {
        onSave(
            PrayerAdjustment(
                alarmEnabled = alarm,
                offsetMinutes = if (fixed) 0 else offset.coerceIn(-60, 60),
                customHour = if (fixed) hour else null,
                customMinute = if (fixed) minute else null,
                azanTone = tone,
                reminderBeforeMinutes = lead,
                customVolume = if (customVolEnabled) vol else null
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(prayerColor)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${prayerType.displayName} Settings",
                            style = MaterialTheme.typography.titleLarge,
                            color = prayerColor
                        )
                        val autoStr = String.format(
                            Locale.US,
                            "%02d:%02d",
                            autoTimeHourMin.first,
                            autoTimeHourMin.second
                        )
                        Text(
                            text = "Location calculated: $autoStr",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(
                    onClick = {
                        useCustomFixedTime = false
                        offsetMinutes = 0
                        customHour = autoTimeHourMin.first
                        customMinute = autoTimeHourMin.second
                        onResetAuto()
                    },
                    modifier = Modifier.testTag("reset_prayer_auto_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset Auto")
                }
            }

            Spacer(Modifier.height(14.dp))

            // Soft Azan Alarm Toggle
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${prayerType.displayName} Soft Azan Alarm",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Notify with gentle Azan tone at prayer time",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = alarmEnabled,
                        onCheckedChange = {
                            alarmEnabled = it
                            persistChanges(alarm = it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = prayerColor
                        ),
                        modifier = Modifier.testTag("sheet_alarm_switch")
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Time Customization Mode
            Text(
                text = "Prayer Time Adjustment",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = !useCustomFixedTime,
                    onClick = {
                        useCustomFixedTime = false
                        persistChanges(fixed = false)
                    },
                    label = { Text("Auto + Offset") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = prayerColor.copy(alpha = 0.22f),
                        selectedLabelColor = prayerColor
                    ),
                    modifier = Modifier.testTag("mode_auto_offset_chip")
                )
                FilterChip(
                    selected = useCustomFixedTime,
                    onClick = {
                        useCustomFixedTime = true
                        persistChanges(fixed = true)
                    },
                    label = { Text("Custom Fixed Time") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = prayerColor.copy(alpha = 0.22f),
                        selectedLabelColor = prayerColor
                    ),
                    modifier = Modifier.testTag("mode_custom_time_chip")
                )
            }

            Spacer(Modifier.height(12.dp))

            if (!useCustomFixedTime) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                val next = (offsetMinutes - 1).coerceAtLeast(-60)
                                offsetMinutes = next
                                persistChanges(offset = next)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("offset_minus_button")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Subtract 1 minute")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val sign = if (offsetMinutes > 0) "+" else ""
                            Text(
                                text = "$sign$offsetMinutes min",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = prayerColor
                            )
                            Text(
                                text = "Relative to location time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                val next = (offsetMinutes + 1).coerceAtMost(60)
                                offsetMinutes = next
                                persistChanges(offset = next)
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("offset_plus_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add 1 minute")
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TimeStepperColumn(
                            label = "Hour",
                            value = customHour,
                            accentColor = prayerColor,
                            onDecrement = {
                                val next = (customHour + 23) % 24
                                customHour = next
                                persistChanges(hour = next)
                            },
                            onIncrement = {
                                val next = (customHour + 1) % 24
                                customHour = next
                                persistChanges(hour = next)
                            }
                        )
                        Text(
                            text = ":",
                            style = MaterialTheme.typography.displayMedium,
                            color = prayerColor
                        )
                        TimeStepperColumn(
                            label = "Minute",
                            value = customMinute,
                            accentColor = prayerColor,
                            onDecrement = {
                                val next = (customMinute + 59) % 60
                                customMinute = next
                                persistChanges(minute = next)
                            },
                            onIncrement = {
                                val next = (customMinute + 1) % 60
                                customMinute = next
                                persistChanges(minute = next)
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Per-Prayer Unique Soft Azan Tone Selector
            Text(
                text = "Soft Azan Tone for ${prayerType.displayName}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftAzanTone.entries.forEach { tone ->
                    val isSelected = selectedTone == tone
                    val isThisPlaying = state.azanPlayback.isPlaying && state.azanPlayback.tone == tone
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) prayerColor.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) prayerColor else Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedTone = tone
                                persistChanges(tone = tone)
                            }
                            .testTag("tone_option_${tone.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) prayerColor else Color.Transparent)
                                        .border(1.5.dp, prayerColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = tone.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = tone.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (isThisPlaying) {
                                        onStopTone()
                                    } else {
                                        onPreviewTone(tone, customVolume, "${prayerType.displayName} Preview")
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("preview_tone_${tone.id}")
                            ) {
                                Icon(
                                    imageVector = if (isThisPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isThisPlaying) "Stop preview" else "Preview ${tone.label}",
                                    tint = prayerColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Per-Prayer Lead Time
            Text(
                text = "Alarm Timing",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(0 to "At Time", 5 to "5m Before", 10 to "10m Before", 15 to "15m Before").forEach { (mins, label) ->
                    FilterChip(
                        selected = reminderLeadMinutes == mins,
                        onClick = {
                            reminderLeadMinutes = mins
                            persistChanges(lead = mins)
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = prayerColor.copy(alpha = 0.22f),
                            selectedLabelColor = prayerColor
                        ),
                        modifier = Modifier.testTag("lead_time_$mins")
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Unique Per-Prayer Soft Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Custom Soft Volume (${(customVolume * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = useCustomVolume,
                    onCheckedChange = {
                        useCustomVolume = it
                        persistChanges(customVolEnabled = it)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = prayerColor
                    )
                )
            }
            if (useCustomVolume) {
                Slider(
                    value = customVolume,
                    onValueChange = { customVolume = it },
                    onValueChangeFinished = { persistChanges(vol = customVolume) },
                    valueRange = 0.15f..0.85f,
                    colors = SliderDefaults.colors(
                        thumbColor = prayerColor,
                        activeTrackColor = prayerColor
                    )
                )
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = prayerColor,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("done_prayer_sheet_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun TimeStepperColumn(
    label: String,
    value: Int,
    accentColor: Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        IconButton(
            onClick = onIncrement,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase $label", tint = accentColor)
        }
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.displayMedium.copy(fontFamily = JetBrainsMonoFontFamily),
            color = accentColor
        )
        IconButton(
            onClick = onDecrement,
            modifier = Modifier.minimumInteractiveComponentSize()
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease $label", tint = accentColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorThemeSheet(
    state: PrayerUiState,
    onSelectPreset: (DarkThemeColorPreset) -> Unit,
    onToggleDynamicColors: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Dark Mode Colors",
                style = MaterialTheme.typography.titleLarge,
                color = state.activeAccentColor
            )
            Text(
                text = "AMOLED dark surfaces designed for low battery consumption",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Color Change Per Namaz Time",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Each prayer button shifts color when its time arrives",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = state.settings.dynamicPrayerColors,
                        onCheckedChange = onToggleDynamicColors,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = state.activeAccentColor
                        ),
                        modifier = Modifier.testTag("dynamic_prayer_colors_switch")
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkThemeColorPreset.entries.forEach { preset ->
                    val selected = state.settings.themePreset == preset
                    val presetAccent = Color(preset.accentHex)
                    val presetBg = Color(preset.backgroundHex)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            if (selected) 1.5.dp else 1.dp,
                            if (selected) presetAccent else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectPreset(preset) }
                            .testTag("theme_preset_${preset.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(presetBg)
                                        .border(3.dp, presetAccent, CircleShape)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = presetAccent
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    state: PrayerUiState,
    onDetectGps: () -> Unit,
    onSelectCity: (CityLocation) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onDetectGps()
        }
    }

    val filteredCities = remember(searchQuery) {
        if (searchQuery.isBlank()) CityCatalog.presetCities
        else CityCatalog.presetCities.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.country.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Prayer Location",
                style = MaterialTheme.typography.titleLarge,
                color = state.activeAccentColor
            )
            Text(
                text = "Current: ${state.settings.location.name}, ${state.settings.location.country}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                },
                enabled = !state.isDetectingLocation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = state.activeAccentColor,
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("detect_gps_button")
            ) {
                if (state.isDetectingLocation) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Detecting Location...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use Current Location (GPS)", fontWeight = FontWeight.Bold)
                }
            }

            state.locationStatusMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelMedium,
                    color = state.activeAccentColor
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search city or country...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("city_search_input")
            )

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredCities, key = { "${it.name}_${it.country}" }) { city ->
                    val isSelected = state.settings.location.name == city.name &&
                        !state.settings.location.isGps
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) state.activeAccentColor.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                onSelectCity(city)
                                onDismiss()
                            }
                            .testTag("city_item_${city.name.lowercase().replace(" ", "_")}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isSelected) state.activeAccentColor
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = city.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = city.country,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = state.activeAccentColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSettingsSheet(
    state: PrayerUiState,
    onCalculationMethodChange: (CalculationMethod) -> Unit,
    onAsrMethodChange: (AsrJuristicMethod) -> Unit,
    onHijriOffsetChange: (Int) -> Unit,
    on24HourToggle: (Boolean) -> Unit,
    onKeepScreenOnToggle: (Boolean) -> Unit,
    onDefaultToneChange: (SoftAzanTone) -> Unit,
    onAzanVolumeChange: (Float) -> Unit,
    onPreviewTone: (SoftAzanTone, Float, String) -> Unit,
    onStopTone: () -> Unit,
    onRefreshAlarms: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var volumeSlider by remember(state.settings.softAzanVolume) {
        mutableStateOf(state.settings.softAzanVolume)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onRefreshAlarms()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Prayer & Azan Settings",
                style = MaterialTheme.typography.titleLarge,
                color = state.activeAccentColor
            )
            Spacer(Modifier.height(14.dp))

            // Permissions Helper
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                OutlinedButton(
                    onClick = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("enable_notifications_button")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Allow Prayer Notifications")
                }
                Spacer(Modifier.height(8.dp))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !PrayerAlarmScheduler.canScheduleExactAlarms(context)) {
                OutlinedButton(
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Enable Exact Alarm Permission")
                }
                Spacer(Modifier.height(10.dp))
            }

            // 24h Clock Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("24-Hour Clock Format", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = state.settings.use24HourFormat,
                    onCheckedChange = on24HourToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = state.activeAccentColor
                    ),
                    modifier = Modifier.testTag("toggle_24h_switch")
                )
            }

            Spacer(Modifier.height(10.dp))

            // Keep Screen Always On Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Keep Screen Always On", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Ideal for desk or table tablet display",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = state.settings.keepScreenOn,
                    onCheckedChange = onKeepScreenOnToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.Black,
                        checkedTrackColor = state.activeAccentColor
                    ),
                    modifier = Modifier.testTag("toggle_keep_screen_on_switch")
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Hijri Date Adjustment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Hijri Calendar Adjustment", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Adjust moon sighting offset (-2 to +2 days)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onHijriOffsetChange(state.settings.hijriOffsetDays - 1) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Subtract Hijri day")
                    }
                    Text(
                        text = "${if (state.settings.hijriOffsetDays > 0) "+" else ""}${state.settings.hijriOffsetDays}d",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = state.activeAccentColor
                    )
                    IconButton(
                        onClick = { onHijriOffsetChange(state.settings.hijriOffsetDays + 1) },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Hijri day")
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Soft Azan Default Tone & Volume
            Text(
                text = "Soft Azan Master Volume (${(volumeSlider * 100).roundToInt()}%)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = volumeSlider,
                onValueChange = { volumeSlider = it },
                onValueChangeFinished = { onAzanVolumeChange(volumeSlider) },
                valueRange = 0.15f..0.85f,
                colors = SliderDefaults.colors(
                    thumbColor = state.activeAccentColor,
                    activeTrackColor = state.activeAccentColor
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Default Tone: ${state.settings.defaultAzanTone.label}", style = MaterialTheme.typography.bodyMedium)
                TextButton(
                    onClick = {
                        if (state.azanPlayback.isPlaying) {
                            onStopTone()
                        } else {
                            onPreviewTone(
                                state.settings.defaultAzanTone,
                                volumeSlider,
                                "Soft Azan Test"
                            )
                        }
                    },
                    modifier = Modifier.testTag("test_soft_azan_button")
                ) {
                    Icon(
                        imageVector = if (state.azanPlayback.isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (state.azanPlayback.isPlaying) "Stop" else "Test Azan")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SoftAzanTone.entries.forEach { tone ->
                    FilterChip(
                        selected = state.settings.defaultAzanTone == tone,
                        onClick = { onDefaultToneChange(tone) },
                        label = { Text(tone.label.substringBefore(" (")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = state.activeAccentColor.copy(alpha = 0.22f),
                            selectedLabelColor = state.activeAccentColor
                        )
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Asr Juristic Method
            Text(
                text = "Asr Calculation Method",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AsrJuristicMethod.entries.forEach { method ->
                    FilterChip(
                        selected = state.settings.asrMethod == method,
                        onClick = { onAsrMethodChange(method) },
                        label = { Text(if (method == AsrJuristicMethod.STANDARD) "Standard (Shafi'i)" else "Hanafi") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = state.activeAccentColor.copy(alpha = 0.22f),
                            selectedLabelColor = state.activeAccentColor
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Calculation Method
            Text(
                text = "Prayer Calculation Authority",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CalculationMethod.entries.forEach { method ->
                    val selected = state.settings.calculationMethod == method
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) state.activeAccentColor.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onCalculationMethodChange(method) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(method.label, style = MaterialTheme.typography.bodyMedium)
                            if (selected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = state.activeAccentColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
