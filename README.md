# dumbhome

`dumbhome` is an Android launcher/home screen built for a flip phone with physical keyboard controls.

## Package ID

- `com.beackers.dumbhome`

## Implemented features

- HOME activity (`MainActivity`) that can be set as the default launcher.
- Keyboard-first operation for flip phones:
  - `ENTER` opens app launcher.
  - Shortcut keys for `F11`, `Menu`, `Up`, `Down`, `Left`, `Right`.
- First-run default key mapping:
  - `F11` → notifications panel
  - `Down` → `com.android.settings`
- Built-in non-SAF wallpaper file picker (`FilePickerActivity`) browsing filesystem image files.
- Live wallpaper selection shortcut.
- In-app custom notification shade powered by a `NotificationListenerService`.
- Keyboard-focus-friendly settings list in `SettingsActivity`.

## Notes

- Targets Android 12 / API 31 (suitable for Android 12 Go devices).
- Notification content in the custom shade requires enabling notification access for DumbHome.
- Wallpaper file browsing uses legacy external storage access APIs and `READ_EXTERNAL_STORAGE`.
