# 💧 WaterPet

An Android app where your favourite pet peeks out from the screen edge while you use
other apps and reminds you to drink water.

## Features
1. **Overlay permission** + preview button that shows the pet over other apps
2. **Slide-in / peek animation** (pet peeks, wiggles, then slides out with a speech bubble)
3. **Scheduling with a foreground service** (restarts after reboot)
4. **Speech bubble** with **"Drank! 💧"** and **"Later"** (10 min snooze) actions
5. **Pet selection, pet name, daily goal tracking, reminder interval, active hours**

Reminders are skipped when the screen is off, outside active hours, during calls,
and in Do Not Disturb.

## Get the APK (no Android Studio needed)
1. Create a new GitHub repo and push this folder to it.
2. Open the **Actions** tab → **Build APK** → wait for it to finish (~3–5 min).
3. Download the **WaterPet-debug-apk** artifact, unzip, and install the `.apk` on your phone.
4. Optional: push a tag like `v1.0` and the APK is attached to a GitHub Release.

## Build locally
Open the folder in Android Studio (Hedgehog or newer) and press Run,
or run `gradle assembleDebug` (Gradle 8.7, JDK 17).

## First run on the phone
Allow notifications → tap **Grant overlay permission** → enable it for WaterPet →
tap **Start reminders**. On Xiaomi/Oppo/Vivo/Samsung, also disable battery optimization.

## Project layout
- `PetOverlay.kt` – overlay window, animation, speech bubble
- `ReminderService.kt` – foreground service + scheduling + skip rules
- `MainActivity.kt` – setup screen
- `Prefs.kt` – settings and daily water counter
- `BootReceiver.kt` – restart after reboot
