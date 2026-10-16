# ADR 0001 — Stack, versions et module unique

- Statut : acceptée (2026-10-16)
- Décisions de pilotage couvertes : D3 (AGP), D5 (targetSdk), et la stack de la SPEC
- Source : `docs/research/01-stack-technique.md` §1

## Contexte

DM Gym est une petite app hors ligne (7 exercices, une dizaine d'écrans). Elle doit utiliser des versions stables et récentes, épinglées dans `gradle/libs.versions.toml`. La recherche A a relevé un écart de compatibilité : KGP 2.4.21 est testé jusqu'à AGP 9.3.1 et Gradle 9.7.0. Aucune source ne confirme non plus KSP 2.3.12 avec Kotlin 2.4.21.

## Options

1. AGP 9.4.0 + Gradle 9.7.x (le plus récent, légèrement hors de la fenêtre testée par KGP).
2. AGP 9.3.1 (dans la fenêtre KGP, une version de retard).
3. targetSdk 37 (Android 17 : audio en arrière-plan durci, orientation non verrouillable sur grand écran) ou 36 (exigence Play depuis le 31 août 2026).
4. Plusieurs modules Gradle (`:core`, `:data`, `:feature-*`) ou un seul module `app`.

## Décision

- **AGP 9.4.0 + Gradle 9.7.x** (9.7.0 par défaut, le plafond testé par KGP), avec **Kotlin intégré à AGP** : pas de plugin `org.jetbrains.kotlin.android`, options dans `kotlin { compilerOptions { } }`.
- **Repli : AGP 9.3.1** si le build de fumée (lot 1, voir `docs/architecture.md` §14) échoue pour une raison AGP/KGP.
- **compileSdk 37, targetSdk 36, minSdk 26.** `java.time` est natif, donc pas de desugaring.
- **Un seul module `app`**, découpé en paquets par fonctionnalité (voir `docs/architecture.md` §2). Ce découpage est sans coût, et le multi-module n'apporte rien à cette taille.
- Versions épinglées (toutes dans `libs.versions.toml`, aucune version en dur dans un `build.gradle.kts`) :

| Clé | Version | Clé | Version |
|---|---|---|---|
| agp | 9.4.0 | lifecycle | 2.11.0 |
| kotlin | 2.4.21 | navigation3 | 1.2.0 |
| ksp | 2.3.12 | room3 | 3.0.3 |
| composeBom | 2026.09.00 (ui 1.12.1, material3 1.4.0) | datastore | 1.2.1 |
| activityCompose | 1.13.0 | serialization | 1.11.0 |
| coreKtx | 1.19.1 | coroutines | 1.11.0 |
| junit (BOM) | 6.1.3 | turbine | 1.2.1 |
| robolectric | 4.17 | kover | 0.9.11 |
| ktlint | 1.8.0 | | |

Coordonnées vérifiées au lot 1 (2026-10-16) et ajoutées au catalogue : `androidx.sqlite:sqlite-framework` 2.7.1 (driver de Room 3), `junit:junit` 4.13.2 (pour Vintage), `org.junit.vintage:junit-vintage-engine` 6.1.3 (BOM), `androidx.test:core` 1.7.0, plugin ktlint `org.jlleitschuh.gradle.ktlint` 14.2.0 (ktlint 1.8.0). Wrapper Gradle : 9.7.0.

Le lot 1 vérifie les coordonnées marquées [P] dans la recherche (JUnit, Turbine, Robolectric, Kover, driver SQLite, `junit:junit` pour Vintage, `androidx.test`) avant de les figer.

- **Interdit tant que la plateforme 37.1 n'est pas installée** : Compose 1.13 et tout BOM postérieur à 2026.09.00.
- Material 3 Expressive (API `@ExperimentalMaterial3ExpressiveApi`) est exclu de la v1.

## Conséquences

- Le build de fumée est le premier lot, et les autres lots en dépendent.
- On ne monte de version qu'avec un nouvel ADR ou une mise à jour de celui-ci, après un build vert.
- On vérifie que `./gradlew buildEnvironment` résout bien KGP 2.4.21 et pas la KGP embarquée par AGP.
- Passer à targetSdk 37 demandera de tester sur grand écran et de revoir l'audio (ADR 0006).

## Résultat du lot 1 (build de fumée, 2026-10-16)

Les versions du tableau ci-dessus ont été utilisées **sans aucun repli** : AGP 9.4.0 + Gradle 9.7.0 + Kotlin 2.4.21 (Kotlin intégré) + KSP 2.3.12 + Room 3.0.3 (plugin `androidx.room3`) + Kover 0.9.11 + ktlint 1.8.0 + JUnit Jupiter 6.1.3 avec Vintage. `./gradlew clean check assembleDebug` est vert sous JDK 21 ; `buildEnvironment` résout `kotlin-gradle-plugin:2.4.21` ; le code Room (`DmGymDatabase_Impl`, `SmokeDao_Impl`) est généré par KSP et le schéma `1.json` est exporté.

Précisions d'exécution :
- `gradle/gradle-daemon-jvm.properties` est écrit à la main (`toolchainVersion=21`), car `updateDaemonJvm` exige un résolveur de toolchain absent (ADR 0002 : pas de foojay). Le wrapper démarre sous le JDK système (27 ici) et le démon sous le JDK 21 détecté. Le bloc `java { toolchain }` n'est pas utilisé dans `app` (`compileOptions` Java 17 suffit).
- Robolectric 4.17 (SDK 36+) exige `--add-exports=java.base/jdk.internal.access=ALL-UNNAMED` dans les `jvmArgs` des tests.
- Le plugin ktlint retenu est `org.jlleitschuh.gradle.ktlint` 14.2.0 (compatible AGP 9.4.0, vérifié).
- Les tests de la tâche `verifyPermissions` vivent dans le build inclus `build-logic/` ; la tâche `check` racine en dépend.
- La CI exécute `./gradlew check assembleDebug` : `check` regroupe `ktlintCheck`, `lintDebug`, `testDebugUnitTest`, `koverVerify`, `verifyPermissions` et les tests de `build-logic`.
