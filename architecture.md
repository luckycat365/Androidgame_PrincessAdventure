# Architecture

Princess Star Adventure is a native Android game implemented as a simple custom Canvas game view.

## Project Layout

- `app/`: Android application module.
- `app/src/main/java/com/example/princessadventure/MainActivity.kt`: fullscreen landscape activity that hosts the game view.
- `app/src/main/java/com/example/princessadventure/PrincessGameView.kt`: game loop, rendering, input, physics, enemy behavior, score, HUD, win/loss overlays.
- `app/src/main/java/com/example/princessadventure/GameAssets.kt`: bitmap, animation, sound effect, and music loading from packaged assets.
- `assets/`: source/design asset library and packaged runtime asset source. The app module includes this folder as an Android assets source directory.
- `game_mechanism_v1.md`: gameplay design source.
- `game_design_assets.md`: asset inventory and organization guide.

## Runtime Model

`PrincessGameView` is a `SurfaceView` with a dedicated loop thread. Each frame:

1. Calculates delta time.
2. Updates player input, gravity, platform collision, projectiles, teacup enemies, health, score, timer, and win/loss state.
3. Draws the scrolling level world.
4. Draws HUD hearts, score, elapsed time, and touch controls.
5. Draws a win or lose overlay when the level ends.

World simulation uses a fixed logical height of `720` units and scales to the device's landscape screen height. The camera follows the princess horizontally through a wider `3400` unit level.

## Gameplay Systems

- Princess movement supports left/right movement, single jump, double jump, wand attack, hurt state, and temporary invulnerability.
- Magic star projectiles damage teacup sentries.
- Teacup sentries patrol their assigned platform and require two hits to destroy.
- The castle is the Level 1 goal.
- Princess starts with two health points. Hearts are drawn from `assets/images/PrincessStarAdventure/ui/heart-health.png`.
- Level score is enemy destruction score plus time bonus. The time bonus is based on remaining time from a five-minute limit.

## Asset Loading

The app does not duplicate runtime art into `res/drawable`. Instead, `app/build.gradle.kts` includes the root `assets/` folder as an Android asset source:

```kotlin
sourceSets {
    getByName("main") {
        assets.srcDir("../assets")
    }
}
```

This keeps generated/source game assets in one place while still packaging them into the APK. `GameAssets` loads images and audio by relative asset path, for example `images/PrincessStarAdventure/princess/standing/01.png`.

## Android Entry Point

`MainActivity` locks the app into fullscreen immersive landscape through `AndroidManifest.xml` and runtime system UI hiding. It pauses/resumes the game view and background music with the activity lifecycle.
