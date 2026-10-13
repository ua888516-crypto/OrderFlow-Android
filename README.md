# Order Flow (OANDA) - Android project
1. Install Android Studio (free) and open this folder.
2. Let Gradle sync (accept any prompt to download Gradle 8.7 / SDK 34).
3. Build > Build Bundle(s) / APK(s) > Build APK(s). Output: app/build/outputs/apk/debug/app-debug.apk
4. Copy the APK to your phone and install (allow "unknown sources").
5. Create a free OANDA practice account, generate an API token under
   My Services > Manage API Access, paste it into the app.
Analysis only: no orders are placed.

## No Android Studio? Two easier ways
- Phone/cloud: upload this folder to a free GitHub repo. The Actions tab runs
  "Build APK" automatically; download OrderFlow-APK from the finished run.
- Computer (Linux/macOS, Java 17): run ./build_apk.sh
