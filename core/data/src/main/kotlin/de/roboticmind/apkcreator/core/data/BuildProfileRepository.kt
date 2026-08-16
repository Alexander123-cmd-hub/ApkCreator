package de.roboticmind.apkcreator.core.data

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Liefert die vorhandenen Build-Profile und serialisiert sie nach JSON.
 *
 * Die Daten sind bewusst in-memory: das Modul demonstriert die Schichtung und
 * dient als Integrationspunkt fuer eine spaetere echte Datenquelle.
 */
class BuildProfileRepository {

    // kotlinx.serialization bleibt ein Implementierungsdetail dieses Moduls und
    // taucht bewusst nicht in der oeffentlichen API auf - so brauchen die
    // konsumierenden Module die Bibliothek nicht auf ihrem Compile-Classpath.
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun profiles(): List<BuildProfile> = DEFAULT_PROFILES

    fun encode(profiles: List<BuildProfile>): String =
        json.encodeToString(PROFILE_LIST_SERIALIZER, profiles)

    fun decode(raw: String): List<BuildProfile> =
        json.decodeFromString(PROFILE_LIST_SERIALIZER, raw)

    private companion object {
        // Expliziter Serializer statt der reified-Variante: so ist zur Compile-Zeit
        // sichtbar, welcher Serializer erzeugt wird, und R8 findet die Referenz.
        val PROFILE_LIST_SERIALIZER = ListSerializer(BuildProfile.serializer())

        val DEFAULT_PROFILES = listOf(
            BuildProfile(
                id = "debug",
                name = "Debug",
                applicationId = "de.roboticmind.apkcreator.debug",
                versionName = "1.0.0-debug",
                minSdk = 26,
                signed = false,
            ),
            BuildProfile(
                id = "release",
                name = "Release",
                applicationId = "de.roboticmind.apkcreator",
                versionName = "1.0.0",
                minSdk = 26,
                signed = true,
            ),
        )
    }
}
