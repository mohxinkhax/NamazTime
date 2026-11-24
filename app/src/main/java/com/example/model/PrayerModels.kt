package com.example.model

import androidx.annotation.RawRes
import androidx.compose.ui.graphics.Color
import com.example.R
import com.example.ui.theme.AsrAmberCopper
import com.example.ui.theme.DhuhrSkyCyan
import com.example.ui.theme.FajrDawnGold
import com.example.ui.theme.IshaEmeraldNight
import com.example.ui.theme.MaghribSunsetRose

enum class PrayerType(
    val displayName: String,
    val uppercaseName: String,
    val defaultColor: Color,
    val defaultAzanTone: SoftAzanTone
) {
    FAJR("Fajr", "FAJR", FajrDawnGold, SoftAzanTone.DAWN_WHISPER),
    DHUHR("Dhuhr", "DHUHR", DhuhrSkyCyan, SoftAzanTone.SOFT_HIJAZ),
    ASR("Asr", "ASR", AsrAmberCopper, SoftAzanTone.GENTLE_RAST),
    MAGHRIB("Maghrib", "MAGHRIB", MaghribSunsetRose, SoftAzanTone.SOFT_HIJAZ),
    ISHA("Isha", "ISHA", IshaEmeraldNight, SoftAzanTone.GENTLE_RAST)
}

enum class SoftAzanTone(
    val id: String,
    val label: String,
    val subtitle: String,
    @RawRes val rawResId: Int
) {
    SOFT_HIJAZ(
        id = "soft_hijaz",
        label = "Soft Madinah (Hijaz)",
        subtitle = "Warm gentle Maqam Hijaz cadence",
        rawResId = R.raw.soft_azan_hijaz
    ),
    GENTLE_RAST(
        id = "gentle_rast",
        label = "Gentle Makkah (Rast)",
        subtitle = "Serene uplifting Maqam Rast motif",
        rawResId = R.raw.soft_azan_rast
    ),
    DAWN_WHISPER(
        id = "dawn_whisper",
        label = "Dawn Whisper (Fajr)",
        subtitle = "Low-register calm morning tone",
        rawResId = R.raw.soft_azan_fajr
    );

    companion object {
        fun fromId(id: String?): SoftAzanTone? =
            entries.find { it.id == id }
    }
}

enum class CalculationMethod(
    val label: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null
) {
    MWL("Muslim World League", 18.0, 17.0),
    ISNA("ISNA (North America)", 15.0, 15.0),
    EGYPT("Egyptian General Authority", 19.5, 17.5),
    MAKKAH("Umm Al-Qura (Makkah)", 18.5, 0.0, 90),
    KARACHI("Karachi (Univ. of Islamic Sciences)", 18.0, 18.0),
    LONDON_UNIFIED("UK / Europe Unified", 18.0, 17.0)
}

enum class AsrJuristicMethod(
    val label: String,
    val shadowFactor: Int
) {
    STANDARD("Standard (Shafi'i / Maliki / Hanbali)", 1),
    HANAFI("Hanafi", 2)
}

enum class DarkThemeColorPreset(
    val id: String,
    val label: String,
    val accentHex: Long,
    val secondaryHex: Long,
    val backgroundHex: Long,
    val surfaceHex: Long,
    val surfaceVariantHex: Long
) {
    OLED_GOLD(
        id = "oled_gold",
        label = "AMOLED Pure Black",
        accentHex = 0xFFF5C542,
        secondaryHex = 0xFF38BDF8,
        backgroundHex = 0xFF000000,
        surfaceHex = 0xFF0A0D14,
        surfaceVariantHex = 0xFF131926
    ),
    ROYAL_SAPPHIRE(
        id = "royal_sapphire",
        label = "Midnight Sapphire",
        accentHex = 0xFFFACC15,
        secondaryHex = 0xFF60A5FA,
        backgroundHex = 0xFF030816,
        surfaceHex = 0xFF0A142D,
        surfaceVariantHex = 0xFF122146
    ),
    MIDNIGHT_EMERALD(
        id = "midnight_emerald",
        label = "Midnight Emerald",
        accentHex = 0xFF34D399,
        secondaryHex = 0xFFFBBF24,
        backgroundHex = 0xFF020807,
        surfaceHex = 0xFF081412,
        surfaceVariantHex = 0xFF102420
    ),
    OBSIDIAN_AMBER(
        id = "obsidian_amber",
        label = "Obsidian Amber",
        accentHex = 0xFFFB923C,
        secondaryHex = 0xFFFDE047,
        backgroundHex = 0xFF050404,
        surfaceHex = 0xFF120E0C,
        surfaceVariantHex = 0xFF1F1713
    ),
    SACRED_TEAL(
        id = "sacred_teal",
        label = "Sacred Teal",
        accentHex = 0xFF2DD4BF,
        secondaryHex = 0xFF93C5FD,
        backgroundHex = 0xFF020709,
        surfaceHex = 0xFF081418,
        surfaceVariantHex = 0xFF102229
    ),
    TWILIGHT_ROSE(
        id = "twilight_rose",
        label = "Twilight Rose",
        accentHex = 0xFFF472B6,
        secondaryHex = 0xFFFBBF24,
        backgroundHex = 0xFF060309,
        surfaceHex = 0xFF120B18,
        surfaceVariantHex = 0xFF1E1329
    );

    companion object {
        fun fromId(id: String): DarkThemeColorPreset =
            entries.find { it.id == id } ?: OLED_GOLD
    }
}

data class PrayerAdjustment(
    val alarmEnabled: Boolean = true,
    val offsetMinutes: Int = 0,
    val customHour: Int? = null,
    val customMinute: Int? = null,
    val azanTone: SoftAzanTone? = null, // null = use global default soft Azan tone
    val reminderBeforeMinutes: Int = 0, // 0, 5, 10, or 15 min lead before prayer time
    val customVolume: Float? = null // null = use global soft Azan volume
) {
    val isCustomFixedTime: Boolean
        get() = customHour != null && customMinute != null
}

data class CityLocation(
    val name: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneId: String,
    val isGps: Boolean = false
)

data class PrayerTimeEntry(
    val type: PrayerType,
    val epochMillis: Long,
    val formattedTime: String,
    val isCurrent: Boolean,
    val isNext: Boolean,
    val isCustomTime: Boolean,
    val offsetMinutes: Int,
    val alarmEnabled: Boolean,
    val effectiveTone: SoftAzanTone,
    val reminderBeforeMinutes: Int,
    val buttonColor: Color
)

data class CountdownParts(
    val hours: String,
    val minutes: String,
    val seconds: String
)

data class AppSettings(
    val location: CityLocation = CityLocation(
        name = "Southampton",
        country = "United Kingdom",
        latitude = 50.9097,
        longitude = -1.4044,
        timeZoneId = "Europe/London",
        isGps = false
    ),
    val calculationMethod: CalculationMethod = CalculationMethod.LONDON_UNIFIED,
    val asrMethod: AsrJuristicMethod = AsrJuristicMethod.STANDARD,
    val hijriOffsetDays: Int = 0,
    val use24HourFormat: Boolean = false,
    val keepScreenOn: Boolean = true,
    val themePreset: DarkThemeColorPreset = DarkThemeColorPreset.OLED_GOLD,
    val dynamicPrayerColors: Boolean = true,
    val defaultAzanTone: SoftAzanTone = SoftAzanTone.SOFT_HIJAZ,
    val softAzanVolume: Float = 0.45f,
    val adjustments: Map<PrayerType, PrayerAdjustment> = PrayerType.entries.associateWith { prayer ->
        PrayerAdjustment(
            azanTone = prayer.defaultAzanTone
        )
    }
)
