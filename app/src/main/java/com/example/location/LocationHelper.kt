package com.example.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.os.Build
import com.example.model.CityLocation
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume

object LocationHelper {

    @SuppressLint("MissingPermission")
    suspend fun detectCurrentCity(context: Context): CityLocation? {
        val client = LocationServices.getFusedLocationProviderClient(context)
        val location = suspendCancellableCoroutine<android.location.Location?> { cont ->
            val cts = CancellationTokenSource()
            cont.invokeOnCancellation { cts.cancel() }
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { loc ->
                    if (loc != null) {
                        cont.resume(loc)
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last -> cont.resume(last) }
                            .addOnFailureListener { cont.resume(null) }
                    }
                }
                .addOnFailureListener {
                    client.lastLocation
                        .addOnSuccessListener { last -> cont.resume(last) }
                        .addOnFailureListener { cont.resume(null) }
                }
        } ?: return null

        val (city, country) = reverseGeocode(context, location.latitude, location.longitude)
        return CityLocation(
            name = city,
            country = country,
            latitude = location.latitude,
            longitude = location.longitude,
            timeZoneId = TimeZone.getDefault().id,
            isGps = true
        )
    }

    private suspend fun reverseGeocode(
        context: Context,
        lat: Double,
        lon: Double
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine<List<android.location.Address>?> { cont ->
                    geocoder.getFromLocation(lat, lon, 1) { list ->
                        cont.resume(list)
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lon, 1)
            }
            val first = addresses?.firstOrNull()
            val city = first?.locality
                ?: first?.subAdminArea
                ?: first?.adminArea
                ?: String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
            val country = first?.countryName ?: "Current Location"
            city to country
        }.getOrElse {
            String.format(Locale.US, "%.2f°, %.2f°", lat, lon) to "Current Location"
        }
    }
}
