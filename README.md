# FoodShare UM

An Android application that helps the Universiti Malaya community share surplus food, discover nearby listings, and coordinate collection—reducing food waste while making food more accessible.

![Android](https://img.shields.io/badge/Android-API%2024%2B-3DDC84?logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-Android-orange?logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?logo=firebase&logoColor=black)

## Key Features

- Register and sign in using email/password or Google Sign-In
- Publish food listings with an image, quantity, dietary information, pickup time, and location
- Browse, search, filter, and sort available food listings
- View nearby listings on an interactive map
- Reserve available portions with stock-aware reservation handling
- Generate and scan QR codes to support food collection
- Track reservation details and status
- Receive in-app and push-notification support for important updates
- Manage user profiles, reviews, and verification information
- Contact food providers through WhatsApp when contact details are available

## Technology Stack

| Area | Technologies |
|---|---|
| Language | Java |
| User interface | Android Views, AndroidX, Material Components |
| Backend services | Firebase Authentication, Cloud Firestore, Cloud Storage, Cloud Messaging, Analytics |
| Maps and location | OpenStreetMap via OSMDroid, Google Play Services Location |
| QR codes | ZXing, JourneyApps Barcode Scanner |
| Image loading | Glide |
| Build system | Gradle with Kotlin DSL |

## Requirements

- Android Studio
- JDK 17 or newer
- Android SDK 36
- An emulator or physical device running Android 7.0 (API 24) or newer
- Internet access for Firebase-backed features

## Getting Started

1. Clone the repository:

   ```bash
   git clone https://github.com/sajeev1003/FoodShare-UM.git
   cd FoodShare-UM
   ```

2. Open the project root folder in Android Studio.
3. Allow Android Studio to complete the Gradle sync.
4. Select an emulator or connected Android device.
5. Run the `app` configuration.

## Firebase Configuration

The Android client configuration used for the demonstrated version is included in the project. To connect a separate Firebase project:

1. Create a Firebase project and register an Android app with the package name `com.example.foodshare`.
2. Enable the required sign-in providers in Firebase Authentication.
3. Configure Cloud Firestore, Cloud Storage, and Cloud Messaging.
4. Register the appropriate signing certificate fingerprints for Google Sign-In.
5. Replace `app/google-services.json` with the configuration downloaded from your Firebase project.

Never commit Firebase Admin SDK service-account keys or other server-side credentials.

## Build from the Command Line

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

On macOS or Linux:

```bash
./gradlew assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

## Project Structure

```text
FoodShare-UM/
├── app/
│   ├── src/main/java/com/example/foodshare/   # Activities, fragments, models, and services
│   ├── src/main/res/                          # Layouts, drawables, menus, and values
│   ├── src/main/AndroidManifest.xml           # Application components and permissions
│   └── build.gradle.kts                       # App dependencies and Android configuration
├── gradle/                                    # Gradle wrapper and version catalog
├── build.gradle.kts                           # Project-level build configuration
└── settings.gradle.kts                        # Project and module configuration
```

## Main Data Collections

The application uses Cloud Firestore collections for:

- `users`
- `foods`
- `reservations`
- `reviews`
- user notification data

## Permissions

Depending on the feature and Android version, the app may request access to the camera, location, notifications, and device media. These permissions support QR scanning, map-based discovery, alerts, and food-image selection.

## Author

Developed by [Sajeev](https://github.com/sajeev1003).
