# ADR 0011 — Série de jours avec jour de grâce, et séance du jour déterministe

- Statut : acceptée (2026-10-16)
- Décisions de pilotage : D15 (jour de grâce, compteur mis en avant), D16 (minuit local), D20 (séance suggérée et remplaçable)
- Sources : `docs/research/01-stack-technique.md` §8, `docs/research/02-pedagogie-exercices.md` §4, `docs/research/03-existant-ux.md` §2.1

## Contexte

La série de jours doit motiver sans culpabiliser : jour de grâce, pas de remise à zéro punitive, nombre de sessions affiché en premier. Le jour change à minuit local. La séance du jour propose 3 exercices déterministes (graine = date), chacun remplaçable, et exclut One-Page par défaut.

## Décision

### Jour d'une session
À la création d'une session, on enregistre `startedAtEpochMillis`, `zoneId` et **`localEpochDay`** (`LocalDate.now(clock).toEpochDay()` dans le fuseau du moment). La série ne se calcule que sur `localEpochDay` : un voyage ou un changement d'heure ne réécrit pas l'historique.

### Session comptée
Une session compte pour la série et pour les statistiques si son statut est `COMPLETED` (voir `docs/architecture.md` §5.4).

### Calcul pur `core/streak/StreakCalculator`
```kotlin
data class StreakSummary(
    val totalSessions: Int,        // affiché en premier
    val practicedDays: Int,
    val current: Int,              // jours pratiqués dans la série courante
    val best: Int,
    val graceUsedOn: Long?,        // jour de grâce consommé dans la série courante (le plus récent)
    val todayDone: Boolean,
)
fun computeStreak(days: Set<Long>, sessions: Int, today: Long): StreakSummary
```
Règles :
1. On remonte depuis aujourd'hui. **Aujourd'hui non pratiqué ne rompt pas la série** : on part d'hier.
2. **Jour de grâce** : un jour manquant isolé, encadré par deux jours pratiqués, ne rompt pas la série si aucun autre jour de grâce n'a été consommé dans les **6 jours précédents** (au plus un par fenêtre glissante de 7 jours). Le jour de grâce ne compte pas dans `current`.
3. Deux jours consécutifs manquants, ou un second jour de grâce dans la fenêtre, terminent la série.
4. **Pas de remise à zéro punitive** : `best`, `practicedDays` et `totalSessions` ne baissent jamais. Après une rupture, la série repart de 1 à la session suivante, sans message de reproche (le libellé est fixé par l'UX). `current` vaut 0 seulement si ni aujourd'hui ni hier ni (en grâce) avant-hier n'ont été pratiqués.

Cas de test obligatoires : deux sessions le même jour, trou d'un jour (grâce), deux trous dans la même semaine, trou de deux jours, aujourd'hui non fait, changement d'heure en mars et en octobre, changement de fuseau, horloge réglée dans le passé (jour futur ignoré), historique vide.

### Séance du jour `core/daily/DailySessionPlanner`
```kotlin
fun plan(today: Long, stats: PracticeStats, catalog: List<ExerciseId>, options: DailyOptions): DailyPlan
```
- `Random(seed = today)` : la séance est reproductible sans rien stocker.
- `stats` ne contient **que l'historique antérieur à aujourd'hui** (`localEpochDay < today`). La séance ne change donc pas dans la journée, même après une session, et le plan de la veille peut être recalculé pour vérifier les règles.
- Règles, dans l'ordre :
  - 3 exercices distincts, dans l'ordre affiché « échauffement → cœur → zone faible » ;
  - One-Page est exclu sauf si `options.includeOnePage` ;
  - 1 échauffement court parmi les exercices ≤ 5 min par défaut (Decision Point, Constraint Box, Status Numbers) ;
  - 1 « zone faible » : l'exercice le moins pratiqué sur les 14 derniers jours (égalités départagées par le `Random`) ;
  - aucun exercice du plan de la veille à la même position ; si c'est impossible, on l'accepte.
- **Remplacement** : l'utilisateur remplace un exercice par un autre, au hasard ou choisi. Les remplacements sont stockés dans DataStore (`daily.epochDay`, `daily.replacements`) et effacés le lendemain.
- **Avancement** : un exercice est « fait » quand une session `COMPLETED` de ce type existe aujourd'hui avec `fromDaily = true`, ou à défaut avec n'importe quelle origine.

## Conséquences

- Les deux calculateurs sont purs et testés en JVM (`Clock.fixed`, jours `Long`).
- Le jour logique à 04:00 (option écartée par D16) reste possible plus tard : il suffit de changer la fonction qui calcule `localEpochDay` à l'écriture, sans migration de la série.

## Complément (lot L6) : définition de « contenu » et sessions comptées

- **Contenu** : une session a du contenu si elle a au moins une réponse (`session_answers`) dont `trim(text) <> ''`. Les tirages seuls et la grille seule ne comptent pas : un tirage n'est pas une production.
- **Session comptée** : une session avec contenu, qu'elle soit `COMPLETED` ou inachevée (`IN_PROGRESS` sans activité depuis plus de 24 h, comparaison stricte). La session en cours active est exclue. Cette définition vaut pour les jours pratiqués (D35), le nombre de sessions et les comptages par exercice. Elle remplace la règle « statut `COMPLETED` » de la section « Session comptée ».
- **Délai de 24 h** : il se compte depuis la dernière activité (`lastActivityAtEpochMillis`, mis à jour par `saveSnapshot` et `updateSession`), pas depuis le début (D36). La purge supprime les sessions en cours vides au-delà de ce délai ; une session avec contenu n'est jamais supprimée.
- Le passage de minuit local et le changement de fuseau relèvent du lot L11.
