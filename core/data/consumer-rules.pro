# Regeln, die dieses Modul automatisch an jede konsumierende App weitergibt.
#
# kotlinx.serialization erzeugt zu jeder @Serializable-Klasse einen Companion mit
# einer statischen serializer()-Methode und ein $$serializer-Objekt. Beides wird
# ueber Reflection bzw. ueber generierte Referenzen aufgeloest - R8 wuerde diese
# Member sonst als "ungenutzt" entfernen und der Release-Build wirft zur Laufzeit
# SerializationException / NoSuchMethodError.

# Serializer-Objekte der Modell-Klassen behalten.
-keepclassmembers class de.roboticmind.apkcreator.core.data.** {
    *** Companion;
}
-keepclasseswithmembers class de.roboticmind.apkcreator.core.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.roboticmind.apkcreator.core.data.**$$serializer { *; }

# Die von kotlinx.serialization erzeugten Metadaten nicht wegwerfen.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault,InnerClasses,Signature
