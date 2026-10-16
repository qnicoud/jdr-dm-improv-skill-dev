# ADR 0004 — Room 3 pour l'historique, avec repli Room 2.8.5

- Statut : acceptée (2026-10-16)
- Décision de pilotage : D2
- Source : `docs/research/01-stack-technique.md` §1

## Contexte

L'historique stocke des sessions, des tirages, des réponses et des auto-évaluations, avec des requêtes de filtre, de comptage et de série. Room 3 (`androidx.room3`, 3.0.3) ne fonctionne qu'avec KSP, exige un `SQLiteDriver` et le plugin Gradle `androidx.room3` avec `schemaDirectory`. Personne n'a confirmé KSP 2.3.12 avec Kotlin 2.4.21.

## Options

1. **Room 3.0.3** (KSP, DAO en `suspend`/`Flow`, driver explicite).
2. Room 2.8.5 (`androidx.room`, mature, beaucoup d'exemples).
3. SQLDelight ou DataStore seul : écartés, car hors stack et inadaptés aux requêtes.

## Décision

- **Room 3.0.3** via KSP, plugin `androidx.room3`, `schemaDirectory("$projectDir/schemas")`. Les schémas exportés sont versionnés.
- **Driver : `AndroidSQLiteDriver`** (SQLite du système, artefact `androidx.sqlite:sqlite-framework`). Il n'alourdit pas l'APK, contrairement au driver embarqué. Sa version est alignée sur celle que tire Room 3 et sera confirmée au lot 1.
- Toutes les méthodes de DAO sont `suspend` ou renvoient un `Flow` : aucun accès sur le thread principal.
- La base est construite **une seule fois** dans l'`AppContainer` (ADR 0005).
- Migrations : la v1 part de la version de schéma 1. Ensuite, on écrit des migrations explicites (`AutoMigration` quand c'est possible), testées. **Pas de `fallbackToDestructiveMigration`** : l'historique de l'utilisateur ne doit jamais être effacé.
- Le repli dépend du symptôme observé au lot 1 :

| Symptôme au build de fumée | Repli |
|---|---|
| Erreur AGP / KGP (plugin, Kotlin intégré) | AGP 9.3.1 (ADR 0001) |
| KSP fonctionne, mais le compilateur ou le runtime Room 3 échoue (DAO non généré, driver) | **Room 2.8.5** (`androidx.room`, toujours via KSP) |
| KSP 2.3.12 lui-même échoue avec Kotlin 2.4.21 | Room 2 ne règle rien (il passe aussi par KSP). On essaie la dernière KSP publiée. Si l'échec persiste, on redescend à la dernière Kotlin 2.3.x (décision D25 du 2026-10-16) et on met à jour l'ADR 0001 |

## Conséquences

- La couche `data/history` expose une interface `HistoryRepository`. Les ViewModels et la logique ne voient jamais d'entité ni de DAO, ce qui limite le repli Room 2 à changer de paquet dans `data/history/db`.
- Tests de DAO : sous Robolectric, avec une base en mémoire.
- La plupart des exemples en ligne utilisent `androidx.room` : il faut adapter les imports.
