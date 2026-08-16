# ---------------------------------------------------------------------------
# R8-/ProGuard-Regeln fuer den Release-Build (isMinifyEnabled + isShrinkResources).
#
# Grundsatz: so wenig keep-Regeln wie moeglich, damit das Shrinking wirkt.
# Aufgenommen wird nur, was durch Reflection oder generierten Code gebraucht wird.
# ---------------------------------------------------------------------------

# --- Zeilennummern fuer lesbare Crash-Reports erhalten ---------------------
# Ohne SourceFile/LineNumberTable sind Stacktraces aus dem Play-Store unbrauchbar.
# Die zugehoerige mapping.txt sichert der Workflow als Artefakt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- JavaScript-Bruecke ----------------------------------------------------
# Methoden mit @JavascriptInterface werden ausschliesslich per Reflection aus
# JavaScript aufgerufen. R8 sieht keinen Aufrufer und wuerde sie entfernen.
# Die Regel greift nur, falls jemand spaeter addJavascriptInterface() nutzt -
# ohne sie waere der Fehler zur Laufzeit schwer zu finden.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

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
