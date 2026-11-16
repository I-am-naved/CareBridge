# UI Redesign to Match CareBridge Design Specification

Provide a comprehensive UI update across all screens, layouts, and components in the CareBridge app to match the exact visual style, colors, typography, iconography, and layout presented in the design reference image.

## User Review Required

> [!IMPORTANT]
> This redesign encompasses all 10 screens shown in the design:
> 1. Login / Welcome Screen
> 2. Home Screen (with Next Appointment banner, 6 quick action cards, notification badge, bottom nav)
> 3. Appointments Screen (Tabs: Upcoming, Past, All, date badges, + Add Appointment)
> 4. Appointment Details Screen (Instructions bullets, Read Aloud, View on Map, Get Directions)
> 5. Appointment Location Screen (Map view, distance/time metrics, Directions & Google Maps buttons)
> 6. Voice Assistant Screen (Waveform rings, microphone button, prompt suggestion chips)
> 7. Add Healthcare Note Screen (Inputs, voice recording preview with waveform and timer)
> 8. Care Tasks Screen (Tabs: To Do (3), Completed (5), task rows with checkboxes and overflow menu)
> 9. Nearby Services Screen (Category filter chips: All, Hospitals, Clinics, Pharmacies, distance metrics)
> 10. Profile Screen (Profile avatar with edit badge, Sarah Ahmed, menu items with icons and chevrons, Sign Out)

## Open Questions

- None. All requirements are directly sourced from the visual design reference.

## Proposed Changes

### Theme & Colors
- Update `colors.xml` and styles to use the exact primary dark blue (`#173B57`), teal accent (`#2D8C88`), background light gray (`#F6F8FA`), card white (`#FFFFFF`), and text colors.

### Layouts & Activities
#### [MODIFY] [activity_main.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/activity_main.xml)
- Implement Welcome/Login landing screen with CareBridge logo, tagline, illustration, and "Log In" / "Create Account" buttons.

#### [MODIFY] [activity_home.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/activity_home.xml)
- Update Home screen header, Sarah's greeting banner, Next Appointment card, 6 quick action cards ("Ask My Provider" replacing generic ones), and bottom navigation bar.

#### [MODIFY] [dialog_appointments.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/dialog_appointments.xml)
- Update Appointments list screen with Upcoming/Past/All tabs, date badges (Nov 20, Dec 05, Dec 18), and "+ Add Appointment".

#### [MODIFY] [activity_appointment_location.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/activity_appointment_location.xml)
- Update Appointment Details and Appointment Location screens with map view, instructions, Read Aloud, distance badges (2.1 km, 6 min), and map buttons.

#### [MODIFY] [activity_voice_assistant.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/activity_voice_assistant.xml)
- Update Voice Assistant screen with waveform visualizer circles, mic button, and prompt suggestion chips.

#### [MODIFY] [dialog_add_activity.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/dialog_add_activity.xml)
- Update Add Healthcare Note screen with title/description inputs, mic icon, and waveform recording preview (00:05).

#### [MODIFY] [dialog_tasks.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/dialog_tasks.xml)
- Update Care Tasks screen with To Do (3) / Completed (5) tabs, checkboxes, and task rows.

#### [MODIFY] [activity_nearby_services.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/activity_nearby_services.xml)
- Update Nearby Services screen with filter chips (All, Hospitals, Clinics, Pharmacies) and list items.

#### [MODIFY] [activity_profile.xml](file:///C:/Users/Asus/Downloads/CareBridge-main/CareBridge-main/app/src/main/res/layout/activity_profile.xml)
- Update Profile screen with avatar, edit icon, Sarah Ahmed, and settings menu options.

## Verification Plan

### Automated Tests
- Build project successfully using Gradle build task to ensure all XML layouts and Java bindings compile correctly.

### Manual Verification
- Deploy app to Android device/emulator and verify each screen matches the visual reference image.
