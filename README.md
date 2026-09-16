# Art Studio (LearningProject01)

Kotlin Multiplatform digital art app for **Android** and **iOS**.

## Modules

| Module | Description |
|--------|-------------|
| `shared` | KMP library — models, engine, storage, ViewModels |
| `app` | Android app (Jetpack Compose) |
| `iosApp` | iOS app (SwiftUI + Xcode) |

## Android

```bash
./gradlew :app:assembleDebug
```

Open in Android Studio and run on an emulator or device.

## iOS

```bash
./gradlew buildIosSimulator
open iosApp/iosApp.xcodeproj
```

See [iosApp/README.md](iosApp/README.md) for full Xcode setup.

## Shared architecture

- **commonMain**: `ArtEngineCore`, `ProjectRepository`, tile storage, ViewModels
- **androidMain**: OpenGL renderer (`DrawingSurface`), Android export (`MediaCodec`, `PdfDocument`)
- **iosMain**: Metal renderer, UIKit export, AVFoundation video stub

## Project format

Projects are stored as `.lpdoc` folders with LZ4-compressed RGBA tiles — compatible across Android and iOS.
