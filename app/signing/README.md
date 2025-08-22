# Android Release Build Signing

This document defines how to configure signing and build the **edX Android app** in release mode.

---

## 📌 Overview

* Introduces `keystore.properties` for secure signing configuration.
* Ensures the release build process can be executed via **Gradle CLI** or **Android Studio**.
* `signing.gradle` automatically picks up values from `keystore.properties` during the build process.
* Keeps sensitive signing information **local only** (not committed to Git).

---

## 🛠️ Setup Instructions

### 1. Create `keystore.properties`

Create a new file at:

```
openedx-app-android/app/signing/keystore.properties
```

Add the following content (replace placeholders with actual values):
RELEASE_KEY_ALIAS
```properties
RELEASE_STORE_FILE=<path_to_your_keystore_file>
RELEASE_STORE_PASSWORD=<store_password>
RELEASE_KEY_ALIAS=<release_key_alias>
RELEASE_KEY_PASSWORD=<key_password>
```

⚠️ **Important Notes**

* This file is **ignored by Git** (`.gitignore`).
* Do **not** commit or publish its contents.
* Keep the keystore file and credentials secure.
* If your password contains special characters (e.g., $, !, #, /, \), use the appropriate escape specifier or string formater in the keystore.properties file to avoid issues with string formatting.

---

### 2. Build the Release APK

#### Option A: Using Gradle CLI

```bash
./gradlew clean assembleProdRelease
```

#### Option B: Using Android Studio

1. Navigate to **Build > Generate Signed Bundle / APK...**
2. Select **APK**.
3. Provide the path to your `*.keystore` file.
4. Complete the wizard to generate a signed APK.

---

### 3. Output Location

The signed APK will be generated at:

```
app/build/outputs/apk/prod/release/
```

---

## 📖 Reference

* [Android App Signing - Official Documentation](https://developer.android.com/studio/publish/app-signing#gradle-signing)
