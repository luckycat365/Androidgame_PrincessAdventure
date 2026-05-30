# Technical Decisions

## Native Android Canvas First

The repository targets Android Studio and a phone emulator, so the game is implemented as a native Android app rather than a browser game. A custom `SurfaceView` was chosen over a heavier game engine to keep the first playable prototype easy to inspect and build.

## Kotlin Without Compose For The Playfield

Application code is Kotlin. The playfield uses direct Canvas rendering because the primary UI is a sprite-based action platformer with continuous animation, physics, and touch controls. Compose can still be added later for menus/settings if those screens grow beyond the current in-game overlays.

## Root Assets Packaged As Android Assets

The existing root `assets/` directory remains the source/design library. The Android module packages it using `sourceSets.main.assets.srcDir("../assets")` so game code can consume the same paths documented in `game_mechanism_v1.md` and `game_design_assets.md`.

This avoids duplicate drawable/raw copies during early iteration. If specific assets need Android resource IDs later, copy or convert only those assets into `app/src/main/res/` with Android-safe filenames.

## Build Tooling

The project uses:

- Android Gradle Plugin `8.10.1`
- Kotlin Gradle Plugin `2.0.21`
- Gradle wrapper `8.11.1`
- Java/JVM toolchain `21`
- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 26`

Kotlin `2.0.21` is used because it is available in the local Gradle cache. A first attempt to resolve newer plugin coordinates was blocked by SSL certificate validation in this environment.

## Gameplay Scope

Level 1 is implemented as a single scrolling platform level using the available background, platform, castle, princess, projectile, teacup, sound, and HUD assets. The level route was expanded from repeated 3,400-unit segments into a doubled `34,000` unit hand-authored path. The first starting platform and final castle platform keep their original shape, while the middle of the route now uses varied platform heights, gaps, climbs, drops, and enemy patrol placements. The level includes:

- touch movement buttons,
- jump and double jump,
- star projectile attack,
- teacup sentry patrols,
- two-point princess health,
- hurt/invulnerability feedback,
- castle win condition,
- score/timer HUD,
- victory and retry overlays.

Future levels can be added by moving the platform blueprint list in `PrincessGameView` into JSON or another external level data format.
