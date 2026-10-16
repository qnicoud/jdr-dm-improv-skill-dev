# DM Gym

Application Android 100 % hors ligne pour entraîner les compétences d'improvisation d'un MJ de JdR.
Spécification : [SPEC.md](SPEC.md). Architecture : [docs/architecture.md](docs/architecture.md).

## Prérequis

- **JDK 21** (le démon Gradle l'exige via `gradle/gradle-daemon-jvm.properties`, voir ADR 0002).
  Sur Arch Linux : `sudo pacman -S jdk21-openjdk`.
- **Android SDK** avec la plateforme `android-37` et les build-tools `36.0.0`
  (`ANDROID_HOME`, par exemple `~/Android/Sdk`).
- Pas de Gradle global : utiliser le wrapper `./gradlew` (Gradle 9.7.0).

## Premier lancement

Le JDK système peut être plus récent (ex. 27) : le wrapper se lance avec lui, mais le démon Gradle
est démarré sous un JDK 21 détecté sur la machine. Si le JDK 21 n'est pas détecté automatiquement,
indiquer-le explicitement :

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew clean check assembleDebug
```

Le premier lancement télécharge Gradle et les dépendances (connexion nécessaire une seule fois).

## Commandes courantes

| Commande | Rôle |
|---|---|
| `./gradlew check assembleDebug` | Tout vérifier et construire l'APK debug (équivalent de la CI) |
| `./gradlew testDebugUnitTest` | Tests unitaires JVM (Jupiter + Robolectric via Vintage) |
| `./gradlew ktlintFormat` | Corriger automatiquement le style |
| `./gradlew lintDebug` | Android Lint |
| `./gradlew koverVerify` | Couverture du domaine (seuil 80 %) |
| `./gradlew verifyPermissions` | Liste blanche de permissions du manifeste fusionné (ADR 0010) |
| `./gradlew buildEnvironment` | Versions réellement résolues (KGP, KSP, AGP) |
| `./gradlew :app:installDebug` | Installer sur un appareil ou un émulateur connecté |

L'APK debug est produit dans `app/build/outputs/apk/debug/`.

## Structure

- `app/` : module Android unique (paquet `io.github.qnicoud.dmgym`).
- `build-logic/` : build inclus (tâche `verifyPermissions` et ses tests).
- `gradle/libs.versions.toml` : toutes les versions (aucune version en dur dans un script de build).
- `docs/` : recherche, ADR, UX, maquettes.
