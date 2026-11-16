# CareBridge - Final integration update

This project keeps the existing Android Java/XML architecture and Firebase Realtime Database.
Firebase Cloud Messaging has been removed from the app dependencies and manifest because this version does not use FCM.

## What was corrected

- Admin appointment offers are saved as `PENDING` under both:
  - `/appointment_offers/{offerId}`
  - `/users/{patientUserId}/appointment_offers/{offerId}`
- Admin no longer creates a confirmed appointment when sending an offer.
- Admin verifies that the target patient exists before sending.
- Admin doctor spinner now maps the selected row to the correct available doctor.
- Patient appointment dialog reads offers from the patient's own database node and renders real offers.
- Patient confirmation uses a Firebase transaction to prevent two devices from booking the same pending offer.
- Confirmed appointment is saved using the offer ID as a stable key, helping prevent duplicate appointment records.
- Confirmation is mirrored to the admin offer tracker and added to the patient's journey.
- Appointment dialog removes Firebase listeners when dismissed.
- Care Tasks now have their own add-task form and save to `/users/{userId}/tasks`.
- Home listens to the patient's offers and displays a local notification for newly-created offers while Home is active.
- Removed FCM service, unused foreground notification service, FCM manifest service declaration, and Firebase Messaging dependency.
- Added password-reset email handling and profile-name editing.
- Journey and Home Firebase listeners are removed when their activities are destroyed.

## Important notification limitation (no FCM)

Without FCM or another push service, Android cannot reliably wake a closed or force-stopped app just because Realtime Database changed.

This version:
- Detects new offers and can display a local notification while the patient app is running with the Home activity active.
- Always loads pending offers from Firebase when the patient opens Appointments.
- Does not promise delivery while the app is closed, force-stopped, or the device is offline.

If reliable closed-app push notifications become a requirement later, FCM or another push delivery service will be needed.

## Database structure used

```text
users/
  {patientUserId}/
    appointments/
      {offerId}/
    appointment_offers/
      {offerId}/
    journey/
      {journeyItemId}/
    tasks/
      {taskId}/

appointment_offers/
  {offerId}/

doctors/
  {doctorId}/
```

## Appointment status flow

- `PENDING`: admin sent the offer; patient has not selected a slot.
- `CONFIRMED`: patient successfully selected an available slot.
- `DECLINED` and `EXPIRED` are reserved statuses; this UI does not currently provide decline/expiry controls.

## Test checklist

1. Sign in as an administrator.
2. Enter an existing patient's User ID exactly as stored in `/users`.
3. Select an available doctor.
4. Enter at least one available slot (example: `Nov 20, 2026 at 10:30 AM`).
5. Send the offer. The admin tracker should show `PENDING RESPONSE`.
6. Sign in as that patient and open Appointments.
7. Confirm the offer appears under Available Appointment Offers.
8. Select a slot and confirm.
9. Confirm exactly one appointment appears in the patient's Confirmed Appointments list.
10. Check `/users/{patientUserId}/appointments/{offerId}` in Realtime Database.
11. Check that `/appointment_offers/{offerId}` now has status `CONFIRMED` and the booked slot.
12. Return to Admin. The offer tracker should show `CONFIRMED` and the selected slot.
13. Test tasks, journey, profile edit, map navigation, and voice features individually.

## Before release

- Open the project in Android Studio and run Gradle Sync.
- Run `app` on a physical device or emulator.
- Test using two separate accounts (one admin, one patient).
- Confirm Android 13+ notification permission is granted if testing local notifications.
- Review and deploy secure Realtime Database Rules. Client-side role checks are not a substitute for Firebase Database security rules.
- Restrict the Google Maps/Places API key to this Android package and signing certificate.
- Do not use real patient medical data for a class demonstration unless the app has appropriate privacy and security controls.
