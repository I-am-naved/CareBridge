# CareBridge final QA status

## Automated checks completed in this environment

- Parsed all 62 XML resource/manifest files successfully.
- Checked Java `R.id` references against IDs declared in layouts: no missing IDs found.
- Checked Java `R.layout` references: only Android framework layouts (`simple_spinner_item` and `simple_spinner_dropdown_item`) were excluded from app resource checks.
- Checked manifest activity declarations against Java activity classes: all declared activities have matching classes.
- Checked source and Gradle configuration for Firebase Cloud Messaging references: none remain.

## Build verification limitation

`./gradlew assembleDebug --no-daemon` could not start because the Gradle wrapper needs to download Gradle 9.6.0 from `services.gradle.org`, and this environment has no DNS/network access. Therefore this is not a successful Gradle build and has not been verified on an emulator or physical device.

## Required checks in Android Studio

1. Open the project and run **Sync Project with Gradle Files**.
2. Run **Build > Make Project** and resolve any dependency or compile errors.
3. Run on a device/emulator and test with separate admin and patient accounts.
4. Verify the Realtime Database security rules before using real patient data.
5. Restrict/rotate the Google Maps API key that was included in the uploaded project. Restrict it to the app package/signing certificate and only the APIs used.

## No-FCM notification limitation

The app uses a Realtime Database listener and local Android notifications while the relevant activity is active. It cannot reliably deliver notifications while the app is closed or force-stopped. Pending offers are loaded when the patient opens the appointments screen.

## Functional test sequence

1. Admin signs in and selects an existing patient and available doctor.
2. Admin creates an offer with at least one slot; status must remain `PENDING`.
3. Patient signs in and opens Appointments; the offer must be visible.
4. Patient selects a slot and confirms; exactly one appointment should be saved.
5. Admin tracker should show `CONFIRMED` and the selected slot.
6. Test Home, tasks, journey, profile, map/back navigation, nearby search, speech recognition and text-to-speech individually.
