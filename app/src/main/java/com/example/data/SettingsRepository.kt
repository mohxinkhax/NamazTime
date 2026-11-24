package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppSettings
import com.example.model.AsrJuristicMethod
import com.example.model.CalculationMethod
import com.example.model.CityLocation
import com.example.model.DarkThemeColorPreset
import com.example.model.PrayerAdjustment
import com.example.model.PrayerType
import com.example.model.SoftAzanTone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "namaz_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val cityName = stringPreferencesKey("city_name")
        val cityCountry = stringPreferencesKey("city_country")
        val latitude = doublePreferencesKey("latitude")
        val longitude = doublePreferencesKey("longitude")
        val timeZoneId = stringPreferencesKey("timezone_id")
        val isGps = booleanPreferencesKey("is_gps")
        val calcMethod = stringPreferencesKey("calc_method")
        val asrMethod = stringPreferencesKey("asr_method")
        val hijriOffset = intPreferencesKey("hijri_offset")
        val use24Hour = booleanPreferencesKey("use_24h")
        val keepScreenOn = booleanPreferencesKey("keep_screen_on")
        val themePreset = stringPreferencesKey("theme_preset")
        val dynamicColors = booleanPreferencesKey("dynamic_colors")
        val defaultAzanTone = stringPreferencesKey("default_azan_tone")
        val softAzanVolume = floatPreferencesKey("soft_azan_volume")

        fun alarmEnabled(p: PrayerType) = booleanPreferencesKey("${p.name.lowercase()}_alarm")
        fun offsetMin(p: PrayerType) = intPreferencesKey("${p.name.lowercase()}_offset")
        fun customHour(p: PrayerType) = intPreferencesKey("${p.name.lowercase()}_custom_hour")
        fun customMinute(p: PrayerType) = intPreferencesKey("${p.name.lowercase()}_custom_min")
        fun azanTone(p: PrayerType) = stringPreferencesKey("${p.name.lowercase()}_azan_tone")
        fun reminderBefore(p: PrayerType) = intPreferencesKey("${p.name.lowercase()}_reminder_min")
        fun customVolume(p: PrayerType) = floatPreferencesKey("${p.name.lowercase()}_custom_volume")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val default = AppSettings()
        val location = CityLocation(
            name = prefs[Keys.cityName] ?: default.location.name,
            country = prefs[Keys.cityCountry] ?: default.location.country,
            latitude = prefs[Keys.latitude] ?: default.location.latitude,
            longitude = prefs[Keys.longitude] ?: default.location.longitude,
            timeZoneId = prefs[Keys.timeZoneId] ?: default.location.timeZoneId,
            isGps = prefs[Keys.isGps] ?: false
        )

        val adjustments = PrayerType.entries.associateWith { prayer ->
            val rawHour = prefs[Keys.customHour(prayer)] ?: -1
            val rawMin = prefs[Keys.customMinute(prayer)] ?: -1
            val rawTone = prefs[Keys.azanTone(prayer)]
            val rawVol = prefs[Keys.customVolume(prayer)] ?: -1f
            PrayerAdjustment(
                alarmEnabled = prefs[Keys.alarmEnabled(prayer)] ?: true,
                offsetMinutes = prefs[Keys.offsetMin(prayer)] ?: 0,
                customHour = if (rawHour in 0..23) rawHour else null,
                customMinute = if (rawMin in 0..59) rawMin else null,
                azanTone = if (rawTone != null) SoftAzanTone.fromId(rawTone) else prayer.defaultAzanTone,
                reminderBeforeMinutes = prefs[Keys.reminderBefore(prayer)] ?: 0,
                customVolume = if (rawVol in 0.05f..1.0f) rawVol else null
            )
        }

        AppSettings(
            location = location,
            calculationMethod = prefs[Keys.calcMethod]?.let { name ->
                CalculationMethod.entries.find { it.name == name }
            } ?: default.calculationMethod,
            asrMethod = prefs[Keys.asrMethod]?.let { name ->
                AsrJuristicMethod.entries.find { it.name == name }
            } ?: default.asrMethod,
            hijriOffsetDays = prefs[Keys.hijriOffset] ?: 0,
            use24HourFormat = prefs[Keys.use24Hour] ?: false,
            keepScreenOn = prefs[Keys.keepScreenOn] ?: true,
            themePreset = prefs[Keys.themePreset]?.let { DarkThemeColorPreset.fromId(it) }
                ?: default.themePreset,
            dynamicPrayerColors = prefs[Keys.dynamicColors] ?: true,
            defaultAzanTone = SoftAzanTone.fromId(prefs[Keys.defaultAzanTone])
                ?: default.defaultAzanTone,
            softAzanVolume = prefs[Keys.softAzanVolume] ?: default.softAzanVolume,
            adjustments = adjustments
        )
    }

    suspend fun updateLocation(location: CityLocation) {
        context.dataStore.edit { prefs ->
            prefs[Keys.cityName] = location.name
            prefs[Keys.cityCountry] = location.country
            prefs[Keys.latitude] = location.latitude
            prefs[Keys.longitude] = location.longitude
            prefs[Keys.timeZoneId] = location.timeZoneId
            prefs[Keys.isGps] = location.isGps
        }
    }

    suspend fun updatePrayerAdjustment(prayer: PrayerType, adjustment: PrayerAdjustment) {
        context.dataStore.edit { prefs ->
            prefs[Keys.alarmEnabled(prayer)] = adjustment.alarmEnabled
            prefs[Keys.offsetMin(prayer)] = adjustment.offsetMinutes
            prefs[Keys.customHour(prayer)] = adjustment.customHour ?: -1
            prefs[Keys.customMinute(prayer)] = adjustment.customMinute ?: -1
            val tone = adjustment.azanTone
            if (tone != null) {
                prefs[Keys.azanTone(prayer)] = tone.id
            } else {
                prefs.remove(Keys.azanTone(prayer))
            }
            prefs[Keys.reminderBefore(prayer)] = adjustment.reminderBeforeMinutes
            prefs[Keys.customVolume(prayer)] = adjustment.customVolume ?: -1f
        }
    }

    suspend fun updateThemePreset(preset: DarkThemeColorPreset) {
        context.dataStore.edit { prefs ->
            prefs[Keys.themePreset] = preset.id
        }
    }

    suspend fun updateDynamicPrayerColors(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.dynamicColors] = enabled
        }
    }

    suspend fun updateCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { prefs ->
            prefs[Keys.calcMethod] = method.name
        }
    }

    suspend fun updateAsrMethod(method: AsrJuristicMethod) {
        context.dataStore.edit { prefs ->
            prefs[Keys.asrMethod] = method.name
        }
    }

    suspend fun updateHijriOffset(offsetDays: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.hijriOffset] = offsetDays.coerceIn(-2, 2)
        }
    }

    suspend fun update24HourFormat(use24h: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.use24Hour] = use24h
        }
    }

    suspend fun updateKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.keepScreenOn] = enabled
        }
    }

    suspend fun updateDefaultAzanTone(tone: SoftAzanTone) {
        context.dataStore.edit { prefs ->
            prefs[Keys.defaultAzanTone] = tone.id
        }
    }

    suspend fun updateSoftAzanVolume(volume: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.softAzanVolume] = volume.coerceIn(0.10f, 0.85f)
        }
    }
}
