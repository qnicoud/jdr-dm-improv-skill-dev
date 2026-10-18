# Rapport QA — 1.1.0 (lot V6)

Branche `test/v6-qa`, partie de `ec86b50`. Recette manuelle : `checklist-v1.1.md` (à dérouler sur le Galaxy Z Flip3). Aucun appareil n'a été utilisé : rien n'a été installé, désinstallé ni écrit sur le téléphone.

## 1. Audit d'accessibilité automatisé

Fichiers : `app/src/test/java/io/github/qnicoud/dmgym/accessibility/`.

- `AuditTestBase.kt` (nouveau) : socle commun extrait de `AccessibilityAuditTest` (règle Compose, passes 1x puis 2x, parcours prêt / en cours / bilan d'un exercice, options d'exercice).
- `AccessibilityAuditTest.kt` : 7 exercices existants + **3 nouveaux** (Échec qui avance, Règle des 3 indices, Oui, et…) + Oui, et… en **mode « Bloquer d'abord »**.
- `AccessibilityAuditV11Test.kt` (nouveau) : section **Rappels** (désactivée, activée, pause + bloquée + refusée + aide « un jour »), écran d'**import S13** (choix, lecture, aperçu, rien à ajouter, résultat, les 7 erreurs de `ImportError`).
- `PseudoLocaleAuditTest.kt` (nouveau) : les 10 exercices, Rappels et une erreur d'import en **pseudo-locale `en-XA`** à 200 %.

Contrôles inchangés (cibles tactiles de 48 dp, libellés des nœuds cliquables, champs étiquetés, rôles des éléments à état, au moins un titre, aucun texte coupé en hauteur ou à ellipse à 2x). 1x et 2x pour chaque état.

### Pseudo-locale en-XA : faisable
Sous Robolectric (qualificatif `en-rXA`), à condition que les ressources de pseudo-locale existent dans le paquet de test. **Changement de build nécessaire** : `isPseudoLocalesEnabled = true` dans le type de build `debug` de `app/build.gradle.kts` (debug seulement, ne touche pas la release). Sans lui, la locale retombe sur le français sans erreur : le premier test de `PseudoLocaleAuditTest` échoue exprès dans ce cas (il vérifie que `import_title` est bien pseudo-localisé : `[Îḿþöŕţéŕ ðéš šéššîöñš one two three]`). Limite : en-XA allonge d'environ 30 % et accentue ; `ar-XB` (miroir RTL) n'est pas audité (l'app est française uniquement).

**Résultat : 0 défaut** sur tous ces états, à 1x et 2x, en français et en en-XA.

Information (non bloquante, identique à la rc1) : 82 mesures « taille visuelle < 48 dp » enregistrées par l'audit, toutes des boutons ou icônes Material 3 de 40 dp de côté (Retour, Réglages, Partager, Plus d'actions, Terminer, Commencer, Continuer, Remplacer, Voir mes bijoux). Leur zone tactile effective est étendue à 48 dp.

Limites : contraste, ordre de focus, annonces TalkBack, débordement en largeur, feuilles et dialogues (heure du rappel, remplacement de séance), listes paresseuses. Couvert par les sections 12 et 13 des checklists.

## 2. Défauts

| Gravité | Défaut | État |
|---|---|---|
| Corrigé | aucun trouvé par l'audit | |
| Ouvert, faible | Écart de documentation : `docs/architecture.md` §16.1 annonce une lecture bornée à **16 Mio** ; le code (`core/backup/BoundedText.kt`, `MAX_BYTES`) borne à **8 Mio** (commit `b432c8d`). Le message et le test de la checklist (5.12) utilisent 8 Mio. | à aligner par l'architecte |
| Ouvert, info | Cibles visuelles M3 de 40 dp (voir §1) | accepté (design system §7.1) |
| À confirmer sur appareil | Rappels sous Doze et Samsung « veille prolongée » ; permission au premier activation ; redémarrage | checklist §6 |

## 3. Suite de tests et couverture

`./gradlew check koverVerify` : **vert** (tests JVM, ktlint, lint, `verifyPermissions`, seuil Kover 80 %). 1337 tests JVM, 0 échec.

Couverture Kover (périmètre métier, données, ViewModels) : instructions **97,9 %** (23 186 / 23 684), branches 89,2 % (1 201 / 1 347), lignes 98,9 %, méthodes 97,7 %.

Paquets les plus bas : `core/smoke` 88,2 %, `core/tables` 90,7 %, `core/detection` 93,9 %, `data/history` 94,6 %, `data/settings` 95,7 %, `feature/backup` 96,0 %, `core/backup` 96,5 %. Nouveaux paquets v1.1 : `core/reminder` 100 %, `core/i18n` 100 %, `core/exercise/definitions` 100 %, `feature/settings` 99,2 %, `feature/exercise` 99,7 %. Tous au-dessus du seuil de 80 %.

## 4. Risques

- Rappels : comportement d'AlarmManager inexact (retard Doze, Samsung), non vérifiable en JVM ; à tester sur appareil (checklist §6).
- Migration Room v2 → v3 sur la vraie base : testée en JVM sur la chaîne 1 → 2 → 3 ; la mise à jour par-dessus la rc1 reste à faire à la main (checklist §10).
- Import : le sélecteur de fichiers système et les fournisseurs de documents tiers ne sont pas simulables ; fichier de plus de 8 Mio et fichier tronqué à vérifier à la main (5.10, 5.12).
- Passage à l'APK signé : la procédure est celle de `release.md` (autre agent) ; la section 11 de la checklist la valide, et une clé différente de celle de la rc1 interdit la mise à jour sans désinstaller (donc sans perdre l'historique) : sauvegarder d'abord.
- Les tests instrumentés (`connectedDebugAndroidTest`) n'ont pas été lancés : pas d'appareil utilisé par contrainte.
- Le changement `isPseudoLocalesEnabled` modifie `app/build.gradle.kts` : à fusionner avec le changement de version fait par l'autre agent (hunks distincts).
