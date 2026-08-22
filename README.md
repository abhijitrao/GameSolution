# GameSolution 

Android floating bubble utility for restarting the app currently in use.

## Features
1. Draw over other apps permission.
2. Floating bubble visible over other apps.
3. Tapping the bubble opens an overlay menu with **Restart App**.
4. Usage Access identifies the most recently foreground app.
5. Foreground service keeps the bubble available while the user uses other apps.
6. Android 13+ notification permission is requested for the foreground service notification.
7. Battery optimization exemption can be requested to improve background reliability.
8. Restart execution uses the strongest mechanism available on the device.

### Restart behavior
- **Rooted device:** attempts `su` + `am force-stop <package>` and then launches the target launcher activity.
- **Normal device:** relaunches the target launcher activity.

### Important Android limitation
A normal third-party Android app cannot silently force-stop an arbitrary other app. Overlay, Usage Access, notification, and battery-optimization permissions do **not** grant arbitrary-app force-stop privileges. Device-owner/system/privileged deployments can add a stronger force-stop mechanism separately.

## Compatibility
- Minimum SDK: 26 (Android 8.0)
- Target SDK: 35
