# Recherche A — Stack technique (octobre 2026)

> Auteur : chercheur. Date de la recherche : 2026-10-16. Périmètre : rapport uniquement, aucun code applicatif.
> Légende : **[S]** fait sourcé (lien) · **[P]** pratique répandue (non vérifiée sur une page officielle) · **[O]** opinion du chercheur.
> Les pages de référence d'API (AudioAttributes, ToneGenerator, SoundPool, OutputTransformation, TextFieldBuffer) étaient tronquées à la lecture : ce qui en dépend est marqué **[P]** et doit être validé par un prototype.

## Résumé

- Stack stable recommandée : Kotlin 2.4.21, AGP 9.4.0 (Kotlin intégré, pas de plugin `kotlin-android`), Gradle 9.7.x, Compose BOM 2026.09.00 (Material 3 1.4.0), Navigation 3 1.2.0, Room 3.0.3 via KSP, compileSdk 37, targetSdk 36, minSdk 26 inchangé.
- JDK 27 : Gradle ≥ 9.8 peut tourner dessus, mais AGP 9.4, Kotlin 2.4.21 et Robolectric ne l'annoncent pas. Utiliser le **JDK 21 déjà installé** (`/usr/lib/jvm/java-21-openjdk`) via `gradle-daemon-jvm.properties`.
- Minuteur par horodatage (`elapsedRealtime`) dans le ViewModel, sans service ni notification en v1 ; DI manuelle ; détection FR en trois couches (normalisation, mots-clés fournis par le rédacteur, racinisation Snowball) validée par un corpus de tests.
- Tables JSON : décodage strict kotlinx.serialization + tests d'invariants JVM (pas de validation au build).
- Risque principal : combinaison KSP 2.3.12 / Kotlin 2.4.21 / AGP 9.4.0 non confirmée par une source unique → build de fumée dès le squelette.

---

## 1. Versions stables compatibles

### Constats

| Composant | Version stable | Date | Source |
|---|---|---|---|
| Kotlin | 2.4.21 (correctif de 2.4.20) ; 2.5.0 prévu en décembre 2026 | 8 oct. 2026 | [kotlinlang releases](https://kotlinlang.org/docs/releases.html) |
| AGP | 9.4.0 ; exige Gradle ≥ 9.6.0, JDK ≥ 17, build-tools 36.0.0 ; compileSdk max = API 37 | sept. 2026 | [AGP release notes](https://developer.android.com/build/releases/gradle-plugin) |
| Gradle | 9.8.1 est la dernière version citée ; 9.7.0 est le plafond testé par KGP 2.4.20-21 | — | [Gradle compat](https://docs.gradle.org/current/userguide/compatibility.html), [Kotlin Gradle](https://kotlinlang.org/docs/gradle-configure-project.html) |
| Compose BOM | 2026.09.00 → ui/foundation 1.12.1, material3 1.4.0 | sept. 2026 | [BOM mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) (tableau aplati à la lecture, recoupé avec la [page des versions](https://developer.android.com/jetpack/androidx/versions)) |
| Material 3 | 1.4.0 stable ; 1.5.0-beta01 (7 oct. 2026) | oct. 2026 | [material3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3) |
| Navigation 2 | 2.10.2 | 23 sept. 2026 | [versions](https://developer.android.com/jetpack/androidx/versions) |
| Navigation 3 | **1.2.0 stable** (1.3.0-alpha02 en alpha) | 23 sept. 2026 | [navigation3 releases](https://developer.android.com/jetpack/androidx/releases/navigation3) |
| Lifecycle | 2.11.0 (minSdk 23 depuis 2.10.0) | 17 juin 2026 | [lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle) |
| Activity | 1.13.0 ; Core(-ktx) 1.19.1 ; DataStore 1.2.1 | sept.-oct. 2026 | [versions](https://developer.android.com/jetpack/androidx/versions) |
| Room 2 / Room 3 | 2.8.5 / **3.0.3** (3.1.0-alpha01) | 9 sept. 2026 | [room3 releases](https://developer.android.com/jetpack/androidx/releases/room3) |
| KSP | 2.3.12 ; AGP minimum 8.12.0 | 9 sept. | [KSP releases](https://github.com/google/ksp/releases/latest) |
| kotlinx.serialization | 1.11.0 (Kotlin 2.3.20), 1.12.0-RC (Kotlin 2.4.10) | 9 avr. / 4 sept. | [releases](https://github.com/Kotlin/kotlinx.serialization/releases) |
| kotlinx.coroutines | 1.11.0 (Kotlin 2.2.20) | 8 mai | [releases](https://github.com/Kotlin/kotlinx.coroutines/releases) |

(L'année n'est pas affichée sur les pages GitHub ; elle est déduite du contexte des versions Kotlin.)

Points de compatibilité importants :

- **[S]** AGP 9.0+ active le **Kotlin intégré** : le plugin `org.jetbrains.kotlin.android` ne doit plus être appliqué ; `kotlinOptions{}` devient `kotlin { compilerOptions {} }` ; `jvmTarget` suit par défaut `compileOptions.targetCompatibility`. Le Kotlin intégré ne sera plus désactivable en AGP 10. [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin)
- **[S]** KGP 2.4.20-2.4.21 est **entièrement supporté avec Gradle 7.6.3-9.7.0 et AGP 8.5.2-9.3.1** ; AGP 9.4.0 est donc légèrement au-dessus de la fenêtre testée (« peut fonctionner, avec avertissements »). [Kotlin Gradle](https://kotlinlang.org/docs/gradle-configure-project.html)
- **[S]** KSP 2.3.10 documente KGP 2.2.10-2.3.x, AGP ≥ 8.12, JDK ≥ 17 ; aucune page ne confirme Kotlin 2.4.x pour KSP 2.3.12. Des correctifs « AGP 9 built-in Kotlin » existent depuis 2.3.5/2.3.10, et un mainteneur indique que les versions KSP sont désormais découplées de celles de Kotlin ([KSP releases](https://github.com/google/ksp/releases), [KSP overview](https://kotlinlang.org/docs/ksp-overview.html), [PR #3107](https://github.com/google/ksp/pull/3107)). **Non confirmé → build de fumée obligatoire.**
- **[S]** Room 3 : paquet `androidx.room3`, artefacts `room3-runtime` / `room3-compiler`, **KSP uniquement** (ni KAPT ni Java), DAO `suspend` ou `Flow`, **`SQLiteDriver` obligatoire**, plugin Gradle `androidx.room3` avec `schemaDirectory` obligatoire. Room 2 et 3 peuvent coexister. [room3 releases](https://developer.android.com/jetpack/androidx/releases/room3)
- **[S]** Les bibliothèques Compose passeront en compileSdk 37.1 à partir de la ligne 1.13 (alpha03, 9 sept. 2026), ce qui impose de mettre à jour compileSdk de tous les consommateurs. La 1.12.1 (BOM 2026.09.00) n'est pas concernée. [compose-ui releases](https://developer.android.com/jetpack/androidx/releases/compose-ui). Lifecycle précise qu'un compileSdk plus récent exige AGP ≥ 9.2.0. [lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle)
- **[S]** Material 3 Expressive : une partie des API est encore en cours de graduation dans les alphas 1.5 (`ExperimentalMaterial3ExpressiveApi`). [material3 releases](https://developer.android.com/jetpack/androidx/releases/compose-material3)
- **[S]** Navigation 3 : retour arrière géré par une liste d'états (`NavBackStack`), `NavDisplay`, routes `@Serializable ... : NavKey`, prérequis minSdk 23 / compileSdk ≥ 36 pour la migration ; les ViewModels par destination passent par l'artefact `lifecycle-viewmodel-navigation3` 2.11.0 et `rememberViewModelStoreNavEntryDecorator()`. [Nav3 intro](https://developer.android.com/guide/navigation/navigation-3), [migration](https://developer.android.com/guide/navigation/navigation-3/migration-guide), [lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle). Le guide de migration ne dit pas si Nav3 est « recommandée pour les nouvelles applications » : **ce point n'est pas sourcé**.

### SDK : compileSdk / targetSdk / minSdk

- **[S]** AGP 9.4 supporte jusqu'à l'API 37 : `compileSdk = 37` est possible avec la plateforme `android-37` installée. [AGP](https://developer.android.com/build/releases/gradle-plugin)
- **[S]** Google Play, à partir du 31 août 2026 : toute nouvelle app ou mise à jour (téléphone/tablette) doit cibler **API 36 minimum** (extension possible jusqu'au 1er nov. 2026). [Play target API](https://developer.android.com/google/play/requirements/target-sdk)
- **[S]** Cibler 36 impose l'edge-to-edge sans échappatoire et active le retour prédictif par défaut ; sur écrans ≥ 600 dp les restrictions d'orientation sont ignorées. [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)
- **[S]** Cibler 37 supprime l'échappatoire d'orientation sur grands écrans et durcit l'audio en arrière-plan (voir §4). [Android 17 behavior changes](https://developer.android.com/about/versions/17/behavior-changes-17)
- **[S]** Aucune bibliothèque retenue n'exige plus que minSdk 23 (Lifecycle 2.10+, Navigation 3). `java.time` est natif dès l'API 26. [desugaring table](https://developer.android.com/studio/write/java8-support-table)
- **[S]** Répartition (données du 1er déc. 2025) : API 30+ ≈ 83 % cumulé ; le chiffre API 26 n'a pas été retrouvé. [heise](https://heise.de/-11162154) via recherche. Le tableau de bord officiel n'a pas été consulté.

### Recommandations

- **Indispensable** : Kotlin 2.4.21, AGP 9.4.0, Gradle 9.7.x, BOM 2026.09.00, Nav3 1.2.0 + lifecycle-viewmodel-navigation3 2.11.0, Room 3.0.3 + KSP 2.3.12, serialization 1.11.0, coroutines 1.11.0. Pas de plugin `kotlin-android`. Épingler toutes les versions dans `libs.versions.toml`. **[O]**
- **Indispensable** : `compileSdk = 37`, `targetSdk = 36`, **`minSdk = 26` conservé** (aucun gain sourcé à le remonter ; `java.time` natif, pas de desugaring). Interdire la ligne Compose 1.13 / BOM suivant 2026.09 tant que la plateforme 37.1 n'est pas installée. **[O]**
- **Utile** : DataStore Preferences 1.2.1 pour les réglages (son, vibration), plutôt que Room. **[O]**
- **Utile** : build de fumée à la première tâche (squelette + une entité Room + un `@Serializable`) pour valider KSP/Kotlin 2.4.21/AGP 9.4.0 ; vérifier avec `./gradlew buildEnvironment` quelle KGP est réellement résolue (AGP 9.0 embarque KGP 2.2.10 comme dépendance et monte une KGP plus ancienne, [source indirecte](https://developer.android.com/build/releases/agp-9-0-0-release-notes)). Plan B documenté : AGP 9.3.1 (dans la fenêtre KGP) ou Room 2.8.5.
- **Optionnel** : attendre kotlinx.serialization 1.12.0 stable (RC bâtie sur Kotlin 2.4.10).

### Extrait `gradle/libs.versions.toml` (proposition)

```toml
[versions]
agp = "9.4.0"
kotlin = "2.4.21"
ksp = "2.3.12"
composeBom = "2026.09.00"
activityCompose = "1.13.0"
coreKtx = "1.19.1"
lifecycle = "2.11.0"
navigation3 = "1.2.0"
room3 = "3.0.3"
datastore = "1.2.1"
serialization = "1.11.0"
coroutines = "1.11.0"
junit = "6.1.3"
turbine = "1.2.1"
robolectric = "4.17"
kover = "0.9.11"
ktlint = "1.8.0"

[libraries]
compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-material3 = { module = "androidx.compose.material3:material3" }
compose-ui = { module = "androidx.compose.ui:ui" }
compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "navigation3" }
androidx-room3-runtime = { module = "androidx.room3:room3-runtime", version.ref = "room3" }
androidx-room3-compiler = { module = "androidx.room3:room3-compiler", version.ref = "room3" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter" }
junit-vintage-engine = { module = "org.junit.vintage:junit-vintage-engine" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
robolectric = { module = "org.robolectric:robolectric", version.ref = "robolectric" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
room3 = { id = "androidx.room3", version.ref = "room3" }
kover = { id = "org.jetbrains.kotlinx.kover", version.ref = "kover" }
# Pas de kotlin-android (Kotlin intégré AGP 9). Plugin ktlint : voir §7.
# Driver SQLite pour Room 3 (sqlite-framework vs sqlite-bundled) : à confirmer par le dev-android.
```

Les coordonnées `androidx.room3:*` et `androidx.navigation3:*` viennent des pages officielles ; les coordonnées JUnit, Turbine, Robolectric, Kover sont de pratique courante **[P]** et à vérifier au premier sync.

---

## 2. JDK (système OpenJDK 27)

### Constats

- **[S]** Gradle : « une JVM entre 17 et 27 est requise pour exécuter Gradle » ; Java 27 est supporté (exécution et toolchains) **à partir de Gradle 9.8.0** ; Java 28 non supporté. Une version plus ancienne de cette page (vue par la recherche) donnait 17-26 : la page courante fait foi. [Gradle compatibility](https://docs.gradle.org/current/userguide/compatibility.html)
- **[S]** AGP 9.4.0 : JDK **minimum 17** (défaut 17), aucun maximum documenté ; Gradle minimum 9.6.0. [AGP](https://developer.android.com/build/releases/gradle-plugin). Aucune source trouvée affirmant AGP 9.4 testé sous JDK 27.
- **[S]** KGP 2.4.20-21 : fenêtre Gradle ≤ 9.7.0 (JDK 27 exige Gradle ≥ 9.8.0, donc hors fenêtre) ; la page ne donne pas de matrice JDK. [Kotlin Gradle](https://kotlinlang.org/docs/gradle-configure-project.html)
- **[S]** Robolectric 4.17 supporte le SDK 37 ; la 4.16 indiquait que les tests SDK 36 exigent **JDK 21**. [Robolectric releases](https://github.com/robolectric/robolectric/releases). detekt 2.0 alpha est testé sur JDK 25. [detekt](https://detekt.dev/changelog-2.0.0/)
- **[S]** Gradle 10 exigera « probablement » Java 21 pour le démon ; ~80 % des utilisateurs Gradle 9 sont déjà en Java 21+ (28 mai 2026). [Gradle blog](https://blog.gradle.org/gradle-is-javamaxxing). Une synthèse de recherche affirmait que Gradle 9.6 déprécie les démons < 21 : non confirmé par la page lue.
- **[S]** Critères de JVM du démon : `./gradlew updateDaemonJvm --jvm-version=21` écrit `gradle/gradle-daemon-jvm.properties` (à versionner) ; ces critères **prennent le pas sur `JAVA_HOME` et `org.gradle.java.home`** ; détection automatique du JDK local, téléchargement automatique désactivable. [Gradle daemon](https://docs.gradle.org/current/userguide/gradle_daemon.html)
- **[S]** Une toolchain demande un plugin de résolution (foojay) uniquement pour **télécharger** un JDK absent. [Kotlin Gradle](https://kotlinlang.org/docs/gradle-configure-project.html)
- **Local (vérifié par listage)** : `/usr/lib/jvm/java-21-openjdk` (JDK complet, avec `jmods`) et `/usr/lib/jvm/java-27-openjdk` sont installés.

### Recommandations

- **Indispensable** : exécuter Gradle sous **JDK 21** (déjà présent), pas 27. Avec Gradle 9.7.x + AGP 9.4.0 + KGP 2.4.21 on cumule déjà un écart de fenêtre ; ajouter JDK 27 en ajouterait un troisième sans bénéfice. **[O]**
- **Indispensable** : commiter `gradle/gradle-daemon-jvm.properties` (`toolchainVersion=21`) via `updateDaemonJvm`. Pas d'`org.gradle.java.home` en dur dans le dépôt (chemin propre à la machine, et ignoré dès que les critères existent).
- **Utile** : bytecode Android en 17 (`compileOptions` source/target 17) ; les tests unitaires tournent sur la JVM 21 (exigence Robolectric). **Le comportement de `java { toolchain }` avec le Kotlin intégré n'est pas couvert par la page de migration : à valider au premier build.**
- **Optionnel** : plugin foojay (`org.gradle.toolchains.foojay-resolver-convention`, version non vérifiée) : inutile tant que JDK 21 est installé ; en CI, `actions/setup-java` fournit le JDK. Jamais nécessaire à l'exécution de l'app (aucun impact sur le mode hors ligne).

---

## 3. Injection de dépendances

### Constats

- **[S]** Google recommande Hilt pour la plupart des applications de production, mais cite l'injection manuelle comme raisonnable pour les petites apps ; coûts de la manuelle : code répétitif et gestion des durées de vie ; à éviter : le service locator. [Android DI](https://developer.android.com/training/dependency-injection)
- **[S]** Hilt `androidx.hilt` 1.4.0 (1 juil. 2026) exige KGP ≥ 2.2.0 ; ses API Compose exigent AGP ≥ 9.2.0. Aucune page ne traite Hilt avec Room 3 ou Navigation 3. [hilt releases](https://developer.android.com/jetpack/androidx/releases/hilt)
- **[S]** Une fabrique de ViewModel se déclare avec `viewModelFactory { initializer { ... } }` et un conteneur créé dans `Application` (guide DI manuelle, d'après la page DI ci-dessus).
- Koin : non étudié sur source primaire.

### Recommandation

- **Indispensable** : **DI manuelle** : une classe `AppContainer` créée dans `Application` (base Room, `TableRepository`, réglages, `Clock`, lecteur de son/vibration) + `viewModelFactory`. Une dizaine de dépendances, un seul module, aucun besoin de portées complexes ; cela évite un deuxième processeur d'annotations à côté de KSP/Room 3 et garde les tests simples (constructeurs avec fakes). **[O]**
- **Optionnel** : réévaluer Hilt si l'app dépasse une quinzaine de dépendances ou se modularise.
- Règle d'architecture à transmettre à l'architecte : injecter `java.time.Clock`, un `Dispatcher`, et des interfaces `Haptics`/`SoundPlayer` pour tester minuteur et séries sans appareil.

---

## 4. Minuteur, vibration, son

### Constats

- **[S]** `FLAG_KEEP_SCREEN_ON` ne nécessite pas de permission ; en arrière-plan le système laisse l'écran s'éteindre ; l'effacer quand le besoin cesse. [Keep screen on](https://developer.android.com/develop/background-work/background-tasks/awake/screen-on). En Compose : `view.keepScreenOn = true` dans un `DisposableEffect` **[P]**.
- **[S]** Service de premier plan : Android 14+ exige type + permission dédiée ; `shortService` est limité à ≈ 3 minutes (exercices de 5 à 10 min : inadapté) ; `specialUse` possible mais sous-type relu par Google Play ; la doc préfère des API dédiées (alarmes, notifications) quand elles suffisent. [FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types)
- **[S]** `VIBRATE` est requis pour `Vibrator` ; les appareils sans vibreur ignorent les appels ; `VibrationEffect.createWaveform` (API 26+) ; effets prédéfinis (`EFFECT_CLICK`, `DOUBLE_CLICK`) dès Android 10 ; **pour une vibration en arrière-plan il faut des attributs (`USAGE_ALARM`)** ; `performHapticFeedback` ne demande aucune permission et respecte le réglage système de retour tactile ; `HAPTIC_FEEDBACK_ENABLED` lisible. [Haptics APIs](https://developer.android.com/develop/ui/views/haptics/haptics-apis)
- **[S]** Android 17 (cible 37) : pour l'audio en arrière-plan (lecture, focus, volume) il faut un service de premier plan avec capacité « while-in-use », ou la permission d'alarme exacte avec flux `USAGE_ALARM`. [Android 17 changes](https://developer.android.com/about/versions/17/behavior-changes-17)
- **[S]** État de saisie `rememberTextFieldState()` : sauvegardé/restauré sur rotation. [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input)
- **[P]** (pages de référence non lues) `ToneGenerator` (bips synthétiques, flux au choix, `release()` obligatoire, peut lever une exception à l'initialisation sur certains appareils) ; `SoundPool` (échantillon court en `res/raw`, chargement asynchrone, `release()`) ; `USAGE_ASSISTANCE_SONIFICATION` suit le mode sonnerie/silencieux alors que `USAGE_ALARM` le contourne.

### Recommandations

- **Indispensable** : minuteur **par horodatage**, pas par compteur. Conserver `endAtElapsedRealtime` (`SystemClock.elapsedRealtime()`, monotone, inclut la veille) et la durée dans le ViewModel ; reste = fin - maintenant, recalculé à chaque tick (`delay` ~250 ms, affichage à la seconde). Rotation = ViewModel intact ; retour d'arrière-plan = recalcul immédiat. Pour la mort du processus, sauvegarder aussi une fin en heure murale (`SavedStateHandle`) ; après redémarrage de l'appareil `elapsedRealtime` repart de zéro, donc seule l'heure murale est exploitable. **[O]**
- **Indispensable** : **pas de service ni notification en v1**. Les exercices sont à l'écran ; `keepScreenOn` tant que le minuteur tourne. À l'expiration au premier plan : vibration + son. Si l'app revient d'arrière-plan après expiration : état « temps écoulé » sans rejouer de son. Évite permissions, revue Play du sous-type et durcissement audio d'Android 17. **[O]**
- **Indispensable** : permission `VIBRATE` (permission normale, aucun accès réseau) ; motif court type `createWaveform` (ex. deux impulsions) ; consulter `hasVibrator()` seulement pour masquer l'option. Réglages : vibration on/off, son on/off. **[O]**
- **Utile** : son via **SoundPool** + un bip bref (< 1 s, original ou CC0, à placer dans `res/raw`) avec `USAGE_ASSISTANCE_SONIFICATION`, joué seulement si `AudioManager.ringerMode == RINGER_MODE_NORMAL` (« son discret » respecté en silencieux), sans demande de focus audio pour ne pas couper une vidéo d'actual play (exercice 5). `ToneGenerator` évite l'asset mais sonne « téléphone » et peut échouer à l'init **[P]**. **[O]**
- **Optionnel** : pause du minuteur (ajuste `endAt`) ; notification de fin hors app (`POST_NOTIFICATIONS` + alarme) en v1.1 ; `performHapticFeedback` comme repli sans permission.
- **À valider sur appareil réel** : comportement vibration/son en mode silencieux et Ne pas déranger (références tronquées).

---

## 5. Détection de mots-clés en français et surlignage

### Constats

- **[S]** Snowball français : tronque les formes à une racine (continuait / continuant / continuerait → *continu* ; mains → *main* ; contractée → *contract* ; maladroite → *maladroit*), gère les élisions et les tréma ; limites : -ent et -ons non retirés, *cheveux/jeux/lieux/neveux* gardent le pluriel, *travaux* → *traval*. Aucune licence ni date sur la page. [Snowball French](https://snowballstem.org/algorithms/french/stemmer.html)
- **[S]** Le seul artefact Maven trouvé est `com.github.rholder:snowball-stemmer` 1.3.0.581.1, **de 2014** (> 18 mois : à écarter), licence BSD ; Lucene embarque les mêmes classes dans `lucene-analysis-common` (poids non mesuré). [Maven Central](https://central.sonatype.com/artifact/com.github.rholder/snowball-stemmer), [Lucene FrenchStemmer](https://lucene.apache.org/core/4_2_1/analyzers-common/org/tartarus/snowball/ext/package-summary.html)
- **[S]** Champs d'édition : l'API à état (`TextFieldState`) est recommandée par Google ; son état survit aux changements de configuration et à la mort du processus ; `OutputTransformation` modifie l'affichage sans toucher à l'état et, contrairement à `VisualTransformation`, **sans calcul d'`OffsetMapping`**. [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input)
- **[S]** `TextFieldBuffer.addStyle(...)` permet d'appliquer un style partiel depuis une `OutputTransformation` ; la transformation est rappelée à chaque nouvel affichage de texte ; les styles ne sont pas conservés si le texte est édité ensuite. [OutputTransformation](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/input/OutputTransformation) (confirmé par recherche ; **statut stable non vérifié**, page tronquée).
- **[P]** `java.text.Normalizer` (NFD) + suppression de `\p{Mn}` est la méthode courante pour ignorer les accents ; les ligatures œ/æ ne se décomposent pas (table manuelle) ; apostrophe typographique ’ ≠ '.

### Recommandations (architecture de la détection)

1. **Indispensable : normalisation par jeton, sans table de correspondance de positions.** Découper le texte original avec une expression `\p{L}[\p{L}\p{M}]*` (on garde les positions d'origine), puis normaliser chaque jeton : minuscules (`Locale.ROOT`), NFD, retrait des marques, œ→oe, æ→ae, ’→'. Les élisions (l', d', qu') sont séparées par l'apostrophe. Les plages de surlignage restent exprimées dans le texte d'origine. **[O]**
2. **Indispensable : mots-clés fournis par le rédacteur.** Chaque entrée de table potentiellement détectée porte `keywords` (mots pleins et synonymes proches, 1 à 4) en plus du texte affiché. Le moteur n'a alors qu'à comparer des jetons ; il n'« invente » rien. Pour une réplique (Connect the Story) : score = part des mots pleins retrouvés (stoplist française d'environ 150 mots embarquée) avec un seuil à fixer par le corpus ; un lieu ou un objet : tête du groupe suffisante. **[O]**
3. **Utile : racinisation.** Comparer aussi les racines. Recommandation : **recopier dans le dépôt** les classes Java Snowball (`SnowballProgram`, `Among`, `FrenchStemmer`), sous licence BSD après vérification de la licence de la version récupérée, derrière une interface `Stemmer` ; ne pas ajouter Lucene ni l'artefact de 2014. Alternative légère : règles maison (pluriel s/x, féminin -e/-ée/-es, -ant/-ante), moins complète sur les conjugaisons. **[O]**
4. **Optionnel : tolérance aux fautes** (distance de Damerau-Levenshtein ≤ 1) : à limiter aux mots ≥ 8 lettres ; sur les mots courts elle crée des faux positifs (lampe/rampe, tour/four). Désactivée par défaut, activable par entrée. **[O]**
5. **Indispensable : corpus de test doré** (~100 phrases françaises par type d'élément, avec positifs attendus et faux positifs piégeux : homographes, négations, mot inclus dans un autre) exécuté en test unitaire JVM ; c'est ce corpus, non le choix d'algorithme, qui réduit le risque noté « élevé » dans `docs/pilotage.md`. Mesurer rappel/précision avant de figer le seuil. **[O]**
6. **Surlignage** : `BasicTextField`/`TextField` avec `TextFieldState` + `OutputTransformation` qui appelle le moteur sur le texte courant et applique `addStyle(SpanStyle(...))` (**à prototyper sur Compose 1.12.1** : disponibilité stable de `addStyle`, coût du recalcul à chaque affichage ; si le moteur dépasse ~1 ms, mettre en cache par texte). Repli sûr et stable : `VisualTransformation` avec `AnnotatedString` et `OffsetMapping.Identity` (le style ne change pas la longueur) sur un champ à `TextFieldValue`. **[O]**
7. **Accessibilité** : ne pas coder l'information par la seule couleur de fond (ajouter gras ou soulignement) et doubler le surlignage par la liste cochée hors du champ (les spans ne sont pas lus par TalkBack **[P]**). Pour Connect the Story : cases recalculées après un `debounce` (~300 ms) via `snapshotFlow { state.text }`, avec bascule manuelle possible puisque la détection est approximative (SPEC). **[O]**

---

## 6. Tables JSON dans `assets/`

### Constats

- **[S]** `Json` par défaut est strict : clés inconnues refusées, `coerceInputValues=false`, `isLenient=false`, `explicitNulls=true` ; `@JsonIgnoreUnknownKeys` est expérimental. [kotlinx.serialization json.md](https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/json.md)
- **[S]** Bibliothèque de validation JSON Schema : `com.networknt` (schema-validator) 2.3.3 du 20 fév. 2026, Java 8+/Jackson 2, drafts jusqu'à 2020-12 (index secondaires, version à revérifier). [recherche](https://repo1.maven.org/maven2/com/networknt/schema-validator/)
- Le statut exact de `decodeFromStream` (JVM, expérimental ou non) n'a pas pu être confirmé.

### Recommandations

- **Indispensable** : un modèle commun versionné : `{ "schemaVersion": 1, "table": "places", "locale": "fr", "entries": [ { "id": "...", "text": "...", "tags": [], "keywords": [] } ] }` ; `story_elements.json` avec 5 catégories nommées. Identifiants stables : l'historique Room stocke l'id du tirage pour rester relisible si le texte est corrigé. **[O]**
- **Indispensable** : chargement runtime avec `Json` strict ; lire via `assets.open(path).bufferedReader().readText()` puis `decodeFromString` (fichiers de quelques dizaines de Ko, API stable). Une table invalide doit échouer au test, pas planter chez l'utilisateur. **[O]**
- **Indispensable : validation par tests JVM, pas au build.** Un test lit tous les fichiers de `src/main/assets/tables/` (répertoire de travail = module **[P]**) et vérifie : décodage strict, ≥ 30 entrées, id uniques, aucune chaîne vide ou avec espaces en bordure, doublons après normalisation, longueur maximale, chaque table attendue présente, 5 catégories dans `story_elements`, `keywords` non vide là où la détection est utilisée, **texte en forme NFC** (`Normalizer.isNormalized`) pour éviter des accents décomposés qui casseraient la détection. **[O]**
- **Utile** : fichier JSON Schema (draft 2020-12) référencé par `$schema` pour l'autocomplétion de l'éditeur du rédacteur ; ne l'exécuter dans les tests (networknt) que si la communauté de contributeurs grandit. **[O]**
- **Optionnel** : tâche Gradle dédiée de validation (aucun bénéfice par rapport aux tests).

---

## 7. Outillage de test et qualité

### Constats

- **[S]** JUnit 6.1.3 (7 août 2026, Platform + Jupiter + Vintage) ; baseline Java 17 selon une source tierce. [JUnit releases](https://github.com/junit-team/junit-framework/releases), [howtodoinjava](https://howtodoinjava.com/junit/junit-6-gradle-dependency/)
- **[S]** Le plugin `de.mannodermaus.android-junit5` (`android-junit-framework` 2.0.1) cible AGP ≥ 8.2 ; **compatibilité AGP 9 / JUnit 6 non confirmée**. Sans plugin, JUnit 5 en test unitaire se règle via `unitTests.all { useJUnitPlatform() }`, mais pas en test instrumenté. [android-junit-framework](https://github.com/mannodermaus/android-junit-framework)
- **[S]** Tests Compose : `createComposeRule()` exige `ui-test-manifest` (debug) ; règles JUnit 4. Tests locaux Compose sous Robolectric dans `src/test` avec `isIncludeAndroidResources = true` **[P]** (page officielle muette sur Robolectric). [Compose testing](https://developer.android.com/develop/ui/compose/testing)
- **[S]** Robolectric 4.17 (10 sept.) : SDK 37 ; Turbine 1.2.1 (11 juin) ; Kover 0.9.11 (29 sept.) avec support AGP 9 depuis 0.9.4/0.9.5. [Robolectric](https://github.com/robolectric/robolectric/releases), [Turbine](https://github.com/cashapp/turbine/releases), [Kover](https://github.com/Kotlin/kotlinx-kover/releases)
- **[S]** ktlint 1.8.0 (14 nov., année probablement 2025 : ~11 mois). [ktlint](https://github.com/pinterest/ktlint/releases). detekt : la 2.0 est **encore en alpha** (2.0.0-alpha.5, 17 juin 2026), construite sur Kotlin 2.4 et AGP 9.x. [detekt changelog](https://detekt.dev/changelog-2.0.0/)
- **[S]** Actions GitHub : `actions/checkout` v7 (7.0.1, 20 juil. 2026), `actions/setup-java` v6 (6.1.0, 14 oct.), `gradle/actions/setup-gradle` v6 (6.4.0, 28 sept.). [checkout](https://github.com/actions/checkout/releases), [setup-java](https://github.com/actions/setup-java/releases), [gradle/actions](https://github.com/gradle/actions/releases)

### Recommandations

- **Indispensable** : tests unitaires JVM pour toute la logique (minuteur avec `kotlinx-coroutines-test`, moteur de détection, tirage, séries, validation des tables) ; Turbine pour les `Flow` des ViewModels. **[O]**
- **Indispensable** : Robolectric 4.17 (JDK 21) pour des tests Compose locaux de l'écran d'exercice et du minuteur ; un petit jeu de tests instrumentés seulement pour la vibration, le son et la rotation (appareil réel). **[O]**
- **Indispensable** : Kover limité au domaine (minuteur, détection, séries, repositories), exclusions : écrans Compose, classes générées Room (`*_Impl`), `BuildConfig`. Seuil 80 % sur ce périmètre (règle globale du projet). **[O]**
- **Indispensable** : garde-fou « hors ligne » en CI : après `processDebugMainManifest`/`processReleaseMainManifest`, échouer si le manifeste fusionné contient `android.permission.INTERNET` ou `ACCESS_NETWORK_STATE` (une dépendance transitive peut l'ajouter). **[O]**
- **Utile** : ktlint seul pour le style (intégration Gradle via Spotless ou plugin dédié : **version et compatibilité AGP 9 non vérifiées**). Ne pas adopter detekt 1.x (Kotlin 2.4 non garanti) ; réévaluer detekt 2.0 stable. Android Lint (`lintDebug`) en CI. **[O]**
- **Utile : JUnit** : Jupiter 6.1.x pour les tests purs, plus `junit-vintage-engine` pour Robolectric et les règles Compose (JUnit 4) ; `tasks.withType<Test> { useJUnitPlatform() }`. Simplification possible : JUnit 4 partout (voir DÉCISION). **[O]**
- **Utile : workflow CI minimal** (`ubuntu-latest`) : `checkout@v7` → `setup-java@v6` (Temurin 21) → `gradle/actions/setup-gradle@v6` → `./gradlew ktlintCheck lintDebug testDebugUnitTest koverVerify assembleDebug` + contrôle de manifeste. Pas d'émulateur en v1. **[O]**
- **Optionnel** : tests de captures d'écran, matrice multi-API, détection de régressions de performance.

---

## 8. Série de jours consécutifs

### Constats

- **[S]** `java.time` est natif dès l'API 26 : `minSdk = 26` dispense du desugaring. [desugaring table](https://developer.android.com/studio/write/java8-support-table)
- **[P]** `LocalDate` n'a ni heure ni fuseau : les arithmétiques de jours sont insensibles aux passages heure d'été/hiver ; la série doit se calculer sur des dates locales, pas sur des intervalles de 24 h.

### Recommandations

- **Indispensable** : à la création d'une session, stocker `startedAtEpochMillis`, `zoneId` (texte) et **`localEpochDay`** (jour local calculé avec le fuseau du moment). La série se calcule sur `localEpochDay` : voyager ou changer de fuseau ne réécrit pas l'historique. **[O]**
- **Indispensable** : fonction pure `série(joursAvecSession, aujourd'hui)` : jours distincts triés ; la série courante compte les jours consécutifs en remontant depuis aujourd'hui **ou depuis hier si rien n'est encore fait aujourd'hui** (la série n'est pas « cassée » avant minuit) ; calculer aussi la meilleure série. `Clock` injecté pour les tests (`Clock.fixed`). **[O]**
- **Utile** : « jour logique » qui commence à 04:00 pour qu'une séance à 00:30 compte pour la veille ; c'est un choix produit (voir DÉCISION). **[O]**
- **Utile** : cas de test : changement d'heure (mars/octobre), minuit, deux sessions le même jour, trou d'un jour, changement de fuseau, horloge réglée dans le passé.
- **Optionnel** : « jour de grâce » hebdomadaire (voir la recherche C pour l'UX anti-culpabilisation).

---

## Questions ouvertes

DÉCISION: Navigation — Navigation 3 1.2.0 (stable, liste d'états) / Navigation Compose 2.10.2 (routes typées) — **Navigation 3**, car stable depuis le 23 sept. 2026 et adaptée à une app mono-activité simple (la SPEC écrit « Navigation Compose » : à confirmer explicitement).

DÉCISION: Base de données — Room 3.0.3 (`androidx.room3`, KSP seul, `SQLiteDriver` obligatoire) / Room 2.8.5 — **Room 3** après build de fumée ; repli Room 2.8.5 si KSP/Kotlin 2.4.21 pose problème. Attention : la plupart des exemples en ligne utilisent encore `androidx.room`.

DÉCISION: Fenêtre de compatibilité — AGP 9.4.0 (hors fenêtre testée par KGP 2.4.21) / AGP 9.3.1 (dans la fenêtre) — **AGP 9.4.0 + Gradle 9.7.x**, avec repli 9.3.1 si le build de fumée échoue.

DÉCISION: JDK — JDK 27 système / JDK 21 installé / JDK 17 — **JDK 21** via `gradle-daemon-jvm.properties` (exigé par Robolectric pour le SDK 36, et plancher probable de Gradle 10).

DÉCISION: targetSdk — 36 (exigence Play dès le 31 août 2026) / 37 (Android 17 : audio en arrière-plan durci, orientation non verrouillable) — **36 pour la v1**, passage à 37 après tests sur grands écrans.

DÉCISION: Minuteur en arrière-plan — horodatage seul, sans service / service de premier plan `specialUse` + notification / alarme + notification — **horodatage seul en v1** ; notification de fin envisageable en v1.1.

DÉCISION: Fin de minuteur — SoundPool avec bip embarqué / ToneGenerator / vibration seule en silencieux — **SoundPool**, joué uniquement en mode sonnerie normal, vibration réglable ; il faut un bip original ou CC0 (à produire).

DÉCISION: Racinisation française — Snowball recopié dans le dépôt / règles maison / aucune (mots-clés du rédacteur uniquement) — **mots-clés du rédacteur + normalisation d'abord, Snowball recopié si le corpus de test montre un rappel insuffisant** (vérifier sa licence BSD).

DÉCISION: Tolérance aux fautes de frappe — activée par défaut / désactivée / par entrée — **désactivée par défaut, activable par entrée**, mots ≥ 8 lettres.

DÉCISION: Cases « cochées auto » de Connect the Story — recalcul pur à chaque frappe / verrouillage une fois cochée / recalcul avec bascule manuelle — **recalcul avec bascule manuelle**, puisque la détection est approximative.

DÉCISION: Jour d'une séance pour la série — minuit local / jour logique à 04:00 — **minuit local en v1**, à trancher avec l'UX (recherche C).

DÉCISION: Framework de test — JUnit 4 partout / Jupiter 6 + Vintage — **Jupiter 6.1.x + Vintage** pour la logique pure et Robolectric ; JUnit 4 pour `androidTest` ; repli JUnit 4 seul si la configuration Gradle devient pénible.

DÉCISION: Contrat de données pour le rédacteur — champ `keywords` obligatoire sur `story_elements` et `sensory_words` / facultatif — **obligatoire sur ces deux tables** (le rédacteur-contenu doit être briefé).

## Points à vérifier par le dev-android au premier build

1. KGP effectivement résolue (`./gradlew buildEnvironment`), absence du plugin `kotlin-android`.
2. KSP 2.3.12 avec Kotlin 2.4.21 et Room 3.0.3 (génération du DAO).
3. Driver SQLite de Room 3 (`AndroidSQLiteDriver` ou `BundledSQLiteDriver`) et impact sur la taille de l'APK (non sourcé).
4. `addStyle` dans `OutputTransformation` sur Compose 1.12.1 (stabilité, performance).
5. Vibration et son en mode silencieux / Ne pas déranger, sur un appareil réel.
