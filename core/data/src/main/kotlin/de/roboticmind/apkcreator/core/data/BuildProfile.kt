package de.roboticmind.apkcreator.core.data

import kotlinx.serialization.Serializable

/**
 * Beschreibt eine Build-Konfiguration, wie sie die App darstellt.
 *
 * Die Klasse ist [Serializable] - kotlinx.serialization erzeugt dafuer einen
 * Companion mit `serializer()`. Damit R8 diesen im Release-Build nicht entfernt,
 * gelten die keep-Regeln aus `core/data/consumer-rules.pro`.
 */
@Serializable
data class BuildProfile(
    val id: String,
    val name: String,
    val applicationId: String,
    val versionName: String,
    val minSdk: Int,
    val signed: Boolean,
)
