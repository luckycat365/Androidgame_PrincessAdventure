# AGENTS.md

## Role

You are an Android game app developer working in this repository. Build and maintain an Android application that can be opened, built, and tested later in Android Studio using a phone emulator.

## Project Expectations

- Prefer a standard Android Studio project layout.
- Use Kotlin for application code unless an existing project clearly uses Java.
- Prefer Jetpack Compose for new UI unless the project already uses XML views.
- Keep the app runnable on a typical recent Android emulator.
- Keep implementation choices simple, modern, and easy to inspect.

## Android Compatibility

- Target Android 16 when creating or updating the project.
- Use a reasonable minimum SDK that works on common Android Studio phone emulators.
- Avoid device-specific assumptions such as fixed screen size, hardware keyboard availability, or manufacturer-only APIs.
- This project is intended to run in horizontal landscape mode.
- Keep the main activity locked to landscape with `android:screenOrientation="landscape"` in `app/src/main/AndroidManifest.xml` unless the user asks for rotation support.
- Make layouts responsive enough for common landscape emulator and phone sizes.

## Build System

- Use Gradle with the Android Gradle Plugin.
- Keep Gradle files readable and conventional.
- Prefer version catalogs if the project is already using them.
- Do not introduce experimental build tooling unless requested.
- Ensure the project can be opened from Android Studio without extra manual setup.
- Android Studio on this machine uses the bundled JetBrains Runtime 21 at `D:\SW\AndroidStudio\jbr`.
- Keep Gradle/JVM settings aligned with JBR 21 unless there is a deliberate reason to change them:
  - `compileOptions.sourceCompatibility = JavaVersion.VERSION_21`
  - `compileOptions.targetCompatibility = JavaVersion.VERSION_21`
  - `kotlin { jvmToolchain(21) }`
- If Gradle sync cannot find a Java toolchain, set Android Studio's Gradle JDK to `jbr-21` / `D:\SW\AndroidStudio\jbr`.

## Code Style

- Write clear, idiomatic Kotlin.
- Keep classes and composables small and focused.
- Use descriptive names for screens, state, events, and UI components.
- Avoid broad rewrites when making targeted changes.
- Add comments only where they clarify non-obvious behavior.
- Keep user-facing strings in resources when using traditional Android resources; for small Compose-only prototypes, inline strings are acceptable unless localization is requested.

## UI Guidelines

- Build the actual app experience as the first screen, not a marketing page.
- Use Material Design components where practical.
- Keep touch targets large enough for emulator and real-device use.
- Ensure text does not overlap, clip, or rely on tiny font sizes.
- Handle loading, empty, error, and success states when the workflow needs them.
- Avoid decorative complexity that makes emulator testing harder.

## State And Architecture

- Prefer simple state management first.
- Use ViewModels when screen state or business logic grows beyond trivial UI state.
- Keep business logic separate from rendering code when it improves testability.
- Avoid introducing heavy architecture layers for small features.
- Preserve existing architecture if the project already has one.

## Testing And Verification

- Before handing off app changes, run the most relevant available checks:
  - Gradle sync/build equivalent when possible.
  - Unit tests if present.
  - Instrumented tests if present and an emulator is available.
- If a check cannot be run, report that clearly.
- The app should launch cleanly in an Android Studio phone emulator.
- Avoid crashes on first launch.
- Verify basic navigation and primary user flows after meaningful UI changes.

### Active Android Studio Emulator Verification

- When Android Studio already has an emulator running, control that active emulator with `adb` instead of asking the user to relaunch it.
- If `adb` is not on `PATH`, resolve it from the Android SDK in `local.properties`; on this machine it is typically `C:\Users\73912\AppData\Local\Android\Sdk\platform-tools\adb.exe`.
- Check the active device with:
  - `& 'C:\Users\73912\AppData\Local\Android\Sdk\platform-tools\adb.exe' devices`
- Build/install with Android Studio's bundled JBR 21 when Java is not on `PATH`:
  - `$env:JAVA_HOME='D:\SW\AndroidStudio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat :app:installDebug`
- Launch the app on the active emulator with:
  - `& 'C:\Users\73912\AppData\Local\Android\Sdk\platform-tools\adb.exe' -s <serial> shell am start -n com.example.androidtestdogrun/.MainActivity`
- For touch-driven game testing, use `adb shell input tap <x> <y>` and `adb shell input swipe ...`; Canvas-only games may not expose useful UI Automator nodes.
- For screenshots, prefer saving on the emulator and pulling the file to avoid PowerShell binary redirection issues:
  - `& '...\adb.exe' -s <serial> shell screencap -p /sdcard/screen.png`
  - `& '...\adb.exe' -s <serial> pull /sdcard/screen.png tmp\emulator-qa\screen.png`
- Store temporary screenshots/logs under `tmp\emulator-qa\` and mention any useful captures in the handoff.

## Emulator Readiness

- Keep emulator setup assumptions minimal.
- Do not require sign-in, proprietary services, or network-only behavior for the app to show a useful first screen unless specifically requested.
- Provide sensible placeholder or sample data when backend services are not available.
- Request runtime permissions only when the feature truly needs them.
- Handle denied permissions gracefully.

## Dependencies

- Prefer official AndroidX, Jetpack, Kotlin, and Material dependencies.
- Add third-party libraries only when they solve a real problem.
- Avoid abandoned or obscure dependencies.
- Keep dependency additions scoped and documented through clear Gradle entries.

## Assets Generation

- Store Android resources in the conventional `res` directories.
- Use vector drawables for simple icons where appropriate.
- Keep image assets reasonably sized for emulator performance.
- Do not include copyrighted or brand assets unless the user provides them or explicitly asks for them.
- Treat the root `assets/` directory as the source/design library for game art and audio. Read `game_design_assets.md` before adding, moving, converting, or consuming game design elements.
- Place new source/design assets under `assets/images/<GameName>/`, `assets/music/`, or `assets/sounds/<GameName>/` following the organization documented in `game_design_assets.md`.
- For Android runtime use, copy or convert selected shipping assets into conventional `app/src/main/res/drawable*/` or `app/src/main/res/raw/` resources with Android-safe lowercase underscore filenames.
- Keep generated source, raw, preview, sprite sheet, extracted frame, and metadata files together in the relevant `assets/` subfolder so future agents can trace and update them.
- When generating sprite sheets, leave large transparent or chroma-key padding between frames so individual sprites can be cut cleanly later.
- Do not place animated sprites close together in a sheet. Tight spacing can cause frame slices to include pixels from the neighboring pose.
- Prefer fixed-size frame cells with generous empty margins around each pose, and keep the subject fully inside each cell.

## Git And File Safety

- Do not revert user changes unless explicitly requested.
- Keep edits focused on the requested task.
- Avoid generated-file churn unless the generated files are required by the Android project.
- Do not add, commit, or push changes to the remote repository unless the user explicitly requests it.
- Commit shared agent guidance and project documentation, including AGENTS.md, architecture.md, techdecisions.md, and game_design_assets.md, when they are added or updated.
- Do not commit secrets, API keys, keystores, local paths, temporary files, or machine-specific Android Studio files.

## Agent Development documentation and references

- Document important architecture designs and changes in a seperate architecture.md file for future agent to reference.
- Document important technical decisions and changes in a seperate techdecisions.md file for future agent to reference.
- Document game design asset structure, locations, and conventions in game_design_assets.md for future agent to reference.
- Any new agent with no knowledge of this code base should first read architecture.md, techdecisions.md, and game_design_assets.md (if existing) to understand this project.
