# WebView
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.examples.android.model.** { *; }
-dontwarn com.google.gson.**
