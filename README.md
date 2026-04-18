# Peekt

Peekt records HTTP(S) request/response round-trips from OkHttp into a local database (Room) for in-app inspection.

## Features

- 💾 Records each OkHttp request/response round-trip locally (Room).
- 🌊 Observe recorded transactions with Kotlin `Flow`.
- 🔀 Use the recording library in debug builds and the no-op artifact in release—the same public API with no recording overhead in production.
- 🎨 No built-in inspector UI—you build your own screens.
- 🪶 No AppCompat / Jetpack Compose or other UI-framework dependencies.

## Download

### Gradle

**Repositories:** add JitPack (for example in the root `build.gradle`):

```groovy
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

If your project declares repositories only in `settings.gradle`, add JitPack there instead of (or as well as) the example above.

With Kotlin DSL (`settings.gradle.kts`), add the JitPack repository inside your `dependencyResolutionManagement { repositories { ... } }` block:

```kotlin
maven("https://jitpack.io")
```

**Dependencies:** in the app (or library) module, depend on the recording implementation for debug and the no-op for release:

```groovy
dependencies {
    debugImplementation "com.github.nakamuuu.peekt:library:VERSION"
    releaseImplementation "com.github.nakamuuu.peekt:library-no-op:VERSION"
}
```

```kotlin
dependencies {
    debugImplementation("com.github.nakamuuu.peekt:library:VERSION")
    releaseImplementation("com.github.nakamuuu.peekt:library-no-op:VERSION")
}
```

## Installation

1. **Create `Peekt` once** (for example in `Application` or your DI graph), using the same `Context` you use for other app-wide singletons:

   ```kotlin
   val peekt = Peekt.create(
       applicationContext,
       PeektConfig(
           redactHeaderNames = setOf("Authorization"),
       ),
   )
   ```

2. **Register the interceptor** on every `OkHttpClient` that should be recorded (same `Peekt` instance):

   ```kotlin
   val client = OkHttpClient.Builder()
       .addInterceptor(peekt.interceptor())
       .build()
   ```

   A minimal end-to-end example lives in [`sample/src/main/kotlin/net/divlight/peekt/sample/MainActivity.kt`](sample/src/main/kotlin/net/divlight/peekt/sample/MainActivity.kt).

3. **Read recordings** from `peekt.recorder`:
   - `observeTransactions()` — `Flow` of transaction summaries (newest first), suitable for a list UI.
   - `getTransactionMessage(id)` — full headers and bodies for one transaction, when the user opens a detail screen.
   - `clear()` — removes all stored transactions.

See KDoc on `Peekt`, `PeektRecorder`, and `PeektConfig` for full API details.

## Configuration

`PeektConfig` controls what gets stored:

| Property | Description |
| -------- | ----------- |
| `maxContentLength` | Maximum bytes kept per request body preview and maximum bytes read from the response via `Response.peekBody` (default `500_000`). Longer content is truncated with an ellipsis. |
| `redactHeaderNames` | Header names (compared case-insensitively) whose values are replaced with `**` before persistence. Empty by default. |

Pass a `PeektConfig` to `Peekt.create`; omit it to use defaults.

## Building

1. Clone this repository.

   ```bash
   git clone https://github.com/nakamuuu/peekt.git
   cd peekt
   ```

2. Build and test with Gradle. You need a JDK and the Android SDK.

   ```bash
   ./gradlew build
   ```

### Optional: AI editor rules (contributors)

Install npm dependencies, then generate editor rules for AI development tools with [rulesync](https://github.com/dyoshikawa/rulesync). Run the script again whenever the rule sources change.

```bash
npm install
npm run rulesync:generate
```

## Requirements

Android 9 (API 28) or higher

## License

```
Copyright 2026 Keita Nakamura

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
