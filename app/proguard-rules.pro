# ============================================================
# AuraPC App — ProGuard Rules
# ============================================================

# Keep line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---- Data models (Gson / Retrofit response bodies) --------
-keep class com.example.aura_pc_app.data.model.** { *; }
-keep class com.example.aura_pc_app.domain.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ---- Retrofit -----------------------------------------------
-keepattributes Signature
-keepattributes Exceptions
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**

# ---- OkHttp -------------------------------------------------
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ---- Gson ---------------------------------------------------
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# ---- Room ---------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.**

# ---- Lifecycle (ViewModel / LiveData) -----------------------
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-dontwarn androidx.lifecycle.**

# ---- WorkManager -------------------------------------------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker
-dontwarn androidx.work.**

# ---- Security Crypto / Biometric ---------------------------
-dontwarn androidx.security.**
-dontwarn androidx.biometric.**

# ---- Parcelable --------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ---- Enums -------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Native methods ----------------------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}
