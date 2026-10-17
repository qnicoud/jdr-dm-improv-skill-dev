# Règles R8 de DM Gym. Les bibliothèques fournissent leurs propres règles consommateur
# (kotlinx.serialization, Room 3, DataStore, Navigation 3, Compose) ; on ne garde ici que
# ce que le code de l'app expose par réflexion.

# kotlinx.serialization : sérialiseurs générés des classes @Serializable de l'app
# (routes de navigation, TableFile/TableEntry, enregistrements de séance, ExerciseId...).
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations
-dontnote kotlinx.serialization.**

-keepclassmembers @kotlinx.serialization.Serializable class io.github.qnicoud.dmgym.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class io.github.qnicoud.dmgym.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class io.github.qnicoud.dmgym.**
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Room 3 : le code généré (*_Impl) est instancié par réflexion.
-keep class * extends androidx.room3.RoomDatabase { <init>(); }
