package io.github.qnicoud.dmgym.feature.exercise

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import io.github.qnicoud.dmgym.core.exercise.AnswerKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * `TextFieldState` possédés par le ViewModel (architecture §3). Leur contenu remonte au réducteur par
 * `snapshotFlow`, et [syncNow] force la remontée avant une écriture pour ne perdre aucun caractère.
 */
internal class FieldStates(
    private val scope: CoroutineScope,
    private val initialText: (AnswerKey) -> String,
    private val onChange: (AnswerKey, String) -> Unit,
) {
    private val states = mutableMapOf<AnswerKey, TextFieldState>()

    fun get(key: AnswerKey): TextFieldState =
        states.getOrPut(key) {
            TextFieldState(initialText(key)).also { state ->
                scope.launch { snapshotFlow { state.text.toString() }.collect { onChange(key, it) } }
            }
        }

    /** Pousse tout de suite le texte courant de chaque champ, sans attendre l'application du snapshot. */
    fun syncNow() {
        states.forEach { (key, state) -> onChange(key, state.text.toString()) }
    }
}
