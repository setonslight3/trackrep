# TrackRep ProGuard & R8 Optimization Rules

# ==================== AndroidX Room ====================
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# ==================== Google ML Kit Pose Detection ====================
-keep class com.google.mlkit.vision.pose.** { *; }
-keep class com.google.mlkit.vision.common.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_pose.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ==================== AndroidX CameraX ====================
-keep class androidx.camera.core.** { *; }
-keep class androidx.camera.camera2.** { *; }
-keep class androidx.camera.lifecycle.** { *; }
-keep class androidx.camera.view.** { *; }
-keep class androidx.camera.video.** { *; }

# ==================== AndroidX Media3 Video ====================
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }

# ==================== TrackRep Data & Domain Models ====================
-keep class com.setons.trackrep.data.local.entity.** { *; }
-keep class com.setons.trackrep.backup.** { *; }
-keep class com.setons.trackrep.adaptive.** { *; }
-keep class com.setons.trackrep.analytics.** { *; }
-keep class com.setons.trackrep.exercise.model.** { *; }
-keep class com.setons.trackrep.ai.action.** { *; }

# ==================== Kotlin Coroutines & JSON ====================
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn org.json.**
