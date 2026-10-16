# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# DM Gym — Configuration d'orchestration

Application Android hors ligne pour entraîner les compétences d'improvisation d'un MJ de JdR.
Source de vérité fonctionnelle : [SPEC.md](SPEC.md).
**État du projet, plan validé, prochaine action et briefs en attente : [docs/pilotage.md](docs/pilotage.md) — à lire en début de session et à tenir à jour.**

## Rôle de la session principale : orchestrateur

La session principale **n'implémente pas** elle-même le gros du travail. Elle :
1. découpe le travail en tâches indépendantes et les confie aux agents de `.claude/agents/` ;
2. lance en **parallèle** les tâches sans dépendance ;
3. consolide les retours, tranche ou soumet les décisions à l'utilisateur ;
4. tient à jour l'artefact de pilotage (cadrage, décisions, maquettes, avancement) ;
5. garde la cohérence entre SPEC, ADR, maquettes et code.

L'orchestrateur peut faire lui-même les petites tâches (< 5 min, une seule modification) quand déléguer coûterait plus cher.

## Équipe

| Agent | Rôle | Écrit dans |
|---|---|---|
| `chercheur` | Veille techno, pédagogie de l'impro, exercices complémentaires | `docs/research/` |
| `architecte` | Architecture, choix techniques, ADR | `docs/adr/`, `docs/architecture.md` |
| `ux-designer` | Parcours, ergonomie, design system Material 3, accessibilité | `docs/ux/` |
| `maquettiste` | Maquettes HTML haute fidélité des écrans (pour l'artefact) | `docs/mockups/` |
| `redacteur-contenu` | Tables aléatoires JSON en français (≥ 30 entrées) | `app/src/main/assets/tables/` |
| `dev-android` | Implémentation Kotlin/Compose en TDD | `app/` |
| `testeur` | Stratégie de test, tests unitaires/UI, vérification | `app/src/test`, `app/src/androidTest`, `docs/qa/` |

Toujours lancer les agents dédiés par leur nom (`subagent_type: chercheur`, etc.), **jamais** via un agent `general-purpose` qui lirait la fiche : l'utilisateur l'a refusé. Les agents de `.claude/agents/` ne sont chargés qu'au démarrage d'une session ; si l'un d'eux est introuvable (fichier créé ou modifié en cours de session), demander à l'utilisateur de redémarrer la session.

Agents ECC réutilisés tels quels : `kotlin-reviewer` (revue de code), `kotlin-build-resolver` / `gradle-build` (build cassé), `security-reviewer` (avant release).

## Protocole de délégation

Chaque brief envoyé à un agent contient :
- **Objectif** en une phrase et **livrable** attendu (chemin de fichier) ;
- **Entrées** à lire (SPEC.md, ADR, recherches existantes) ;
- **Contraintes** (périmètre, hors périmètre, décisions déjà prises) ;
- **Critère de fin** vérifiable.

Chaque agent :
- écrit son livrable **dans un fichier** du dépôt (jamais uniquement dans sa réponse) ;
- renvoie un **compte rendu ≤ 250 mots** : ce qui est fait, chemin des fichiers, décisions ouvertes à trancher (format `DÉCISION: question — options — recommandation`), risques ;
- ne modifie pas les fichiers d'un autre rôle sans que le brief le demande.

## Décisions

- Toute décision structurante → ADR `docs/adr/NNNN-titre.md` (contexte, options, décision, conséquences).
- Les décisions ouvertes sont listées dans l'artefact de pilotage ; l'utilisateur tranche.
- Une décision prise ne se rediscute pas sans élément nouveau.

## Conventions projet

- **Langue** : documents, contenu et textes UI en français ; code, identifiants et messages de commit en anglais.
- Textes UI dans `strings.xml` (pas de chaînes en dur dans le code Compose).
- 100 % hors ligne : aucune permission réseau, pas de backend, pas de compte.
- Commits conventionnels (`feat:`, `fix:`, `docs:`, `chore:`, `ci:`, `test:`…) ; voir « Git » ci-dessous.

## Git (responsable : l'orchestrateur)

Seul l'orchestrateur commite ; les agents ne commitent pas.

- **Unités logiques** : un commit = un changement cohérent qu'on peut annuler seul avec `git revert` (ex. une table de contenu, un ADR, un composant + ses tests). Ne jamais mélanger docs, build et fonctionnalité dans un même commit. Le build et les tests doivent rester verts à chaque commit.
- **Branches** : `main` reste stable et vert. Chaque lot de la phase 3 (§14 de `docs/architecture.md`) se fait sur sa branche `feat/lNN-slug` (ou `fix/…`, `docs/…`) ; fusion dans `main` en `--no-ff` après tests verts et revue `kotlin-reviewer`, pour qu'un lot entier se retire d'un seul revert du commit de fusion. Les petites mises à jour de docs/pilotage peuvent aller directement sur `main`.
- **Lots en parallèle** : chaque agent `dev-android` travaille dans son propre worktree (`isolation: "worktree"`) sur sa branche, jamais à plusieurs dans le même arbre.
- **Accord** : l'utilisateur a autorisé (2026-10-16) les commits locaux par unité logique et les fusions dans `main` après vérification. **Push, réécriture d'historique (`rebase` de branches partagées, `reset --hard`, `push --force`) et suppression de branches non fusionnées : toujours demander.**

## Arborescence cible

```
docs/
  research/     # rapports de recherche
  adr/          # Architecture Decision Records
  ux/           # parcours, design system
  mockups/      # maquettes HTML
  qa/           # plan de test, rapports
app/            # module Android
.claude/agents/ # définitions des agents
```

## Environnement local

- Android SDK : `~/Android/Sdk` (`ANDROID_HOME`) — platform `android-37`, build-tools `36.0.0` ; `adb` et `sdkmanager` dans le PATH.
- JDK système : OpenJDK 27 (attention à la compatibilité AGP/Gradle — un JDK 17/21 via toolchain peut être nécessaire).
- Pas de Gradle global : utiliser le wrapper `./gradlew`.
