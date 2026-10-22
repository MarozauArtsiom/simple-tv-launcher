# Weather widget

This is the launcher's first network feature. It brings in the `INTERNET` permission. Per
the project principles, it only touches the network while the home screen is visible.

## Behaviour

- **Top bar chip:** a weather icon, the current temperature and the city (`☁ +7° · Минск`).
  The chip is focusable. OK opens the weather panel.
- **Weather panel** (a side sheet on the right): city and location mode, current conditions,
  feels-like temperature and wind, the next 12 h in 3 h steps, 5 days, a "Change city" button,
  and the attribution "Данные: Open-Meteo.com".
- **Location**, stored in DataStore:
  - `Auto` (default): IP geolocation. A TV has no GPS, and this way there's no permission
    dialog. Re-detect at most once a day.
  - `Manual(city)`: chosen in "Change city" through a search field (Gboard) and a list of
    results. Auto stays one press away.
  - If IP geolocation fails or returns nothing, ask the user to pick a city. Never guess.
- Units: °C, wind in m/s, Russian text.

## Data sources (free, no API key)

Forecast: Open-Meteo (non-commercial, < 10,000 calls a day, data under CC BY 4.0, so the
attribution must be visible):
```
https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}
  &current=temperature_2m,apparent_temperature,weather_code,is_day,wind_speed_10m
  &hourly=temperature_2m,weather_code&forecast_hours=12
  &daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=5
  &timezone=auto&wind_speed_unit=ms
```

City search (and Russian names): Open-Meteo Geocoding:
```
https://geocoding-api.open-meteo.com/v1/search?name={query}&count=8&language=ru&format=json
```
Fields used: `name`, `admin1`, `country`, `latitude`, `longitude`. There is no reverse geocoding,
so for Auto mode take the city name from the IP lookup, then search it with `language=ru` and
pick the result nearest to the IP coordinates. That gives a Russian name and better coordinates.

IP geolocation: a keyless HTTPS service returning city + lat/lon (for example `ipapi.co/json`,
with `ipinfo.io/json` as a fallback). Check both are still keyless and HTTPS when implementing,
and note that a VPN on the TV makes Auto show the VPN's city.

Weather codes (WMO) → icon and text: 0 clear; 1–3 mainly clear / partly cloudy / overcast;
45, 48 fog; 51–57 drizzle; 61–67 rain; 71–77 snow; 80–82 showers; 85–86 snow showers;
95–99 thunderstorm. Use the night icon variant when `is_day == 0`.

## Refresh policy (resources principle)

- Show the cached result instantly. Cache it in DataStore with a timestamp.
- Fetch only while the home screen is `STARTED`:
  `repeatOnLifecycle(STARTED) { while (true) { if (stale(30.min)) fetch(); delay(30.min) } }`.
  Nothing runs when another app is in front: no WorkManager, no alarms, no service.
- On failure, keep the cached data. Hide the temperature (show only the icon and "—°") once
  the cache is more than 6 h old.
- HTTP: `HttpURLConnection` with 10 s timeouts on `Dispatchers.IO`, plus
  `kotlinx.serialization` for JSON. No OkHttp or Retrofit, to keep the APK small. A response
  is a few KB.

## Manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />
```
No location permission. Auto mode uses IP geolocation.
