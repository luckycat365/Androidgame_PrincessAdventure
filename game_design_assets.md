# Game Design Assets

This document records the current `assets/` structure and the conventions future agents should follow when adding or consuming game design elements.

## Current Asset Library

The repository currently has 62 asset files under `assets/`, totaling about 21.2 MB. Assets are grouped first by media type, then by game or feature set.

```text
assets/
  images/
    Starting View.png
    PrincessStarAdventure/
      backgrounds/
      castle/
      enemies/
      platforms/
      princess/
      projectiles/
      ui/
  music/
  sounds/
    PrincessStarAdventure/
```

## Images

`assets/images/Starting View.png` is a standalone starting-screen image.

`assets/images/PrincessStarAdventure/` contains the main visual asset set for the Princess Star Adventure game:

- `backgrounds/Level 1.png`: level background art.
- `castle/castle.png`: castle/environment prop.
- `princess/`: player character animation frames.
- `enemies/teacup-sentry/`: enemy sprite sheets, extracted animation frames, preview/source files, and metadata.
- `platforms/level1/`: Level 1 platform tiles, generated sheets, preview/source files, and metadata.
- `projectiles/star/star-projectile.png`: star projectile sprite.
- `ui/`: mobile touch controls for attack, jump, left, and right, plus HUD icons such as health.

### Princess Character

Player animation frames live in:

- `assets/images/PrincessStarAdventure/princess/standing/`
- `assets/images/PrincessStarAdventure/princess/running/`
- `assets/images/PrincessStarAdventure/princess/jumping/`
- `assets/images/PrincessStarAdventure/princess/attacking/`
- `assets/images/PrincessStarAdventure/princess/hurt/`

Standing, running, jumping, and attacking currently have six numbered PNG frames from `01.png` through `06.png`. Hurt currently has one frame at `01.png`. Keep this naming pattern for new frame-based character animations.

### Teacup Sentry Enemy

The teacup sentry enemy lives in `assets/images/PrincessStarAdventure/enemies/teacup-sentry/`.

Important files:

- `teacup-sentry.json`: metadata describing the enemy, frame size, animation sources, frame directories, and default facing direction.
- `teacup-sentry-walking.png`: walking sprite sheet.
- `teacup-sentry-walking-source.png`: source image for the generated walking animation.
- `teacup-sentry-walking-raw.png`: raw generated image.
- `teacup-sentry-preview.png`: preview image.
- `walking/01.png` through `walking/06.png`: extracted walking frames.
- `hit/01.png`: hit frame.
- `destroyed/01.png`: destroyed frame.

The metadata currently defines 256 x 256 walking frames, six walking frames, and a default `left` facing direction.

### Level 1 Platforms

Level 1 platform assets live in `assets/images/PrincessStarAdventure/platforms/level1/`.

Individual platform images:

- `grass-short.png`
- `grass-long.png`
- `grass-round.png`
- `flower-bridge.png`
- `crystal.png`
- `cloud.png`

Supporting files:

- `fantasy-platforms.json`: metadata listing platform names, image files, and dimensions.
- `fantasy-platforms-sheet.png`: composed sprite sheet.
- `fantasy-platforms-source.png`: source image.
- `fantasy-platforms-raw.png`: raw generated image.
- `fantasy-platforms-preview.png`: preview image.

When adding platform pieces, update `fantasy-platforms.json` with the display name, image filename, width, and height.

## UI And HUD

UI assets live in `assets/images/PrincessStarAdventure/ui/`.

Current UI assets include:

- `mobile-attack-button.png`
- `mobile-jump-button.png`
- `mobile-left-button.png`
- `mobile-right-button.png`
- `heart-health.png`: a 256 x 256 transparent heart icon for princess health.

## Audio

Music tracks live directly under `assets/music/`:

- `ChasingLight.mp3`

Princess Star Adventure sound effects live under `assets/sounds/PrincessStarAdventure/`:

- `princess/hurt.wav`
- `princess/princess double jump.wav`
- `princess/star-attack.wav`
- `enemies/teacup/teacup-crash.wav`

Sound effects are organized by owner or gameplay source. Put princess/player sounds under `princess/`, enemy sounds under `enemies/<enemy-name>/`, and keep filenames descriptive of the action they represent.

## Where To Put New Assets

Use this structure for new game design elements:

- Character art: `assets/images/<GameName>/<character-name>/<animation-name>/NN.png`
- Enemy art: `assets/images/<GameName>/enemies/<enemy-name>/`
- Platforms/tiles: `assets/images/<GameName>/platforms/<theme-name>/`
- Backgrounds: `assets/images/<GameName>/backgrounds/`
- Props/environment pieces: a named folder under `assets/images/<GameName>/`, such as `castle/`
- Projectiles: `assets/images/<GameName>/projectiles/<projectile-name>/`
- UI controls and HUD art: `assets/images/<GameName>/ui/`
- Music: `assets/music/`
- Sound effects: `assets/sounds/<GameName>/`

Prefer descriptive lowercase names with hyphens for new folders and files. Existing names with spaces should be preserved unless there is a specific migration task.

## How To Use Assets In The Android App

Keep this root `assets/` directory as the source/design library. The current Android app also packages this folder directly through `app/build.gradle.kts` with `assets.srcDir("../assets")`, so game code can load these files by their documented relative paths.

For Android runtime resources that need generated resource IDs, copy or convert selected shipping assets into conventional Android locations:

- Put image assets used by Compose or Android resources under `app/src/main/res/drawable/` or density-specific drawable folders when appropriate.
- Put music and sound effects that should ship with the app under `app/src/main/res/raw/`.
- Keep source, raw, preview, and generation files in the root `assets/` tree unless the app needs them at runtime.

When adding runtime copies to `res/`, keep filenames Android-resource-safe: lowercase letters, numbers, and underscores only. For example, `star-attack.wav` should become something like `star_attack.wav` in `res/raw/`.

## Generation And Editing Notes

- Keep generated sprite frames in fixed-size cells with generous transparent padding.
- Do not place animation poses tightly next to one another in source sheets.
- Keep extracted frames numbered consistently as `01.png`, `02.png`, and so on.
- Add or update JSON metadata when a folder contains sprite sheets, generated variants, frame dimensions, or named platform pieces.
- Keep preview/source/raw files beside the generated output so future agents can trace how an asset was made.
- Avoid copyrighted or branded material unless the user provides it or explicitly asks for it.
