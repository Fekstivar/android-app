<br />

<p align="center">
    <img src=".github/static/logo.svg" alt="Metiq logo" width="30%" />
</p>

<br />

<p align="center">
  No-nonsense noise app for focus, sleep, study, and relaxation.
</p>

<br />

<p align="center">
    <img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="License: GPL v3">
    <img src="https://github.com/metiq-xyz/android-app/actions/workflows/ci.yml/badge.svg" alt="CI">
</p>

# Metiq

A no-nonsense noise app for focus, sleep, study, and relaxation. Colored noise
(white, pink, brown, grey) with a sleep timer. Designed around two
non-negotiables: simplicity and battery efficiency.

## What it does

- **Colored noise playback** — pre-rendered, seamless loops at -22 LUFS so every
  color sounds equally loud.
- **Sleep timer** — set hours, minutes, and seconds, or pick from your saved
  presets. Audio fades out gracefully when the timer expires.
- **Stays out of your way** — no accounts, no cloud, no tracking, no ads. The
  app runs without an internet connection.
- **Light on your battery** — audio is handled by the hardware mixer via
  Android's `AudioTrack` in static mode, so the app process stays idle while you
  sleep. Background animations pause automatically when battery saver kicks in.
- **Familiar media controls** — lock screen, notification, and Bluetooth headset
  transport buttons all work the way you'd expect.
- **Five languages out of the box** — English, Italian, Spanish, French,
  Portuguese.
- **Free, forever** — Metiq is free and will stay free. If you'd like to support
  the project, you can [buy us a coffee on Ko-fi](https://ko-fi.com/metiq) or
  [sponsor us on GitHub](https://github.com/sponsors/metiq-xyz).

## Where to get it

- **F-Droid**: coming soon
- **Google Play**: coming soon
- **Direct APK**: built and attached to every tagged release on this repository

Metiq targets Android 10 (API 29) and newer, which covers the vast majority of
devices in active use today.

## A few things that aren't in v1

We kept the first release deliberately small. The following are on the roadmap
for future versions:

- Ambient sounds (rain, thunder, sea waves, wind, café, fan, fireplace)
- Sound mixing with user presets
- iOS and web/browser extension clients

## Contributing

If you'd like to file a bug, suggest a feature, or contribute code or
translations, see **[CONTRIBUTING.md](CONTRIBUTING.md)** for setup instructions,
the architecture overview, and the development workflow.

## Credits

Metiq is built on the work of others — Jetpack Compose, Material 3, AndroidX
Media3, the Kotlin standard library, and the Satoshi typeface by Indian Type
Foundry. Full attribution is available inside the app under \_Settings → About →
Open-source licenses.

## License

Metiq is licensed under the [GNU General Public License v3.0 or later](LICENSE).
See `LICENSE` for the full text.
