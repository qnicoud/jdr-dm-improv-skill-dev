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
- **En cours (2026-10-16)** : 6 agents en worktree, chacun sur sa branche — `feat/l2-tables-engine`, `feat/l3-timer-settings` (crée `core/settings`), `feat/l4-detection` (moteur + test de mesure), `test/l4-detection-corpus` (`testeur`, corpus doré), `feat/l6-room-history`, `feat/l7-navigation-home` (réglages provisoires, branchement après fusion de L3).
- **Prochaine action** : à chaque retour → vérifier (`./gradlew check`), revue `kotlin-reviewer`, commits logiques sur la branche, fusion `--no-ff` dans `main` ; aligner le format du corpus entre L4 et le testeur ; petite tâche de branchement L7 ↔ L3 (réglages) ; redéployer sur le téléphone après L7 ; ensuite L5.

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
