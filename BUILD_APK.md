# 📱 How to Build LeafPDF APK

You have **3 options** to get the APK — choose whichever is easiest for you.

---

## ✅ Option 1: GitHub Actions (Easiest — No Setup Needed)

Build the APK in the cloud for free using GitHub Actions.

### Steps:
1. **Create a free GitHub account** at https://github.com if you don't have one
2. **Create a new repository**: Click `+` → `New repository` → Name it `LeafPDF` → Click `Create`
3. **Upload the project files**:
   - Click `uploading an existing file`
   - Drag and drop ALL files from this ZIP (keep folder structure)
   - Click `Commit changes`
4. **Go to Actions tab** in your repository
5. Click **`Build LeafPDF APK`** workflow on the left
6. Click **`Run workflow`** → `Run workflow` (green button)
7. Wait ~3-5 minutes for the build to finish ✅
8. Click the completed build → Scroll down to **Artifacts** → Download **`LeafPDF-debug`**
9. Extract the ZIP → You have `app-debug.apk` 🎉

---

## ✅ Option 2: Android Studio (Local Build)

### Requirements:
- Android Studio (any recent version)
- JDK 11 or higher (bundled with Android Studio)
- ~5 GB disk space for Android SDK

### Steps:
1. Open Android Studio
2. `File` → `Open` → Select this `LeafPDF/` folder
3. Wait for **Gradle Sync** to complete (downloads dependencies)
4. Click `Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
5. Click **`locate`** in the notification to find the APK at:
   ```
   LeafPDF/app/build/outputs/apk/debug/app-debug.apk
   ```

---

## ✅ Option 3: Command Line (Gradle)

### Requirements:
- JDK 11+ installed
- Android SDK installed (ANDROID_HOME set)

```bash
cd LeafPDF/
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

---

## 📲 Installing the APK on your Android device

1. Enable **Install from Unknown Sources**:
   - Settings → Security → Unknown Sources (Android 7 and below)
   - Settings → Apps → Special App Access → Install Unknown Apps (Android 8+)
2. Transfer `app-debug.apk` to your phone (USB, email, WhatsApp, etc.)
3. Tap the APK file → Install → Done!

---

## 🐛 Common Build Issues

| Error | Fix |
|-------|-----|
| `Could not find android-pdf-viewer` | Make sure `settings.gradle` has `maven { url 'https://jitpack.io' }` |
| `SDK location not found` | Set `ANDROID_HOME` or create `local.properties` with `sdk.dir=/path/to/sdk` |
| `Minimum supported Gradle version is X` | Use `./gradlew wrapper --gradle-version=8.2` |
| `Duplicate class` | Already handled with `packagingOptions` in `build.gradle` |
