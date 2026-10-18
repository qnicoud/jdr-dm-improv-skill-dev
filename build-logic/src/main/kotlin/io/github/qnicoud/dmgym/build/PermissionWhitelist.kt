package io.github.qnicoud.dmgym.build

/** Permissions autorisées dans le manifeste fusionné (ADR 0010, amendée par l'ADR 0014). */
object PermissionWhitelist {
    val BASE: List<String> =
        listOf(
            "android.permission.VIBRATE",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.RECEIVE_BOOT_COMPLETED",
        )

    /** Ajoute la permission de récepteur dynamique d'AndroidX, propre à l'identifiant de l'application. */
    fun forApplication(applicationId: String): List<String> =
        BASE + "$applicationId.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
}
