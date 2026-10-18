package io.github.qnicoud.dmgym.core.backup

import io.github.qnicoud.dmgym.core.history.SessionDetail
import kotlinx.serialization.Serializable

/** Première ligne de l'export texte lisible, que l'import reconnaît pour dire « export à lire ». */
const val READABLE_EXPORT_HEADER = "DM Gym - export de mes sessions"

/** Nom du format et version que cette app écrit (ADR 0015 §1, §2). */
internal const val BACKUP_FORMAT = "dmgym-sessions"
internal const val BACKUP_VERSION = 1

/** Raisons pour lesquelles un fichier est refusé en entier ; chacune a son message dans l'écran d'import (UX §3.3). */
enum class ImportError {
    /** Pas du JSON, ou pas le bon format. */
    NotABackup,

    /** L'export texte lisible, choisi par erreur. */
    ReadableExport,

    /** Tronqué, abîmé ou modifié : le décodage strict ou la validation a refusé le fichier. */
    Incomplete,

    /** Écrit par une version plus récente de l'app. */
    NewerVersion,

    /** Le fournisseur de fichiers n'a pas pu le lire. */
    Unreadable,

    /** Au-delà de la taille maximale ([BoundedText.MAX_BYTES]). */
    TooLarge,

    /** L'écriture a échoué : la transaction est annulée, rien n'a été ajouté. */
    WriteFailed,
}

sealed interface DecodeResult {
    /** [sessions] valides (des exercices connus) ; [unknownExercise] sessions ignorées car d'un exercice inconnu. */
    data class Success(
        val sessions: List<SessionDetail>,
        val unknownExercise: Int,
        /** Sessions écartées parce qu'une de leurs valeurs est hors bornes ; le reste du fichier est importé. */
        val invalid: Int = 0,
    ) : DecodeResult

    data class Failure(
        val error: ImportError,
    ) : DecodeResult
}

/** En-tête commun à toutes les versions : lu avant le reste du fichier, qui peut avoir une forme inconnue. */
@Serializable
internal data class BackupEnvelope(
    val format: String,
    val formatVersion: Int,
)
