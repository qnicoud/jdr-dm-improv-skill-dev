# Build de publication (L12c)

Version : `versionCode 1`, `versionName 1.0.0-rc1` (`app/build.gradle.kts`). L'écran À propos lit `versionName` dans le paquet installé.

## 1. Build non signé (CI et contrôle)

```bash
./gradlew check koverVerify assembleRelease
```

- `release` : `isMinifyEnabled` et `isShrinkResources` actifs ; règles dans `app/proguard-rules.pro`
  (sérialiseurs `@Serializable` de l'app, `Companion`, `serializer()` ; Room 3 `RoomDatabase`).
  kotlinx.serialization, Room, DataStore et Navigation 3 apportent leurs propres règles consommateur.
- Sortie : `app/build/outputs/apk/release/app-release-unsigned.apk`. `verifyReleasePermissions` (dans `check`) ne tolère que la liste blanche (`VIBRATE`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`, ADR 0014) : aucune permission réseau ni d'alarme exacte.
- La CI exécute `assembleRelease` sans signature.

## 2. Signature de publication (à faire par l'utilisateur)

Avec sa propre clé, **jamais committée** (`*.jks` et `*.keystore` sont ignorés par git) :

```bash
zipalign -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /tmp/dmgym-aligned.apk
apksigner sign --ks ~/chemin/vers/ma-cle.jks --out dmgym-1.0.0-rc1.apk /tmp/dmgym-aligned.apk
apksigner verify --print-certs dmgym-1.0.0-rc1.apk
```

Garder la clé et son mot de passe hors du dépôt, avec une sauvegarde : une mise à jour doit être signée par la même clé.

## 3. Vérification locale sur appareil

Propriété Gradle optionnelle, uniquement en local : signe avec la clé debug **et** ajoute `applicationIdSuffix ".release"`.
L'app s'installe donc à côté de l'app réelle (`io.github.qnicoud.dmgym`) sans l'écraser. Ne jamais désinstaller cette dernière : cela effacerait l'historique.

```bash
./gradlew assembleRelease -Pdmgym.signReleaseWithDebug=true
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -c
adb shell am start -n io.github.qnicoud.dmgym.release/io.github.qnicoud.dmgym.MainActivity
# ouvrir chaque exercice (rotation comprise), puis :
adb logcat -d -b crash
```

À regarder : aucun crash `SerializationException` / `ClassNotFound` / `NoSuchMethod`, navigation et restauration (routes `@Serializable`), chargement des 10 tables, historique (Room), réglages (DataStore).
Désinstaller seulement `io.github.qnicoud.dmgym.release` si nécessaire.

### Résultat du 2026-10-17

Installation réussie et processus lancé, aucun crash dans `logcat -b crash`. Le téléphone était verrouillé (PIN) : les écrans n'ont **pas** pu être parcourus. Le parcours des exercices reste à faire à la main sur la release.

## 4. Sauvegarde (D26)

Couverte par `BackupRulesTest` : `cloud-backup` exclut tout ; `device-transfer` inclut base, fichiers et préférences ; `full_backup_content` (API 26-30) exclut tout, car ces versions ne distinguent pas le transfert de la sauvegarde cloud.
