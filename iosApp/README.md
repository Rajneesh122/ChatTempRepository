# iOS App (Art Studio)

## Prerequisites

- macOS with Xcode 15+
- JDK 17+
- Android Studio or command-line Gradle

## Build the Kotlin framework

From the project root:

```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

The framework is generated at:

`shared/build/bin/iosSimulatorArm64/debugFramework/Shared.framework`

## Open in Xcode

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Ensure the **Shared.framework** search path includes the Gradle output above (Debug builds use `FRAMEWORK_SEARCH_PATHS` in the project).
3. Select an iOS Simulator target and run.

## Gradle convenience tasks

```bash
./gradlew buildIosSimulator   # links debug simulator framework
./gradlew buildIosDevice      # links debug device framework
```

## Architecture

- **SwiftUI** UI in `iosApp/iosApp/`
- **Shared Kotlin** business logic from `:shared` (ViewModels, engine core, storage)
- **Metal** canvas rendering via `MetalCanvasRenderer` in `shared/src/iosMain`

## Troubleshooting

### Info.plist / code signing error
The project sets `GENERATE_INFOPLIST_FILE = YES` so Xcode auto-generates Info.plist.

### PhaseScriptExecution failed
The **Build Shared Framework** script runs Gradle. Expand that step in the Report navigator (⌘9) to see the real error.

Common causes:
1. **No Java** — install JDK 17+ (`brew install openjdk@17`) or use Android Studio’s bundled JBR.
2. **Wrong framework output** — use an **iOS Simulator** destination for daily dev, or run both Gradle tasks once:
   ```bash
   ./gradlew buildIosSimulator buildIosDevice
   ```
3. **Script sandbox** — `ENABLE_USER_SCRIPT_SANDBOXING = NO` is set in the Xcode project.

The build script lives at `iosApp/scripts/build-shared-framework.sh` and picks simulator vs device automatically.

If builds still fail, run manually from the project root:
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew buildIosSimulator
```
