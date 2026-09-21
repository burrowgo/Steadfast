# Proguard rules for Steadfast
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
