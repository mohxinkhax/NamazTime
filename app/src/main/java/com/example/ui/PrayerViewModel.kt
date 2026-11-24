package com.example.ui

import android.app.Application
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AzanPlaybackState
import com.example.alarm.PrayerAlarmScheduler
import com.example.alarm.SoftAzanPlayer
import com.example.data.SettingsRepository
import com.example.domain.HijriCalendarHelper
import com.example.domain.PrayerScheduleSnapshot
import com.example.domain.PrayerTimeCalculator
import com.example.location.LocationHelper
import com.example.model.AppSettings
import com.example.model.AsrJuristicMethod
import com.example.model.CalculationMethod
import com.example.model.CityLocation
import com.example.model.DarkThemeColorPreset
import com.example.model.PrayerAdjustment
import com.example.model.PrayerType
import com.example.model.SoftAzanTone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class PrayerUiState(
    val settings: AppSettings = AppSettings(),
    val nowMillis: Long = System.currentTimeMillis(),
    val currentTimeFormatted: String = "",
    val dateAndHijriBanner: String = "",
    val schedule: PrayerScheduleSnapshot = PrayerTimeCalculator.buildSchedule(
        System.currentTimeMillis(),
        AppSettings()
    ),
    val activeAccentColor: Color = Color(DarkThemeColorPreset.OLED_GOLD.accentHex),
    val azanPlayback: AzanPlaybackState = AzanPlaybackState(),
    val isDetectingLocation: Boolean = false,
    val locationStatusMessage: String? = null
)

class PrayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)
    private val nowFlow = MutableStateFlow(System.currentTimeMillis())
    private val locationDetectingFlow = MutableStateFlow(false)
    private val locationMessageFlow = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow(PrayerUiState())
    val uiState: StateFlow<PrayerUiState> = _uiState.asStateFlow()

    init {
        // 1-second aligned ticker for battery-efficient live clock and countdown
        viewModelScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                nowFlow.value = now
                val millisToNextSecond = 1000L - (now % 1000L)
                delay(millisToNextSecond.coerceIn(50L, 1000L))
            }
        }

        // Reschedule alarms whenever persisted settings change
        repository.settingsFlow
            .distinctUntilChanged()
            .onEach { settings ->
                PrayerAlarmScheduler.rescheduleAll(getApplication(), settings)
            }
            .launchIn(viewModelScope)

        combine(
            repository.settingsFlow,
            nowFlow,
            SoftAzanPlayer.playbackState,
            locationDetectingFlow,
            locationMessageFlow
        ) { settings, now, playback, isDetecting, locMsg ->
            val schedule = PrayerTimeCalculator.buildSchedule(now, settings)
            val tz = TimeZone.getTimeZone(settings.location.timeZoneId)
            val clockPattern = if (settings.use24HourFormat) "HH:mm:ss" else "hh:mm:ss a"
            val clockFormatted = SimpleDateFormat(clockPattern, Locale.getDefault())
                .apply { timeZone = tz }
                .format(Date(now))
            val banner = HijriCalendarHelper.formatCombinedDateBanner(
                epochMillis = now,
                timeZoneId = settings.location.timeZoneId,
                hijriOffsetDays = settings.hijriOffsetDays
            )
            val activeAccent = if (settings.dynamicPrayerColors) {
                (schedule.currentPrayer ?: schedule.nextPrayer).defaultColor
            } else {
                Color(settings.themePreset.accentHex)
            }

            PrayerUiState(
                settings = settings,
                nowMillis = now,
                currentTimeFormatted = clockFormatted,
                dateAndHijriBanner = banner,
                schedule = schedule,
                activeAccentColor = activeAccent,
                azanPlayback = playback,
                isDetectingLocation = isDetecting,
                locationStatusMessage = locMsg
            )
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    fun togglePrayerAlarm(prayer: PrayerType) {
        viewModelScope.launch {
            val current = _uiState.value.settings.adjustments[prayer] ?: PrayerAdjustment()
            repository.updatePrayerAdjustment(
                prayer,
                current.copy(alarmEnabled = !current.alarmEnabled)
            )
        }
    }

    fun updatePrayerAdjustment(prayer: PrayerType, adjustment: PrayerAdjustment) {
        viewModelScope.launch {
            repository.updatePrayerAdjustment(prayer, adjustment)
        }
    }

    fun resetPrayerToAuto(prayer: PrayerType) {
        viewModelScope.launch {
            val current = _uiState.value.settings.adjustments[prayer] ?: PrayerAdjustment()
            repository.updatePrayerAdjustment(
                prayer,
                current.copy(offsetMinutes = 0, customHour = null, customMinute = null)
            )
        }
    }

    fun selectThemePreset(preset: DarkThemeColorPreset) {
        viewModelScope.launch {
            repository.updateThemePreset(preset)
        }
    }

    fun setDynamicPrayerColors(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateDynamicPrayerColors(enabled)
        }
    }

    fun selectCity(city: CityLocation) {
        viewModelScope.launch {
            locationMessageFlow.value = null
            repository.updateLocation(city)
        }
    }

    fun detectCurrentLocation() {
        if (locationDetectingFlow.value) return
        viewModelScope.launch {
            locationDetectingFlow.value = true
            locationMessageFlow.value = null
            val detected = LocationHelper.detectCurrentCity(getApplication())
            if (detected != null) {
                repository.updateLocation(detected)
                locationMessageFlow.value = "Updated to ${detected.name}"
            } else {
                locationMessageFlow.value = "Could not detect GPS location. Choose a city below."
            }
            locationDetectingFlow.value = false
        }
    }

    fun clearLocationMessage() {
        locationMessageFlow.value = null
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            repository.updateCalculationMethod(method)
        }
    }

    fun updateAsrMethod(method: AsrJuristicMethod) {
        viewModelScope.launch {
            repository.updateAsrMethod(method)
        }
    }

    fun updateHijriOffset(offsetDays: Int) {
        viewModelScope.launch {
            repository.updateHijriOffset(offsetDays)
        }
    }

    fun set24HourFormat(use24h: Boolean) {
        viewModelScope.launch {
            repository.update24HourFormat(use24h)
        }
    }

    fun setKeepScreenOn(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateKeepScreenOn(enabled)
        }
    }

    fun updateDefaultAzanTone(tone: SoftAzanTone) {
        viewModelScope.launch {
            repository.updateDefaultAzanTone(tone)
        }
    }

    fun updateSoftAzanVolume(volume: Float) {
        viewModelScope.launch {
            repository.updateSoftAzanVolume(volume)
        }
    }

    fun previewSoftAzan(tone: SoftAzanTone, volume: Float, label: String) {
        SoftAzanPlayer.play(
            context = getApplication(),
            tone = tone,
            targetVolume = volume,
            label = label,
            isAlarmStream = false
        )
    }

    fun stopSoftAzan() {
        SoftAzanPlayer.stop()
    }

    fun getAutoCalculatedTime(prayer: PrayerType): Pair<Int, Int> {
        return PrayerTimeCalculator.getCalculatedAutoTime(
            prayerType = prayer,
            nowMillis = _uiState.value.nowMillis,
            settings = _uiState.value.settings
        )
    }

    fun refreshAlarms() {
        PrayerAlarmScheduler.rescheduleAll(getApplication(), _uiState.value.settings)
    }
}
