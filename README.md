# Pixel 5 Clock • AxionOS

LSPosed test module for the uploaded AxionOS SystemUI build. It targets the large `GeneralClockView` inside `com.android.systemui` and replaces the visual clock with a two-line Pixel 5-style clock using `GoogleSansClock-Regular.ttf` when available.

## Install after build

1. Install the generated APK.
2. Enable **Pixel 5 Clock • Axion** in LSPosed.
3. Scope it only to `com.android.systemui`.
4. Reboot.

This module does not replace `SystemUI.apk`; disabling the LSPosed module restores the original behavior.