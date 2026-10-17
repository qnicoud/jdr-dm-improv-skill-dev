# ADR 0013 — Réglages dans DataStore Preferences

- Statut : acceptée (2026-10-16)
- Décisions de pilotage : D7 (son, vibration réglables), D17 (durée réglable, sans minuteur), D20 (remplacements de la séance du jour)
- Source : `docs/research/01-stack-technique.md` §1 (DataStore 1.2.1)

## Contexte

Les réglages sont peu nombreux, de type clé/valeur, et lus en flux par plusieurs écrans. Les mettre dans Room mélangerait préférences et historique.

## Options

1. SharedPreferences (API synchrone, déconseillée).
2. **DataStore Preferences 1.2.1** (asynchrone, `Flow`).
3. Une table Room de réglages.

## Décision

DataStore Preferences, derrière `SettingsRepository` (interface du domaine). L'implémentation `data/settings/DataStoreSettingsRepository` utilise un fichier unique `settings.preferences_pb`.

| Clé | Type | Défaut |
|---|---|---|
| `sound_enabled` | Boolean | `true` |
| `vibration_enabled` | Boolean | `true` |
| `duration_s.<exerciseId>` | Int | absent = durée par défaut de la définition |
| `untimed.<exerciseId>` | Boolean | `false` (`true` pour One-Page, dont la politique est `Optional`) |
| `daily_include_one_page` | Boolean | `false` |
| `daily_epoch_day` | Long | — |
| `daily_replacements` | String (`pos:exerciseId;…`) | — |

```kotlin
data class Settings(
    val soundEnabled: Boolean,
    val vibrationEnabled: Boolean,
    val durations: Map<ExerciseId, Duration>,
    val untimed: Set<ExerciseId>,
    val dailyIncludeOnePage: Boolean,
)
interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}
```

- Les bornes de durée (ADR 0006) sont appliquées **à l'écriture** par une fonction pure `clampDuration(definition, value)`, et de nouveau à la lecture (valeur corrompue → défaut).
- Une lecture en erreur (`IOException`) émet les valeurs par défaut et journalise l'erreur.

## Conséquences

- Les tests JVM utilisent un `FakeSettingsRepository`. L'implémentation DataStore est testée sous Robolectric avec un fichier temporaire.
- Ajouter un réglage revient à ajouter une clé, sans migration.

## Complément (2026-10-17, décision D53)

Nouveau réglage `themePalette` : `PARCHMENT` (défaut) / `DYNAMIC` (Material You, API 31+ ; masqué ou replié sur `PARCHMENT` en dessous) / quelques palettes Material conformes. Lu par le thème Compose ; contrastes vérifiés par `ContrastTest` pour chaque palette statique.

## Complément (2026-10-18, v1.1)

- Rappels (ADR 0014) : `reminder_enabled`, `reminder_minute_of_day`, `reminder_days` et `reminder_resumed_epoch_day`.
- Séance du jour : `daily_plan` (ADR 0017 §4).
- La langue n'est **pas** stockée dans DataStore : sur Android 13+, le système conserve le choix de langue par app (ADR 0016).
- La clé `crazy_eights_minute_haptic` garde son nom ; seul son libellé change (« Vibration de rythme »).
