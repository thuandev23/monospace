# ─── Crash traces: giữ source file và line number ───────────────────────────
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ─── Room ─────────────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface *
-keepclassmembers @androidx.room.Dao interface * { *; }

# ─── Retrofit + OkHttp ────────────────────────────────────────────────────────
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ─── Gson ─────────────────────────────────────────────────────────────────────
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
# Keep all DTOs and domain models used with Gson
-keep class com.monospace.app.core.network.dto.** { *; }
-keep class com.monospace.app.core.domain.model.** { *; }
-keepclassmembers class com.monospace.app.core.domain.model.** { *; }

# ─── Hilt / Dagger ────────────────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * extends androidx.lifecycle.ViewModel
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# ─── WorkManager ──────────────────────────────────────────────────────────────
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.CoroutineWorker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ─── Kotlin coroutines ────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ─── Kotlin Metadata (used by Hilt/KSP) ──────────────────────────────────────
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.reflect.jvm.internal.**

# ─── DataStore ────────────────────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ─── Firebase Crashlytics ─────────────────────────────────────────────────────
-keepattributes *Annotation*
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.**

# ─── Compose ──────────────────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ─── Google Play Billing ──────────────────────────────────────────────────────
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**

# ─── App-specific: enums (used in Room TypeConverters) ────────────────────────
-keepclassmembers enum com.monospace.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
