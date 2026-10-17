# ADR 0010 — 100 % hors ligne : liste blanche de permissions vérifiée en CI

- Statut : acceptée (2026-10-16)
- Contrainte : SPEC « entièrement hors ligne », `CLAUDE.md` « aucune permission réseau »
- Source : `docs/research/01-stack-technique.md` §7

## Contexte

Une dépendance transitive peut ajouter `INTERNET` ou `ACCESS_NETWORK_STATE` au manifeste fusionné sans que personne ne s'en aperçoive. Une liste noire laisse passer les permissions qu'on n'a pas prévues.

## Options

1. Revue manuelle du manifeste avant chaque release.
2. Liste noire (`INTERNET`, `ACCESS_NETWORK_STATE`, …) vérifiée en CI.
3. **Liste blanche** de `uses-permission` vérifiée en CI sur le manifeste fusionné de chaque variante.

## Décision

- Une tâche Gradle `verify<Variant>Permissions` est enregistrée dans `app/build.gradle.kts` par `androidComponents.onVariants`. Elle lit l'artefact `SingleArtifact.MERGED_MANIFEST` et échoue si un `uses-permission` (ou `uses-permission-sdk-23`) sort de la liste blanche. La tâche agrégée `verifyPermissions` est rattachée à `check`.
- Liste blanche v1 :
  - `android.permission.VIBRATE` ;
  - `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (ajoutée par androidx.core, sans rapport avec le réseau).
- Ajouter une permission à la liste (par exemple `POST_NOTIFICATIONS` en v1.1) exige de mettre à jour cet ADR.
- En plus, un test de la tâche vérifie qu'un manifeste contenant `INTERNET` est bien rejeté.
- Pas de bibliothèque d'analytics ni de crash reporting, et aucun SDK tiers ayant un usage réseau.

## Conséquences

- Le garde-fou s'applique en local (`./gradlew check`) comme en CI.
- Si une montée de version de dépendance ajoute une permission, la CI casse : on la retire alors avec `tools:node="remove"` dans le manifeste et on le justifie dans le commit.
- La sauvegarde automatique Android (`allowBackup`) n'a pas besoin de permission, mais elle copie les données hors de l'appareil. Décision D26 (2026-10-16) : seul le transfert d'appareil à appareil est autorisé (`dataExtractionRules` sans `cloud-backup` ; `fullBackupContent` vide pour API 26-30). Rien ne part vers un cloud.

## Complément (2026-10-17, décisions D51, D52)

- La v1 reste sans permission réseau ni audio. La saisie vocale passe par la dictée du clavier (aucune permission pour l'app).
- Le LLM optionnel prévu après la v1 demandera `INTERNET` : il fera l'objet d'un nouvel ADR qui amende celui-ci (liste blanche de `verifyPermissions`, option désactivée par défaut, clé fournie par l'utilisateur et stockée chiffrée).
