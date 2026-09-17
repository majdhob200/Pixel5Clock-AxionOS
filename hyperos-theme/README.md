# Pixel Stacked Clock for HyperOS 3

A minimal Xiaomi MAML lockscreen theme that recreates the large two-line Android 12 / Pixel-style clock:

```text
09
02
```

## Target
- HyperOS 3
- uiVersion: 17
- miuiAdapterVersion: 5.0
- Designed for modern full-screen Xiaomi phones, including Xiaomi 17T Pro-class displays
- No root required for the theme itself

## What it includes
- Large stacked hour/minute clock
- 12/24-hour mode follows the system setting
- Localized date above the clock
- Battery percentage near the bottom
- System fingerprint overlay is left enabled
- Optional wallpaper picker in lockscreen theme settings
- Uses the built-in `mitype-clock` family. No Google Sans / proprietary font file is bundled.

## Build on Linux

```bash
chmod +x build.sh
./build.sh
```

Output:

```text
dist/PixelStackedClock-HyperOS3-v0.1.mtz
```

## Install / test
Import or apply the generated MTZ through Xiaomi Themes / Theme Editor, then apply only the Lock screen style first.

HyperOS Global/EEA can restrict local third-party MTZ importing depending on Theme Manager version/region/account. That restriction is separate from the theme code itself.

## Notes
The first version intentionally targets the lockscreen only. AOD is not bundled yet so it cannot interfere with Xiaomi's system AOD/fingerprint behavior during the first test.

## References
The MAML structure, `DateTime`, `#time_format`, `mitype-clock`, `ImagePicker`, screen-relative sizing, and `Unlocker` are based on Xiaomi Theme Designer documentation.
