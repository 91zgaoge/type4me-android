# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep DataStore
-keep class androidx.datastore.** { *; }

# Keep Retrofit
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
