# ---------------------------------------------------------------------------
# R8-/ProGuard-Regeln fuer den Release-Build (isMinifyEnabled + isShrinkResources).
#
# Grundsatz: so wenig keep-Regeln wie moeglich, damit das Shrinking wirkt.
# Aufgenommen wird nur, was durch Reflection oder generierten Code gebraucht wird.
# ---------------------------------------------------------------------------

# --- Zeilennummern fuer lesbare Crash-Reports erhalten ---------------------
# Ohne SourceFile/LineNumberTable sind Stacktraces aus dem Play-Store unbrauchbar.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlin-Reflection-Metadaten -------------------------------------------
# Signature/Annotations werden u. a. von kotlinx.serialization und von
# generischen Typen zur Laufzeit ausgewertet.
-keepattributes Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

# --- kotlinx.serialization -------------------------------------------------
# Reflection-basiert: serializer() wird ueber den Companion aufgeloest.
# Die Modell-spezifischen Regeln liefert :core:data selbst per consumer-rules.pro;
# hier stehen nur die bibliotheksweiten Regeln.
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-dontwarn kotlinx.serialization.**

# --- Jetpack Compose -------------------------------------------------------
# Compose selbst bringt seine Regeln als consumer rules mit; noetig ist nur,
# dass die Compose-Runtime-Annotationen nicht verloren gehen.
-dontwarn androidx.compose.**

# --- Kotlin-Standardbibliothek --------------------------------------------
# Intrinsics-Aufrufe fuer Null-Checks entfernen (kleinere und schnellere APK).
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkNotNull(java.lang.Object, java.lang.String);
    static void checkNotNullParameter(java.lang.Object, java.lang.String);
    static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
}

# --- Log-Ausgaben aus dem Release-Build entfernen --------------------------
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
