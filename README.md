# GameSolution

Android floating bubble utility.

## Current requirements
1. Draw over other apps permission.
2. Floating bubble visible over other apps.
3. Tapping the bubble opens an overlay menu with **Restart App**.
4. Restart App targets the app that was in the foreground immediately before GameSolution's overlay.

### Important Android limitation
A normal third-party Android app cannot silently force-stop an arbitrary other app. The project therefore separates **target detection** from **restart execution**. On standard devices it relaunches the target app; on managed/rooted/POS devices a privileged force-stop + launch strategy can be enabled later.
