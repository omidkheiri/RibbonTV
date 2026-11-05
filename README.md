# IPTVPlayer

A Kotlin Android TV / Google TV starter application with a fullscreen Media3 HLS player. It uses the public Mux HLS test stream for playback checks; it does not include IPTV provider authentication or subscription access.

## Development environment

- Host: macOS 26.7.1 on Apple Silicon (arm64, M3 Pro).
- Android Studio: Rabbit 2, 2026.2.2 (build `AI-262.10968.63.2622.16730706`), Apple Silicon, installed at `~/Applications/Android Studio.app`.
- Java: bundled JBR 25.0.4 (Android Gradle Plugin requires JDK 17 or newer); no separate JDK installation.
- Android SDK: API 36 revision 2 and API 37.0 revision 2; API 37.2 revision 2 is also installed. `compileSdk` is 37 and `targetSdk` is 36.
- Build Tools: 36.0.0.
- Android SDK Platform-Tools: 37.0.1.
- Android Emulator: 37.2.13.
- Android command-line tools: revision 15859902.
- Android TV system image: API 36, ARM64, revision 4.
- Gradle: 9.8.2 via the project Gradle Wrapper.
- AVD: `IPTV_TV_1080p`, Android TV, API 36, ARM64, 1920x1080, landscape, host GPU enabled.

The selected stable dependency set requires compile SDK 37 or newer, so `compileSdk` is 37 while `targetSdk` remains 36 (Android 16), the latest stable target documented by Android. The API 36 TV emulator verifies runtime compatibility without opting into API 37 behavior changes. The Google Android SDK license was accepted for this installation. `~/.zshrc` configures `ANDROID_HOME`, the SDK tools, and the bundled JBR without duplicating `PATH` entries.

### Application dependency versions

Versions are centrally managed in `gradle/libs.versions.toml`:

| Component | Version |
| --- | --- |
| Android Gradle Plugin | 9.5.0 |
| Compose compiler plugin | 2.2.10 |
| Compose BOM | 2026.09.00 |
| Compose for TV Material | 1.1.0 |
| AndroidX Media3 | 1.11.1 |
| Room | 2.8.5 |
| Retrofit | 3.0.0 |
| OkHttp | 5.5.1 |
| Kotlin Coroutines | 1.11.0 |
| WorkManager | 2.12.0 |
| Hilt | 2.60.1 |
| Lifecycle | 2.11.0 |
| Activity Compose | 1.13.0 |
| KSP | 2.3.13 |
| Coil Compose | 3.4.0 |
| AndroidX Hilt Work | 1.4.0 |

## Architecture

- `MainActivity.kt` hosts the Compose TV screen and obtains its `PlayerViewModel` through Hilt.
- `feature/player/PlayerScreen.kt` displays the fullscreen player, loading/error states, and a focusable remote-control play/pause button.
- `core/player/PlayerViewModel.kt` owns the Media3 ExoPlayer lifecycle and HLS playback state.
- `feature/home/PlaylistViewModel.kt` coordinates playlist URL input, refresh progress, category browsing, and periodic refresh scheduling.
- `data/remote/PlaylistDownloader.kt` downloads HTTP(S) playlists with bounded size and reports byte progress.
- `data/remote/M3uParser.kt` parses standard M3U `#EXTINF` entries, common IPTV metadata, and relative stream URLs.
- `domain/classification/ContentClassifier.kt` classifies only explicit category conventions and leaves uncertain items uncategorized.
- `data/local/` persists playlist metadata and content in Room. Successful refreshes replace the catalog in one database transaction; failed downloads/parses leave the previous catalog intact.
- `feature/refresh/PlaylistRefreshWorker.kt` refreshes the saved playlist periodically when a network is available.
- `domain/model/IptvModels.kt` defines `Channel`, `ChannelGroup`, `Playlist`, `EPGProgram`, `FavoriteChannel`, and `PlaybackState`.
- `data/remote/M3uPlaylistParser.kt` parses channel-list M3U entries and their common TV metadata.
- `domain/repository/ChannelRepository.kt` defines the channel repository boundary.

M3U playlist parsing and HLS playback are separate concerns: the parser reads playlist metadata such as `#EXTINF` and produces channel records. HLS manifests (identified by `#EXT-X-` tags) are not treated as channel playlists; individual channel stream URLs are passed to Media3 for playback.

Room stores the downloaded catalog for offline browsing; playback still requires network access. Retrofit is included for future API integrations but playlist retrieval currently uses OkHttp directly.

## Build and run

From the project directory:

```sh
./gradlew :app:assembleDebug
```

Run the M3U parser unit tests:

```sh
./gradlew :app:testDebugUnitTest
```

The unit tests cover M3U metadata parsing, conservative Live TV / Movies / Series / Videos classification, URL validation, and playlist download progress. For a real import check, enter an authorized HTTP(S) M3U URL in the app, select **Load Playlist**, then browse each category with the TV remote.

Start the Android TV emulator:

```sh
emulator -avd IPTV_TV_1080p
adb wait-for-device
```

Install and launch the app:

```sh
./gradlew :app:installDebug
adb shell am start -a android.intent.action.MAIN \
  -c android.intent.category.LEANBACK_LAUNCHER \
  -n com.iptvplayer.tv/.MainActivity
```

Use the emulator remote controls or keyboard arrows and Enter/Center to focus and activate the play/pause button.

## ADB debugging

```sh
adb devices
adb logcat -s AndroidRuntime ExoPlayerImplInternal
adb shell input keyevent KEYCODE_DPAD_CENTER
```

## Known limitations and next steps

- Playback requires network access to `https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8`.
- Only user-provided playlist URLs and their authorized streams should be used. The application does not bundle provider URLs or content.
- The classifier uses playlist metadata and recognizable group conventions; ambiguous entries intentionally appear under **Uncategorized** for review.
- HTTP URLs are supported as requested, which enables cleartext networking at the app level. Prefer HTTPS whenever the playlist host supports it.
- M3U parsing and classification have JVM unit tests; repository, UI, and player behavior do not yet have automated integration tests.
- This command-line-tools release warns that `sdkmanager` is deprecated and recommends Android CLI for new SDK-management workflows; `sdkmanager --list_installed` remains available for verification.
- Lint retains three intentional warnings: API 36 is the latest stable target documented here, Compose Compiler 2.2.10 is pinned to the compiler bundled with AGP 9.5.0, and the 320×180 TV launcher banner exceeds the generic vector-icon size recommendation.
- Connect the parser to playlist import, then add Room-backed channels and repository synchronization.
- Add playlist management for multiple saved URLs, EPG data, and favorites after the single-playlist flow is validated.
- Add automated UI and player tests before expanding playback behavior.
