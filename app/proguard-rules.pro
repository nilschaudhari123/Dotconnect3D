-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keep class com.naampath.colorpath3d.data.db.** { *; }
-keep class com.badlogic.gdx.** { *; }
-keepclassmembers class com.badlogic.gdx.backends.android.AndroidInput* {
    <init>(com.badlogic.gdx.Application, android.content.Context, java.lang.Object, com.badlogic.gdx.backends.android.AndroidApplicationConfiguration);
}
-dontwarn com.badlogic.gdx.**

-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.android.billingclient.** { *; }
-keep class com.google.android.gms.games.** { *; }

-keepclassmembers class * {
    @com.google.firebase.crashlytics.FirebaseCrashlytics *;
}
