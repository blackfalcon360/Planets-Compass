# Planets Compass

Where are the Sun, Moon and all the planets right now? A **compass-shaped sky map** that uses your **GPS position** and works **offline**.

- Package: `planetscompass.blackfalcon.jan` — APK: `PlanetsCompass.apk`
- **By: Black Falcon**

## The map
- **Centre = straight overhead**, **rim = horizon**; rings at 30° and 60° altitude.
- Direction letters N, NE, E … around the rim. The dial **turns with your phone**, so the direction you are facing is at the top (gold marker).
- Sun, Moon, Mercury, Venus, Mars, Jupiter, Saturn, Uranus, Neptune are drawn at their azimuth and altitude. **Hollow dim circles** are below the horizon.
- Below the map: a list with each body's azimuth, direction, altitude and distance.

## The slider
- Drag the slider to move **±24 hours** (10-minute steps); **− 1 day / + 1 day** jump by whole days; **Now** goes back to live.

## Offline
- Position: phone **GPS chip** (no internet). The app has **no INTERNET permission**. The last position is saved.
- Planet positions are calculated on the phone (Paul Schlyter's low-precision method, about 1–2 arc-minutes — fine for a sky map). Works for dates from about 1900 to 2100.
- Uses the phone clock and time zone. Azimuth is from **true north** (magnetic declination is applied).

## Notes
- Phone flat → "up" on the dial is the top of the phone. Phone upright → "up" is the way the back of the phone points (hold it up to the sky).
- Keep the phone away from magnets and metal; move it in a figure-8 to calibrate.
- No atmospheric refraction is applied (it matters only very near the horizon). Android 8.0+.

## Build
Push to GitHub — the **Build APK** workflow runs automatically.
Open **Actions → latest run → Artifacts → PlanetsCompass** to download `PlanetsCompass.apk`.
