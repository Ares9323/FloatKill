# AIDL hidden APIs accessed via reflection (DirectKillStrategy)
-keep class android.app.IActivityManager { *; }
-keep class android.app.ActivityManagerNative { *; }
-keep class android.app.ActivityManager { *; }
-keepclassmembers class android.app.ActivityManager {
    public void forceStopPackage(java.lang.String);
}
