package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CountdownParts
import com.example.model.PrayerTimeEntry
import com.example.model.PrayerType
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun DashboardScreen(
    state: PrayerUiState,
    onLocationClick: () -> Unit,
    onColorThemeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPrayerCardClick: (PrayerType) -> Unit,
    onTogglePrayerAlarm: (PrayerType) -> Unit,
    onStopAzanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor by animateColorAsState(
        targetValue = state.activeAccentColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "activeAccent"
    )
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        bgColor,
                        bgColor.copy(alpha = 0.98f),
                        accentColor.copy(alpha = 0.05f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 600.dp

            if (isWideScreen) {
                LandscapeDashboard(
                    state = state,
                    accentColor = accentColor,
                    onLocationClick = onLocationClick,
                    onColorThemeClick = onColorThemeClick,
                    onSettingsClick = onSettingsClick,
                    onPrayerCardClick = onPrayerCardClick,
                    onTogglePrayerAlarm = onTogglePrayerAlarm
                )
            } else {
                PortraitDashboard(
                    state = state,
                    accentColor = accentColor,
                    onLocationClick = onLocationClick,
                    onColorThemeClick = onColorThemeClick,
                    onSettingsClick = onSettingsClick,
                    onPrayerCardClick = onPrayerCardClick,
                    onTogglePrayerAlarm = onTogglePrayerAlarm
                )
            }
        }

        // Floating Soft Azan Playback Stop Pill
        AnimatedVisibility(
            visible = state.azanPlayback.isPlaying,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.5.dp, accentColor),
                tonalElevation = 6.dp
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = state.azanPlayback.label ?: "Soft Azan Playing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(12.dp))
                    FilledTonalButton(
                        onClick = onStopAzanClick,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = accentColor,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.testTag("stop_azan_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Stop", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun LandscapeDashboard(
    state: PrayerUiState,
    accentColor: Color,
    onLocationClick: () -> Unit,
    onColorThemeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPrayerCardClick: (PrayerType) -> Unit,
    onTogglePrayerAlarm: (PrayerType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        TopDashboardBar(
            cityName = state.settings.location.name,
            isGps = state.settings.location.isGps,
            currentTimeFormatted = state.currentTimeFormatted,
            accentColor = accentColor,
            showInlineClock = true,
            onLocationClick = onLocationClick,
            onColorThemeClick = onColorThemeClick,
            onSettingsClick = onSettingsClick
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            NextPrayerHeading(
                nextPrayerName = state.schedule.nextPrayer.uppercaseName,
                accentColor = accentColor
            )
            Spacer(Modifier.height(14.dp))
            CountdownRow(
                countdown = state.schedule.countdown,
                accentColor = accentColor,
                compact = false
            )
            Spacer(Modifier.height(14.dp))
            DateHijriAndSunriseStrip(
                dateBanner = state.dateAndHijriBanner,
                sunriseFormatted = state.schedule.sunriseFormatted
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.schedule.entries.forEach { entry ->
                PrayerTimeCard(
                    entry = entry,
                    onClick = { onPrayerCardClick(entry.type) },
                    onToggleAlarm = { onTogglePrayerAlarm(entry.type) },
                    modifier = Modifier.weight(1f),
                    compact = false
                )
            }
        }

        VerseFooter()
    }
}

@Composable
private fun PortraitDashboard(
    state: PrayerUiState,
    accentColor: Color,
    onLocationClick: () -> Unit,
    onColorThemeClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onPrayerCardClick: (PrayerType) -> Unit,
    onTogglePrayerAlarm: (PrayerType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopDashboardBar(
            cityName = state.settings.location.name,
            isGps = state.settings.location.isGps,
            currentTimeFormatted = state.currentTimeFormatted,
            accentColor = accentColor,
            showInlineClock = false,
            onLocationClick = onLocationClick,
            onColorThemeClick = onColorThemeClick,
            onSettingsClick = onSettingsClick
        )

        Spacer(Modifier.height(6.dp))

        // Prominent Real-Time Clock
        Text(
            text = state.currentTimeFormatted,
            style = MaterialTheme.typography.displayMedium.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 34.sp
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.testTag("live_clock_text")
        )

        Spacer(Modifier.height(4.dp))

        DateHijriAndSunriseStrip(
            dateBanner = state.dateAndHijriBanner,
            sunriseFormatted = state.schedule.sunriseFormatted
        )

        Spacer(Modifier.height(14.dp))

        NextPrayerHeading(
            nextPrayerName = state.schedule.nextPrayer.uppercaseName,
            accentColor = accentColor
        )

        Spacer(Modifier.height(10.dp))

        CountdownRow(
            countdown = state.schedule.countdown,
            accentColor = accentColor,
            compact = true
        )

        Spacer(Modifier.height(18.dp))

        // 5 Namaz Cards in responsive 2+2+1 layout
        val entries = state.schedule.entries
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrayerTimeCard(
                    entry = entries[0],
                    onClick = { onPrayerCardClick(entries[0].type) },
                    onToggleAlarm = { onTogglePrayerAlarm(entries[0].type) },
                    modifier = Modifier.weight(1f),
                    compact = true
                )
                PrayerTimeCard(
                    entry = entries[1],
                    onClick = { onPrayerCardClick(entries[1].type) },
                    onToggleAlarm = { onTogglePrayerAlarm(entries[1].type) },
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PrayerTimeCard(
                    entry = entries[2],
                    onClick = { onPrayerCardClick(entries[2].type) },
                    onToggleAlarm = { onTogglePrayerAlarm(entries[2].type) },
                    modifier = Modifier.weight(1f),
                    compact = true
                )
                PrayerTimeCard(
                    entry = entries[3],
                    onClick = { onPrayerCardClick(entries[3].type) },
                    onToggleAlarm = { onTogglePrayerAlarm(entries[3].type) },
                    modifier = Modifier.weight(1f),
                    compact = true
                )
            }
            PrayerTimeCard(
                entry = entries[4],
                onClick = { onPrayerCardClick(entries[4].type) },
                onToggleAlarm = { onTogglePrayerAlarm(entries[4].type) },
                modifier = Modifier.fillMaxWidth(),
                compact = true
            )
        }

        Spacer(Modifier.height(14.dp))
        VerseFooter()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun TopDashboardBar(
    cityName: String,
    isGps: Boolean,
    currentTimeFormatted: String,
    accentColor: Color,
    showInlineClock: Boolean,
    onLocationClick: () -> Unit,
    onColorThemeClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onLocationClick)
                .minimumInteractiveComponentSize()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .testTag("location_button")
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mosque,
                    contentDescription = stringResource(R.string.cd_mosque_icon),
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = cityName,
                style = MaterialTheme.typography.titleLarge,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = if (isGps) Icons.Default.LocationOn else Icons.Default.Edit,
                contentDescription = stringResource(R.string.cd_edit_location),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (showInlineClock) {
                Text(
                    text = currentTimeFormatted,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .testTag("live_clock_text")
                )
            }
            IconButton(
                onClick = onColorThemeClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("color_theme_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = stringResource(R.string.cd_theme_colors),
                    tint = accentColor
                )
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun NextPrayerHeading(
    nextPrayerName: String,
    accentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.next_prayer_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = nextPrayerName,
            style = MaterialTheme.typography.headlineMedium,
            color = accentColor,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.in_label),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CountdownRow(
    countdown: CountdownParts,
    accentColor: Color,
    compact: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CountdownDigitBox(
            value = countdown.hours,
            label = stringResource(R.string.hours_label),
            accentColor = accentColor,
            compact = compact
        )
        CountdownColon(accentColor = accentColor)
        CountdownDigitBox(
            value = countdown.minutes,
            label = stringResource(R.string.minutes_label),
            accentColor = accentColor,
            compact = compact
        )
        CountdownColon(accentColor = accentColor)
        CountdownDigitBox(
            value = countdown.seconds,
            label = stringResource(R.string.seconds_label),
            accentColor = accentColor,
            compact = compact
        )
    }
}

@Composable
private fun CountdownDigitBox(
    value: String,
    label: String,
    accentColor: Color,
    compact: Boolean
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f)),
        modifier = Modifier.width(if (compact) 86.dp else 108.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = if (compact) 10.dp else 14.dp, horizontal = 6.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = if (compact) 34.sp else 42.sp,
                    lineHeight = if (compact) 38.sp else 46.sp
                ),
                color = accentColor
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CountdownColon(accentColor: Color) {
    Text(
        text = ":",
        style = MaterialTheme.typography.displayMedium,
        color = accentColor,
        modifier = Modifier.padding(horizontal = 8.dp)
    )
}

@Composable
private fun DateHijriAndSunriseStrip(
    dateBanner: String,
    sunriseFormatted: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = dateBanner,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.90f),
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("hijri_date_banner")
        )
        Spacer(Modifier.height(4.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.WbTwilight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "Sunrise (Shuruq) $sunriseFormatted",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PrayerTimeCard(
    entry: PrayerTimeEntry,
    onClick: () -> Unit,
    onToggleAlarm: () -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val activeHighlight = entry.isCurrent
    val targetBg = when {
        activeHighlight -> entry.buttonColor.copy(alpha = 0.16f)
        entry.isNext -> entry.buttonColor.copy(alpha = 0.08f)
        else -> MaterialTheme.colorScheme.surface
    }
    val targetBorder = when {
        activeHighlight -> entry.buttonColor
        entry.isNext -> entry.buttonColor.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outline
    }
    val cardBg by animateColorAsState(targetValue = targetBg, label = "cardBg")
    val cardBorder by animateColorAsState(targetValue = targetBorder, label = "cardBorder")

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(if (activeHighlight) 2.dp else 1.dp, cardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("prayer_card_${entry.type.name.lowercase()}")
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = if (compact) 10.dp else 14.dp)) {
            Column(
                horizontalAlignment = if (compact) Alignment.Start else Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (compact) Arrangement.SpaceBetween else Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.type.uppercaseName,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (activeHighlight || entry.isNext) entry.buttonColor
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeHighlight || entry.isNext) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (activeHighlight) entry.buttonColor else entry.buttonColor.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = if (activeHighlight) stringResource(R.string.now_badge)
                                    else stringResource(R.string.next_badge),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (activeHighlight) Color.Black else entry.buttonColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (compact) {
                        AlarmToggleIcon(
                            enabled = entry.alarmEnabled,
                            accentColor = entry.buttonColor,
                            onToggle = onToggleAlarm,
                            prayerType = entry.type
                        )
                    }
                }

                Spacer(Modifier.height(if (compact) 4.dp else 8.dp))

                Text(
                    text = entry.formattedTime,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = if (compact) 23.sp else 25.sp
                    ),
                    color = if (activeHighlight) entry.buttonColor else MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (compact) Arrangement.Start else Arrangement.Center
                ) {
                    val metaText = buildString {
                        if (entry.isCustomTime) {
                            append("Custom")
                        } else if (entry.offsetMinutes != 0) {
                            val sign = if (entry.offsetMinutes > 0) "+" else ""
                            append("${sign}${entry.offsetMinutes}m")
                        } else {
                            append("Auto")
                        }
                        append(" • ")
                        append(
                            if (entry.alarmEnabled) {
                                if (entry.reminderBeforeMinutes > 0) "-${entry.reminderBeforeMinutes}m Azan"
                                else "Azan"
                            } else {
                                "Muted"
                            }
                        )
                    }
                    Text(
                        text = metaText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!compact) {
                    Spacer(Modifier.height(6.dp))
                    AlarmToggleIcon(
                        enabled = entry.alarmEnabled,
                        accentColor = entry.buttonColor,
                        onToggle = onToggleAlarm,
                        prayerType = entry.type
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmToggleIcon(
    enabled: Boolean,
    accentColor: Color,
    onToggle: () -> Unit,
    prayerType: PrayerType
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier
            .size(36.dp)
            .minimumInteractiveComponentSize()
            .testTag("prayer_alarm_toggle_${prayerType.name.lowercase()}")
    ) {
        Icon(
            imageVector = if (enabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
            contentDescription = stringResource(
                if (enabled) R.string.cd_alarm_enabled else R.string.cd_alarm_disabled
            ),
            tint = if (enabled) accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun VerseFooter() {
    Text(
        text = stringResource(R.string.verse_text),
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    )
}
