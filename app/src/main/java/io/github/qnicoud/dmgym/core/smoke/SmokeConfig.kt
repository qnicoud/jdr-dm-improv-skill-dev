package io.github.qnicoud.dmgym.core.smoke

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Modèle de fumée du lot 1 : prouve le décodage JSON strict (ADR 0009). Retiré avec le lot 2. */
@Serializable
data class SmokeConfig(
    val id: String,
    val count: Int,
    val tags: List<String> = emptyList(),
) {
    companion object {
        /** Mode strict : toute clé inconnue ou champ manquant lève une `SerializationException`. */
        private val strictJson =
            Json {
                ignoreUnknownKeys = false
                isLenient = false
                explicitNulls = true
            }

        fun decode(text: String): SmokeConfig = strictJson.decodeFromString(serializer(), text)
    }
}
