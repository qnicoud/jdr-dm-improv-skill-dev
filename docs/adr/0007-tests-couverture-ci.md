# ADR 0007 — Framework de test, couverture et intégration continue

- Statut : acceptée (2026-10-16)
- Décision de pilotage : D8
- Source : `docs/research/01-stack-technique.md` §7

## Contexte

Toute la logique métier (tirage, minuteur, détection, série, séance du jour, validation des tables) est en Kotlin pur. La règle du projet exige 80 % de couverture et du TDD. Les tests Compose et Robolectric reposent sur des règles JUnit 4.

## Options

1. JUnit 4 partout (simple, mais API vieillissante).
2. **JUnit Jupiter 6 + moteur Vintage** en test local, JUnit 4 en test instrumenté.
3. Plugin `de.mannodermaus.android-junit5` : aucune source ne confirme sa compatibilité avec AGP 9 et JUnit 6. Écarté.

## Décision

- **Tests locaux (`src/test`)** : JUnit Platform via `testOptions.unitTests.all { it.useJUnitPlatform() }` et `isIncludeAndroidResources = true`.
  - Logique pure : **Jupiter 6.1.x** (`@Test`, `@ParameterizedTest` pour les corpus).
  - Robolectric 4.17 (DAO Room, `AssetTableSource`, écrans Compose avec `createComposeRule`) : JUnit 4 exécuté par **`junit-vintage-engine`**.
  - Coroutines : `kotlinx-coroutines-test` (`runTest`, `StandardTestDispatcher`). Flows : **Turbine**.
- **Tests instrumentés (`src/androidTest`)** : JUnit 4 + `AndroidJUnitRunner`. Ils sont limités au strict nécessaire : rotation de l'écran d'exercice, vibration et son sur appareil réel. Ils ne tournent pas en CI en v1.
- **Repli** : si la plateforme JUnit pose problème avec AGP 9.4, on passe à JUnit 4 partout. Ce changement reste local aux dépendances de test.
- **Couverture : Kover 0.9.11**, règle `verify` à **80 % de lignes** sur le périmètre « domaine » :
  - inclus : `*.core.*`, `*.data.*` (repositories, mappers), `*.feature.**.*ViewModel*`, `*.feature.exercise.*.*Definition*` ;
  - exclus : `*.ui.*`, fonctions `@Composable`, `*_Impl`, `*Database_*`, `BuildConfig`, `*.platform.*` (code purement Android, couvert par des tests manuels ou instrumentés).
- **Style** : ktlint 1.8.0. L'intégration Gradle (Spotless ou plugin dédié) sera choisie au lot 1 selon sa compatibilité avec AGP 9. On n'adopte pas detekt tant que la 2.0 n'est pas stable. Android Lint (`lintDebug`) est activé avec `warningsAsErrors` sur `HardcodedText` et `MissingPermission`.
- **CI GitHub Actions** (`.github/workflows/ci.yml`, `ubuntu-latest`) : `actions/checkout@v7` → `actions/setup-java@v6` (Temurin 21) → `gradle/actions/setup-gradle@v6` → `./gradlew ktlintCheck lintDebug testDebugUnitTest koverVerify verifyPermissions assembleDebug`. Pas d'émulateur en v1.

## Conséquences

- Deux styles de test coexistent : Jupiter pour le pur, JUnit 4 pour Robolectric et Compose. La règle est simple : tout ce qui demande Android est en JUnit 4.
- `koverVerify` bloque la CI sous 80 % sur le domaine.
- Les tests de contenu (tables JSON) et le corpus doré de détection font partie de `testDebugUnitTest` : une table cassée ou une régression de détection fait échouer la CI.

## Complément (2026-10-16, décision D43)

Plugin ktlint retenu : `org.jlleitschuh.gradle.ktlint` 14.2.0 (ktlint 1.8.0), déjà en place depuis le lot 1.
