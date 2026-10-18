# Pilotage — état du projet

> Fichier tenu par l'orchestrateur. À lire en début de session pour reprendre le travail.
> Dernière mise à jour : 2026-10-16.

## État actuel

- Dépôt : `main`, **aucun commit** ; remote `origin` = `git@github.com:qnicoud/jdr-dm-improv-skill-dev.git`.
- Fait : `SPEC.md` (fourni), `CLAUDE.md` (config orchestrateur), 7 agents dans `.claude/agents/`, dossiers `docs/{research,adr,ux,mockups,qa}`.
- Phase 0 terminée : `docs/research/01-stack-technique.md`, `02-pedagogie-exercices.md`, `03-existant-ux.md`.
- Pas encore de code Android.
- **Artefact de pilotage** : https://claude.ai/artifact/1asNFgMQPYgd7VobQUKucz (source : `docs/pilotage.html`, republier ce même fichier pour garder l'URL). Les choix de l'utilisateur sont stockés dans la base de l'artefact, collection `decisions`, un document par décision (`{choice, note, updatedAt}`) → les relire avec `ArtifactData list` avant d'écrire les ADR. La section « Maquettes » attend les écrans de la phase 2.
- Phase 1 terminée (2026-10-16) : D1–D24 tranchées dans l'artefact ; SPEC.md révisée en conséquence.
- **Phase 2** : `architecte` terminé (ADR 0001–0013, `docs/architecture.md` avec découpage L1–L12 au §14) ; `ux-designer` terminé (`docs/ux/parcours.md`, `design-system.md`, `brief-maquettes.md`).
- D25–D42 tranchées le 2026-10-16 : **toutes les recommandations retenues** ; reportées dans `docs/architecture.md` §15, ADR 0004 et 0010, `docs/ux/parcours.md` §8.
- Phase 2 terminée (2026-10-16) : 10 maquettes dans `docs/mockups/` (index : `docs/mockups/index.md`), intégrées à l'artefact (version 4, section « Maquettes », fichiers publiés sous `mockups/`).
- Phase 3 démarrée : **L1 vert** (versions de l'ADR 0001, aucun repli), APK debug installée sur le téléphone de l'utilisateur (Galaxy Z Flip3, `R5CTA23GG8K`) ; **contenu v1 écrit** (10 tables, `docs/research/04-notes-contenu.md`). Commit initial fait en 9 commits logiques (règles git : `CLAUDE.md` § Git).
- D43–D48 tranchées le 2026-10-16 (recommandations retenues, reportées dans ADR 0007/0008/0009 et `parcours.md` §9) : D43 plugin ktlint, D44 mots-clés des répliques, D45 contre-stéréotype Status, D46 texte du jour de grâce (Progrès), D47 saisie à 200 %, D48 panneau Connect the Story > 35 %.
- **Fusionnés dans `main`** (2026-10-16, `--no-ff`, check vert après chaque fusion) : L4 détection, L6 historique, L2 tables/tirage, L3 minuteur/réglages/signal. Chaque lot : revue `kotlin-reviewer` → corrections → re-check. Fix `d77a523` : `.gitignore` masquait `build-logic/src` (paquet nommé `build`).
- Arbitrages orchestrateur (L6) : « contenu » = ≥ 1 réponse non vide (trim) ; 24 h comptées depuis la dernière activité ; compteur de sessions = sessions avec contenu (terminées ou inachevées), comme les jours pratiqués (ADR 0011, complément). Vibration de fin en `USAGE_ALARM` (L3).
- **Fusionnés ensuite** (2026-10-17) : L7 navigation/thème/accueil (réglages + compteur branchés), corpus doré + réglage des mots-clés D44 (rappel 0,983, précision 0,918, `docs/qa/detection.md`), L11 stats/série/séance du jour (ADR 0011 aligné sur D35 : grâce hebdomadaire rechargée le lundi).
- **Fusionnés ensuite** (2026-10-17) : L5 moteur/écran d'exercice générique, L9 historique/détail/reprise, L8 exercices 1-2-6 avec détection et surlignage, L10 exercices 3-4-5-7. **Les 7 exercices sont jouables** ; APK installé sur le téléphone de l'utilisateur.
- Arbitrages orchestrateur (2026-10-17) : bilan intégré à l'écran d'exercice ; réplique de Connect the Story détectée (SPEC) ; panneau Connect the Story replié à la 1re ouverture du clavier puis laissé au choix de l'utilisateur (D48) ; **comptage : session terminée = comptée même sans texte (Status Numbers oral), inachevée = comptée seulement avec contenu** (ADR 0011).
- **Test utilisateur n°1** (2026-10-17, `commentaires-test-utilsateur.md`) : crash Connect the Story corrigé (`minLines 8 > MAX_LINES 6` dans `FieldList`) + `AllExercisesSmokeTest` (ouvre/démarre/saisit les 7 exercices) ; retours UI appliqués (barre de minuteur arrondie, Core Image : question en cours au-dessus du champ, liste dépliable, questions numérotées ; aide des options de Status Numbers). Fusionnés dans `main`, APK installé.
- 6 propositions de logo (`docs/ux/logo/`) dans l'artefact (version 5, section « Logo »). **Décisions ouvertes D49–D53** : concept de logo, fond d'icône, LLM, saisie vocale, couleur du thème.
- Git : ménage fait (worktrees et branches fusionnées supprimés, historique intact via les commits de fusion `--no-ff`). Seule branche : `main`.
- D49–D53 tranchées le 2026-10-17 (toutes les recommandations) : logo d20 sablier sur fond brique ; LLM optionnel après la v1 (clé utilisateur ou modèle local, nouvel ADR à venir) ; dictée du clavier en v1 ; réglage de palette en v1. Reportées dans SPEC, ADR 0010 et 0013.
- **Test utilisateur n°2** (journal `docs/qa/tests-utilisateur.md`, désormais commité) → corrigés et fusionnés : signal de fin inaudible (Samsung route `USAGE_ASSISTANCE_SONIFICATION` vers le volume système à 0 → bip en `USAGE_MEDIA`, vibration 2×250 ms pleine amplitude) ; panneau replié de Connect the Story réduit à une ligne. Ajoutés : icône adaptative d20 (+ monochrome, logo final vectorisé), écran Réglages et À propos, palettes Parchemin/Forêt/Nuit/Brume/couleurs du téléphone, écran de démarrage tenu jusqu'à la lecture de la palette.
- **Test utilisateur n°3** (journal) → force des vibrations + étiquette « Préparation » (L12a).
- **L12 terminé** (2026-10-17) : L12a Réglages (intensité des vibrations, apparence clair/sombre/système, réduction des animations, export lisible, « Tout effacer » avec saisie EFFACER) ; L12b (S07 fin de séance du jour et enchaînement, Decision Point « Pause faite : lancer 30 s » + question de bilan, « Passer cette question » Core Image, bascules selon le statut, icônes de sens ; **migration Room v1 → v2** testée, données de l'utilisateur conservées ; ADR 0004 : toute évolution de schéma passe par une migration testée) ; L12c (R8, version `1.0.0-rc1`, règles de sauvegarde testées, CI `assembleRelease`, `docs/qa/release.md`).
- Revue de sécurité : **prête pour la rc1**, 4 points mineurs dont 3 corrigés (WAL vidé après effacement, export partiel supprimé, domaines de transfert restreints) ; reste : épingler les actions CI par SHA (Dependabot), plus tard.
- Audit d'accessibilité automatisé (10 écrans + 7 exercices, polices 1x/2x) : aucun défaut ; couverture Kover 98 %. Boutons M3 de 40 dp visuels / 48 dp tactiles conservés (convention Material). Livrables : `docs/qa/checklist-rc1.md`, `docs/qa/rapport-rc1.md`.
- **Recette rc1** (2026-10-17, saisie dans l'artefact, collection `recette`) : 96 OK / 4 KO, verdict « corrections nécessaires » → `docs/qa/recette-rc1-resultats.md`. Corrigé et fusionné en 3 lots revus : R1a (recochage sur nouvelle occurrence, écran Prêt en lecture seule, statut selon le rang, choix « jouée en premier » de Decision Point), R1b (feuille de durée sur l'écran Prêt, zone épinglée compacte en paysage, découpage d'`ExerciseScreen`), R1c (lien bijoux, engrenage sur les 3 onglets, coche d'export, saisie EFFACER stable, splash aux couleurs de la palette et du mode, 1 s minimum). Les 11 critères concernés et le verdict sont remis « à tester » dans l'artefact.
- Dette notée : `ExerciseViewModel.kt` à 420 lignes (à découper) ; 3 points LOW de la revue R1a (libellé d'export en dur, cas limite ADR 0008, clé `first_pick` orpheline) ; épinglage SHA des actions CI.
- Recette rc1 close (2026-10-18) : 100/100 OK après deux derniers ajustements (icônes de catégorie de Connect the Story ; compteur de bijoux à 0 sans lien). Tag local `v1.0.0-rc1` posé sur `main` (non poussé).
- `main` et le tag `v1.0.0-rc1` poussés sur GitHub (2026-10-18, accord de l'utilisateur). APK signé par l'utilisateur (`*.apk`, `*.idsig` désormais ignorés).

## v1.1 — périmètre (lancé le 2026-10-18)

1. **Exercices** (recherche B, `docs/research/02-pedagogie-exercices.md` §84-86) : « Échec qui avance » (4 min, 3 complications de types différents + préférée), « Règle des 3 indices » (5 min, 3 indices objet/PNJ/lieu pour une conclusion), « Oui, et… » (5 offres de joueurs, réponse « oui, et » + ajout ; option « bloquer d'abord »). Nouvelles tables JSON (≥ 30 entrées).
2. **Rappels** optionnels, désactivés par défaut (D19) : notification locale, permission `POST_NOTIFICATIONS` → ADR amendant l'ADR 0010.
3. **Import des sessions** : réimporter un export (mêmes données, sans doublon) — nécessaire pour passer de la version de développement à l'APK signé sans perdre l'historique. Format d'export à rendre réimportable (JSON versionné) en plus du texte lisible.
4. **Gestion de la langue** (demande de l'utilisateur) : langues cibles, réglage de langue dans l'app (sélecteur par app d'Android 13+), traduction des textes (`strings.xml`), **contenu des tables par langue**, **détection des mots-clés par langue** (normalisation, corpus doré par langue), formats de date/nombre, ordre de bascule. À trancher : quelles langues, et si la v1.1 livre l'architecture seule ou aussi une traduction.
5. Dette : découper `ExerciseViewModel.kt` (420 lignes) ; 3 LOW de la revue R1a ; épinglage SHA de la CI.

- Conception v1.1 faite : `docs/ux/parcours-v1.1.md`, `docs/ux/brief-maquettes-v1.1.md`, ADR 0014 (rappels), 0015 (sauvegarde/import), 0016 (langues), 0017 (moteur v1.1), `architecture.md` §16 (lots V0 dette, V1 moteur, C1 tables FR, V2a/b/c exercices, V3 rappels, V4 sauvegarde/import, V5 langues + Room v3, V6 release ; anglais E1–E4 optionnels). Vagues : (V0, V3, V4, C1) → (V1, V5) → V2 → V6.
- **Décisions ouvertes D54–D70** dans l'artefact (langue, sélecteur — désaccord UX/architecte —, complications, Oui et…, rappels, export/import, passage à l'APK signé, séance du jour, sauvegarde des réglages).
- D54–D70 tranchées (toutes les recommandations ; D55 = sélecteur in-app AppCompat). Vague 1 lancée : V0, V3, V4, C1.
- C1 fusionné : `failure_situations` (36), `complication_types` (6, exception « vocabulaire » au minimum de 30 à appliquer dans `TablesContentTest` en V1), `clue_conclusions` (34), `player_offers` (46). Définitions des types de complication → `strings.xml` en V2a. Tables à la racine de `assets/tables/` (déplacement vers `fr/` en V5).
- **Vague 1 v1.1 fusionnée** (2026-10-18) : V0 (ViewModel d'exercice découpé à 284 lignes en 3 contrôleurs testés ; test instable corrigé : `snapshotFlow` global + `Dispatchers.Main` de test ; actions CI épinglées par SHA + Dependabot), C1 (tables), V3 (rappels, AlarmManager inexact, canal bas, `POST_NOTIFICATIONS` + `RECEIVE_BOOT_COMPLETED`), V4 (sauvegarde JSON réimportable, import atomique avec aperçu et annulation persistée ; revue sécurité non bloquante, corrigée : 8 Mio, OOM, session importée jamais reprenable, bidi, écart par session). Installé sur le téléphone en mise à jour.
- **Vague 2 fusionnée** (2026-10-18) : V1 (extensions génériques du moteur : champs liés au tirage, révélation progressive, grille selon les options, libellé de choix, repère par minute ; tables C1 enregistrées avec règle « vocabulaire fermé » ; plan du jour figé D69) et V5 (14 tables dans `assets/tables/fr/`, détection et corpus par langue, **schéma Room v3** `contentLocale` avec migration testée, sauvegarde v1 toujours importable, formats selon la langue du contenu ; AppCompat D55 reporté à la 2e langue, cf. ADR 0016). Téléphone migré en v3. **Ne plus installer de build antérieur à cette fusion (rétrogradation de schéma).**
- **Vague 3 fusionnée** (2026-10-18/19) : V2b Règle des 3 indices (+ icône `npc`, étiquette « Nouveau » qui disparaît après la 1re session comptée), V2a Échec qui avance (+ catalogue `ExerciseId` dérivé du registre, définitions des types sur l'écran Prêt, repli pour un exercice inconnu, **correctif : la frappe juste avant « Terminer » était perdue pour tous les exercices**), V2c Oui, et… (offres une à une, repère par minute, mode bloquer, touche Suivant, détail/export par offre). Installé sur le téléphone.
- **V6 en cours** : `testeur` (audit a11y étendu, checklist v1.1, rapport) ∥ `security-reviewer` (v1.1) ∥ `dev-android` (version 1.1.0, procédure de passage à l'APK signé dans `release.md`, SPEC v1.1). Ensuite : intégrer la checklist v1.1 dans l'artefact, recette par l'utilisateur, tag `v1.1.0`, passage à l'APK signé.

## Décisions (issues de la phase 0) — TRANCHÉES le 2026-10-16

Toutes les recommandations retenues, **sauf D24 : pas de vérification Play Store** (on avance sur le positionnement proposé).

Source entre crochets : A = stack, B = pédagogie, C = existant/UX. **Gras** = recommandation.

### Technique
| # | Question | Options | Recommandation |
|---|---|---|---|
| D1 | Navigation [A] | Navigation 3 1.2.0 / Navigation Compose 2.10.2 | **Navigation 3** (SPEC dit « Navigation Compose » → amender) |
| D2 | Base de données [A] | Room 3.0.3 / Room 2.8.5 | **Room 3**, repli 2.8.5 si build de fumée KO |
| D3 | AGP [A] | 9.4.0 / 9.3.1 | **9.4.0 + Gradle 9.7.x**, repli 9.3.1 |
| D4 | JDK [A] | 27 / 21 / 17 | **21** via `gradle-daemon-jvm.properties` |
| D5 | targetSdk [A] | 36 / 37 | **36 en v1** (compileSdk 37, minSdk 26) |
| D6 | Minuteur en arrière-plan [A] | horodatage seul / service premier plan / alarme | **horodatage seul en v1** |
| D7 | Son de fin [A] | SoundPool / ToneGenerator / vibration seule | **SoundPool** (mode sonnerie normal), vibration réglable ; bip original/CC0 à produire |
| D8 | Framework de test [A] | JUnit 4 / Jupiter 6 + Vintage | **Jupiter 6 + Vintage** ; JUnit 4 en `androidTest` |
| D9 | DI [A] | Hilt / Koin / manuelle | **manuelle** |

### Détection de mots-clés (Connect the Story, Sensory)
| # | Question | Options | Recommandation |
|---|---|---|---|
| D10 | Principe [A, B] | auto + manuel / manuel seul | **auto (normalisation) avec cochage manuel qui fait foi** |
| D11 | Racinisation [A] | Snowball / règles maison / aucune | **mots-clés du rédacteur d'abord**, Snowball si le corpus de test le justifie |
| D12 | Fautes de frappe [A] | activée / désactivée / par entrée | **désactivée par défaut, activable par entrée** (mots ≥ 8 lettres) |
| D13 | Cases cochées auto [A] | recalcul pur / verrouillage / recalcul + bascule manuelle | **recalcul + bascule manuelle** |
| D14 | Champ `keywords` [A] | obligatoire / facultatif sur `story_elements`, `sensory_words` | **obligatoire** (brief rédacteur) |

### Produit / UX
| # | Question | Options | Recommandation |
|---|---|---|---|
| D15 | Série de jours [B, C] | stricte / avec jour de grâce, sans reset punitif / aucune | **jour de grâce**, compteur de séances mis en avant |
| D16 | Jour de la série [A] | minuit local / jour logique à 04:00 | **minuit local en v1** |
| D17 | Durée des exercices (WCAG 2.2.1) [C] | réglable + option sans minuteur / fixe | **réglable + option sans minuteur** |
| D18 | Auto-évaluation [B, C] | aucune / 3 niveaux optionnels / note | **3 niveaux optionnels** (+ grille 2-3 questions, « bijou ») |
| D19 | Notifications de rappel [B, C] | aucune / optionnelles, off par défaut | **optionnelles en v1.1** (POST_NOTIFICATIONS) |
| D20 | Séance du jour [B] | imposée / suggérée, remplaçable | **suggérée, remplaçable** |
| D21 | Champ `difficulty` dans les tables [B] | dès v1 / v1.1 | **dès v1** |
| D22 | Table de situations pour One-Page [B] | oui / non | **oui, 30 entrées** |
| D23 | Exercices v1.1 [B] | A+B+C / A+B+D / A+B+C+D | **Échec qui avance, 3 indices, Oui et…** ; PNJ éclair en v1.2 |
| D24 | Vérif. manuelle Play Store avant de figer le positionnement [C] | oui (~30 min) / non | **oui** (par l'utilisateur) |

### Corrections à reporter dans la SPEC (pas de choix, juste à faire)
- Status Numbers : « d'après Johnstone » (le jeu de cartes 1-10 est une adaptation, pas un exercice d'*Impro*).
- Decision Point : reformuler la consigne (« la première option doit déjà être bonne », cf. *take-the-first*).
- Durées de Constraint Box, Core Image, Status, One-Page : choix maison, non sourcés.
- Positionnement : « salle de sport du MJ », pas un générateur de table.

### Risques mis à jour
- KSP + Kotlin 2.4.21 + AGP 9.4 non confirmés par une source → **build de fumée en tout premier lot**.
- Gradle sous JDK 27 impossible avant 9.8 → JDK 21 obligatoire.

## Plan validé

| Phase | Contenu | Agents | Statut |
|---|---|---|---|
| 0 — Recherche | A stack technique, B pédagogie + exercices complémentaires, C existant/UX | `chercheur` ×3 en parallèle | **terminée** (2026-10-16) — décisions à trancher ci-dessous |
| 1 — Cadrage | Artefact de pilotage : synthèse, périmètre v1, décisions à trancher, avancement | orchestrateur | **terminée** (2026-10-16) |
| 2 — Conception | Architecture + ADR + découpage ∥ parcours + design system → maquettes HTML intégrées à l'artefact | `architecte` ∥ `ux-designer` → `maquettiste` | conception terminée, maquettes **en cours** |
| 3 — Réalisation | Ordre SPEC : squelette → minuteur + tirage → ex. 1, 2, 6 → Room + historique → ex. 3, 4, 5, 7 → stats. Contenu JSON en parallèle. Chaque lot : dev → test → revue (`kotlin-reviewer`) → commit après accord | `dev-android`, `testeur`, `redacteur-contenu` | **démarrée** (L1 + contenu) |

## Risques identifiés

- **Élevé** : détection approximative de mots-clés en français (flexions, faux positifs) → prototyper tôt.
- **Moyen** : minuteur fiable (arrière-plan, rotation) ; volume de contenu original (~11 tables × 30 entrées).
- **Faible** : JDK système = OpenJDK 27, probablement trop récent pour AGP → toolchain JDK 17/21.

## Briefs de la phase 0

Chaque agent écrit uniquement son livrable et renvoie un compte rendu ≤ 250 mots avec les lignes `DÉCISION:` ouvertes. Pas de code.

### A — Stack technique → `docs/research/01-stack-technique.md`
Objectif : stack Android à jour (oct. 2026) et solutions aux points délicats. Entrées : `SPEC.md`, `CLAUDE.md` (env : JDK 27, android-37, build-tools 36.0.0, pas de Gradle global).
1. Versions stables compatibles : Kotlin, AGP, Gradle, Compose BOM, Material 3, Navigation Compose (type-safe ? Navigation 3 ?), Room (Room 3 / KSP), Lifecycle, kotlinx.serialization, coroutines ; extrait `libs.versions.toml` ; compileSdk/targetSdk vs minSdk 26 (à remonter ?).
2. JDK : Gradle/AGP sous JDK 27 ? toolchain 17/21, `org.gradle.java.home`, foojay.
3. DI : Hilt vs Koin vs manuelle.
4. Minuteur : rotation, arrière-plan, écran verrouillé ; vibration + son discret (VIBRATE, ToneGenerator/SoundPool, mode silencieux) ; service/notification ou horodatage de fin.
5. Détection de mots-clés FR (accents, casse, pluriels, féminins, conjugaisons) sans dépendance lourde : normalisation Unicode, Snowball French, distance d'édition ; surlignage temps réel en Compose (VisualTransformation / OutputTransformation + TextFieldState).
6. Tables JSON dans `assets/` (kotlinx.serialization), validation de schéma au build ou en test.
7. Outillage : JUnit 5, Turbine, Robolectric, tests UI Compose, Kover, ktlint/detekt, CI GitHub Actions.
8. Série de jours consécutifs (java.time, fuseau) et minSdk 26.

### B — Pédagogie et exercices → `docs/research/02-pedagogie-exercices.md`
Objectif : sourcer les 7 exercices et proposer des exercices complémentaires. Inspiration : vidéo Edariad « How to Get Better at DMing Crazy Fast » (https://www.youtube.com/watch?v=RhmlT8crkG4) — chercher résumé/transcription.
1. Pour chaque exercice : sources d'origine (Crazy 8s GV, Johnstone *Impro*, développement motivique, decision training…), améliorations suggérées (durées, consignes, variantes, progression), pièges.
2. Pratique délibérée : feedback, répétition espacée, difficulté progressive, auto-évaluation, relecture ; feedback possible sans IA ni serveur.
3. 6 à 10 exercices complémentaires (voix de PNJ, description en 3 détails, noms, rythme/coupes, « oui, et… », conséquences d'échec, résumé de session, cliffhangers, règle des 3 indices, factions…) : discipline, compétence, déroulé, contrainte, durée, saisie, contenu aléatoire, complexité S/M/L, source ; classement valeur/effort, top 2-3 pour v1.1.
4. Fonctionnalités transverses (séance du jour, difficulté progressive, défis hebdo) hors ligne, sans IA.

### C — Existant et UX → `docs/research/03-existant-ux.md`
Objectif : apps/outils proches, bonnes idées, manques, positionnement. Entrée : `SPEC.md`.
1. Apps MJ/impro (Play Store, web, itch.io) : générateurs, impro théâtrale, écriture sous contrainte / writing sprints, pratique créative quotidienne — fonction, modèle, hors ligne ?, forces, manques, avis.
2. Patterns UX : minuteur de sprint (fin sans brutalité), saisie rapide sous pression, journal de pratique, stats/séries sans culpabilisation, tirage façon carte.
3. Ergonomie/accessibilité de la saisie longue sur mobile.
4. Positionnement + 3 à 5 recommandations pour l'UX designer.

## Journal

- 2026-10-16 — Cadrage initial, création des agents et de `CLAUDE.md`. Les agents projet n'étaient pas chargés dans la session où ils ont été créés : redémarrage de session nécessaire avant la phase 0.
- 2026-10-16 — Phase 0 terminée (3 `chercheur` en parallèle, agents dédiés chargés correctement). 24 décisions consolidées (D1–D24), en attente de l'utilisateur.
- 2026-10-16 — Phase 2 : ADR 0001–0013, architecture, docs UX livrés ; D25–D42 tranchées (recommandations). Lancement parallèle maquettiste + rédacteur + dev-android L1.
