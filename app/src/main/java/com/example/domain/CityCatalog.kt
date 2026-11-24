package com.example.domain

import com.example.model.CityLocation

object CityCatalog {
    val presetCities: List<CityLocation> = listOf(
        CityLocation("Southampton", "United Kingdom", 50.9097, -1.4044, "Europe/London"),
        CityLocation("London", "United Kingdom", 51.5074, -0.1278, "Europe/London"),
        CityLocation("Birmingham", "United Kingdom", 52.4862, -1.8904, "Europe/London"),
        CityLocation("Manchester", "United Kingdom", 53.4808, -2.2426, "Europe/London"),
        CityLocation("Makkah", "Saudi Arabia", 21.3891, 39.8579, "Asia/Riyadh"),
        CityLocation("Madinah", "Saudi Arabia", 24.5247, 39.5692, "Asia/Riyadh"),
        CityLocation("Riyadh", "Saudi Arabia", 24.7136, 46.6753, "Asia/Riyadh"),
        CityLocation("Dubai", "UAE", 25.2048, 55.2708, "Asia/Dubai"),
        CityLocation("Karachi", "Pakistan", 24.8607, 67.0011, "Asia/Karachi"),
        CityLocation("Lahore", "Pakistan", 31.5204, 74.3587, "Asia/Karachi"),
        CityLocation("Islamabad", "Pakistan", 33.6844, 73.0479, "Asia/Karachi"),
        CityLocation("Istanbul", "Türkiye", 41.0082, 28.9784, "Europe/Istanbul"),
        CityLocation("Cairo", "Egypt", 30.0444, 31.2357, "Africa/Cairo"),
        CityLocation("Kuala Lumpur", "Malaysia", 3.1390, 101.6869, "Asia/Kuala_Lumpur"),
        CityLocation("Jakarta", "Indonesia", -6.2088, 106.8456, "Asia/Jakarta"),
        CityLocation("Dhaka", "Bangladesh", 23.8103, 90.4125, "Asia/Dhaka"),
        CityLocation("Delhi", "India", 28.7041, 77.1025, "Asia/Kolkata"),
        CityLocation("Mumbai", "India", 19.0760, 72.8777, "Asia/Kolkata"),
        CityLocation("New York", "USA", 40.7128, -74.0060, "America/New_York"),
        CityLocation("Chicago", "USA", 41.8781, -87.6298, "America/Chicago"),
        CityLocation("Los Angeles", "USA", 34.0522, -118.2437, "America/Los_Angeles"),
        CityLocation("Toronto", "Canada", 43.6532, -79.3832, "America/Toronto"),
        CityLocation("Paris", "France", 48.8566, 2.3522, "Europe/Paris"),
        CityLocation("Berlin", "Germany", 52.5200, 13.4050, "Europe/Berlin"),
        CityLocation("Sydney", "Australia", -33.8688, 151.2093, "Australia/Sydney")
    )
}
