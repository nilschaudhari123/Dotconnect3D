# Color Path 3D

Original 3D puzzle game for Android. Connect matching nodes across a raised grid without crossing another path.

## Build

Open the project in Android Studio (JDK 17, Android SDK 36) or run:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:bundleRelease
```

Application id defaults to `com.naampath.colorpath3d` and can be changed with `colorpath.applicationId` in `gradle.properties`.

Release signing reads `keystore.properties` when that file exists. Without it, the release bundle is signed with the debug key so the task still builds. Replace that before uploading to Google Play. See `keystore.properties.example`.

## Configure services

Test AdMob ids live in `app/src/main/res/values/strings.xml`. Swap those strings for production ids. Do not change gameplay code.

`app/google-services.json` is a placeholder. Replace it with the file from your Firebase project. Remote Config defaults are in `app/src/main/res/xml/remote_config_defaults.xml`.

Google Play Games leaderboards stay disabled until `play_games_app_id` and the leaderboard id strings are replaced with real ids. Local leaderboards work offline either way.

## Play listing

Store images are in `play/`. Privacy and data-safety notes are in `play/privacy-policy.md` and `play/data-safety.md`.
