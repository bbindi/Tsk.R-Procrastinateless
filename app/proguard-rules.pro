# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\bindi\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any custom keep rules here.

# Keep Room generated classes
-keep class com.brixavier.tskr.data.*_Impl { *; }

# Keep Glance Widget Receiver
-keep class com.brixavier.tskr.widget.TskRWidgetReceiver { *; }

# Strip Android debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}
