# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Metro DI - Keep Application class and its DI graph
# Required to prevent ClassCastException in ComposeAppComponentFactory
-keep class ink.trmnl.android.buddy.TrmnlBuddyApp {
    public <methods>;
}
-keep class ink.trmnl.android.buddy.di.AppGraph** { *; }

# Keep ComposeAppComponentFactory and its methods
-keep class ink.trmnl.android.buddy.di.ComposeAppComponentFactory {
    public <methods>;
}

# Metro - Keep all generated factories and components
-keep class **_Factory { *; }
-keep class **_Component { *; }

# Keep Metro Provider interface
-keep interface dev.zacsweers.metro.Provider { *; }