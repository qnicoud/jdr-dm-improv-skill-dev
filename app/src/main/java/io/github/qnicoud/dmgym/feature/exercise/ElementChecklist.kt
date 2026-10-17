package io.github.qnicoud.dmgym.feature.exercise

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.core.exercise.CardSlot
import io.github.qnicoud.dmgym.core.exercise.Draw
import io.github.qnicoud.dmgym.core.exercise.DrawKey
import io.github.qnicoud.dmgym.core.exercise.ExerciseDefinition
import io.github.qnicoud.dmgym.core.exercise.Phase
import io.github.qnicoud.dmgym.ui.components.CategoryIcon
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

/** Carte de tirage : avec cases « utilisé » quand l'exercice détecte des mots, simple sinon. */
@Composable
internal fun ReadyDraw(
    state: ExerciseUiState,
    actions: ExerciseActions,
) {
    val draws = state.session.draws
    val showReroll = state.session.phase != Phase.Ended
    if (state.definition.detection != null) {
        DetectionDrawCard(
            definition = state.definition,
            draws = draws,
            rerollCount = state.session.rerollCount,
            showReroll = showReroll,
            onReroll = { actions.onReroll(null) },
            onCheckToggled = actions.onCheckToggled,
            readOnly = state.session.phase == Phase.Ready,
        )
    } else {
        PlainDraw(state, actions)
    }
}

/**
 * Tirage sans détection : la carte de statut en grand (Status Numbers), la bascule en bandeau, et rien du tout
 * quand aucun slot n'est tiré (mode vidéo de Decision Point, One-Page avec campagne).
 */
@Composable
private fun PlainDraw(
    state: ExerciseUiState,
    actions: ExerciseActions,
) {
    val slots = state.definition.slots
    val slotKeys = slots.map { it.key }.toSet()
    val shown = state.session.draws.filter { it.slotKey in slotKeys }
    state.session.draws
        .filter { it.slotKey !in slotKeys }
        .forEach { ShiftBanner(it) }
    if (shown.isEmpty()) return
    DrawCard(
        draws = shown,
        rerollCount = state.session.rerollCount,
        showReroll = state.session.phase != Phase.Ended,
        showChecks = false,
        onReroll = { actions.onReroll(null) },
        onCheckToggled = actions.onCheckToggled,
        cardSlots = slots.filterIsInstance<CardSlot>().map { it.key }.toSet(),
    )
}

/**
 * Carte de tirage d'un exercice à détection (Constraint Box, Connect the Story) : chaque élément a sa case
 * « utilisé », annoncée par TalkBack avec son état. Le panneau se replie quand le clavier s'ouvre si
 * [ExerciseDefinition.collapsiblePanel], et ne se déplie qu'à la demande (D48).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetectionDrawCard(
    definition: ExerciseDefinition,
    draws: List<Draw>,
    rerollCount: Int,
    showReroll: Boolean,
    onReroll: () -> Unit,
    onCheckToggled: (DrawKey) -> Unit,
    modifier: Modifier = Modifier,
    /** Écran Prêt : éléments affichés sans case, le cochage n'existe que pendant l'exercice. */
    readOnly: Boolean = false,
) {
    val items = remember(definition, draws) { checklistItems(definition, draws) }
    var fold by rememberSaveable(stateSaver = PanelFold.Saver) { mutableStateOf(PanelFold()) }
    val imeVisible = WindowInsets.isImeVisible
    LaunchedEffect(imeVisible, definition.collapsiblePanel) {
        if (imeVisible && definition.collapsiblePanel) fold = fold.onKeyboardShown()
    }
    ElementChecklist(
        items = items,
        collapsible = definition.collapsiblePanel,
        expanded = fold.expanded || !definition.collapsiblePanel,
        onExpandedChange = { fold = fold.toggled() },
        rerollCount = rerollCount,
        showReroll = showReroll,
        onReroll = onReroll,
        onCheckToggled = onCheckToggled,
        modifier = modifier,
        readOnly = readOnly,
    )
}

/** Version sans état de [DetectionDrawCard]. */
@Composable
fun ElementChecklist(
    items: List<ChecklistItem>,
    collapsible: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    rerollCount: Int,
    showReroll: Boolean,
    onReroll: () -> Unit,
    onCheckToggled: (DrawKey) -> Unit,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
) {
    Card(
        modifier = modifier.fillMaxWidth().testTag(ExerciseTags.DRAW_CARD),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = if (expanded || readOnly) 16.dp else 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!readOnly) ProgressHeader(items, collapsible, expanded, onExpandedChange)
            if (expanded || readOnly) {
                items.forEach { ItemRow(it, onCheckToggled, readOnly) }
                if (showReroll) RerollRow(rerollCount, onReroll)
            }
        }
    }
}

@Composable
private fun ProgressHeader(
    items: List<ChecklistItem>,
    collapsible: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val (done, total) = items.progress()
    if (total == 0) return
    val format = if (collapsible) R.string.checklist_linked else R.string.checklist_used_count
    val rowModifier =
        if (collapsible) {
            val action = stringResource(if (expanded) R.string.checklist_collapse else R.string.checklist_expand)
            Modifier
                .heightIn(min = 48.dp)
                .clickable(onClickLabel = action, role = Role.Button) { onExpandedChange(!expanded) }
                .testTag(ExerciseTags.CHECKLIST_TOGGLE)
        } else {
            Modifier
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = rowModifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(format, done, total),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f).testTag(ExerciseTags.CHECKLIST_COUNT),
        )
        if (collapsible) {
            Icon(
                DmIcons.Chevron,
                contentDescription = null,
                modifier = Modifier.rotate(if (expanded) COLLAPSE_ROTATION else EXPAND_ROTATION),
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: ChecklistItem,
    onCheckToggled: (DrawKey) -> Unit,
    readOnly: Boolean,
) {
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(ExerciseTags.check(item.key))
    if (!item.checkable || readOnly) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = rowModifier.semantics(mergeDescendants = true) {},
        ) {
            CategoryMark(item.category)
            Column { ItemTexts(item) }
        }
        return
    }
    val state = stateText(item)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            rowModifier
                .toggleable(
                    value = item.checked,
                    role = Role.Checkbox,
                    onValueChange = { onCheckToggled(item.key) },
                ).semantics { stateDescription = state },
    ) {
        CategoryMark(item.category)
        Column(modifier = Modifier.weight(1f)) { ItemTexts(item) }
        if (item.auto) {
            Text(
                stringResource(R.string.checklist_auto_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Checkbox(checked = item.checked, onCheckedChange = null)
    }
}

/** Icône de la catégorie, lue par TalkBack avec la ligne (« Vue, Mot imposé, brumeux »). Rien sans catégorie connue. */
@Composable
private fun CategoryMark(category: String?) {
    val mark = CategoryIcon.fromCategory(category) ?: return
    Icon(
        mark.icon,
        contentDescription = stringResource(mark.label),
        modifier = Modifier.padding(end = 12.dp).size(24.dp),
    )
}

@Composable
private fun ItemTexts(item: ChecklistItem) {
    item.label?.let {
        Text(
            stringResource(it),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Text(item.text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun stateText(item: ChecklistItem): String =
    stringResource(
        when {
            item.checked && item.auto -> R.string.checklist_state_auto
            item.checked -> R.string.checklist_state_used
            else -> R.string.checklist_state_unused
        },
    )

private const val COLLAPSE_ROTATION = -90f
private const val EXPAND_ROTATION = 90f

private val previewItems =
    listOf(
        ChecklistItem(
            DrawKey("place", 0),
            R.string.connect_story_place,
            "une auberge de bord de route",
            true,
            true,
            true,
        ),
        ChecklistItem(DrawKey("object", 0), R.string.connect_story_object, "une clé sans serrure", true, true, false),
        ChecklistItem(DrawKey("line", 0), R.string.connect_story_line, "« Je n’ai rien vu. »", true, false, false),
    )

@Preview(name = "Panneau déplié, clair", showBackground = true)
@Preview(name = "Panneau déplié, sombre", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ExpandedChecklistPreview() {
    DmGymTheme {
        ElementChecklist(
            previewItems,
            true,
            true,
            {},
            rerollCount = 1,
            showReroll = true,
            onReroll = {},
            onCheckToggled = {},
        )
    }
}

@Preview(name = "Panneau replié, clair", showBackground = true)
@Preview(name = "Panneau replié, sombre", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FoldedChecklistPreview() {
    DmGymTheme {
        ElementChecklist(
            previewItems,
            true,
            false,
            {},
            rerollCount = 0,
            showReroll = true,
            onReroll = {},
            onCheckToggled = {},
        )
    }
}
