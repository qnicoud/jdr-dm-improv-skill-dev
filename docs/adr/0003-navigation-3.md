# ADR 0003 — Navigation 3

- Statut : acceptée (2026-10-16)
- Décision de pilotage : D1
- Source : `docs/research/01-stack-technique.md` §1

## Contexte

L'app a une seule Activity et environ 7 destinations (accueil, exercice, séance du jour, historique, détail, statistiques, réglages). Les ViewModels sont créés par destination, et l'écran d'exercice reçoit des arguments (identifiant d'exercice, identifiant de session à reprendre).

## Options

1. Navigation Compose 2.10.2 (graphe, routes typées `@Serializable`).
2. **Navigation 3 1.2.0** (stable depuis le 23 sept. 2026) : la pile de retour est une liste d'états possédée par l'app (`NavBackStack`), affichée par `NavDisplay`.

## Décision

**Navigation 3 1.2.0**, avec `lifecycle-viewmodel-navigation3` 2.11.0.

- Les routes forment une hiérarchie scellée `@Serializable` qui implémente `NavKey` (`app/navigation/Routes.kt`).
- La pile est créée par `rememberNavBackStack(Home)` : elle est sauvegardée sur rotation et à la mort du processus.
- `NavDisplay` utilise les décorateurs `rememberSaveableStateHolderNavEntryDecorator()` et `rememberViewModelStoreNavEntryDecorator()`. Chaque entrée a ainsi son `ViewModelStore` et son `SavedStateHandle`.
- Les ViewModels sont créés par `viewModel(factory = …)` avec les fabriques de l'`AppContainer` (ADR 0005). Les arguments de route sont passés au constructeur, pas lus dans le `SavedStateHandle`.

```kotlin
@Serializable sealed interface Route : NavKey
@Serializable data object Home : Route
@Serializable data class Exercise(val exerciseId: String, val resumeSessionId: Long? = null, val fromDaily: Boolean = false) : Route
@Serializable data object Daily : Route
@Serializable data object History : Route
@Serializable data class SessionDetail(val sessionId: Long) : Route
@Serializable data object Stats : Route
@Serializable data object Settings : Route
```

## Conséquences

- Navigation 3 n'utilise pas de graphe XML ni de `NavController`. Naviguer se résume à `backStack.add(route)` ou `backStack.removeLastOrNull()`, ce qui se teste sans Android.
- Le retour prédictif (actif par défaut avec targetSdk 36) passe par `NavDisplay(onBack = …)`.
- La plupart des exemples en ligne utilisent encore Navigation 2. Le dev doit s'appuyer sur le guide officiel de Navigation 3.
- Repli : si Navigation 3 bloque (bug de décorateur, par exemple), on passe à Navigation Compose 2.10.2. La migration est locale à `app/navigation/`.
