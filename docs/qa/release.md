# Build de publication (L12c)

Version : `versionCode 2`, `versionName 1.1.0` (`app/build.gradle.kts`). L'écran À propos lit `versionName` dans le paquet installé.

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
apksigner sign --ks ~/chemin/vers/ma-cle.jks --out dmgym-1.1.0.apk /tmp/dmgym-aligned.apk
apksigner verify --print-certs dmgym-1.1.0.apk
```

Garder la clé et son mot de passe hors du dépôt, avec une sauvegarde : une mise à jour doit être signée par la même clé.

### Script automatisé : `tools/release.sh`

Enchaîne construction, signature, vérification et publication sur GitHub :

```bash
export DMGYM_KEYSTORE=~/chemin/vers/ma-cle.jks
export DMGYM_CERT_SHA256=<empreinte SHA-256 du certificat>   # recommandé : refuse une autre clé
tools/release.sh --dry-run   # construit et signe dans dist/, sans rien publier
tools/release.sh             # idem, puis pousse le tag et crée la release GitHub
```

- La version vient de `versionName` (`app/build.gradle.kts`) ; le tag est `v<version>`.
- L'APK est toujours construit **depuis le tag**, dans un worktree temporaire. Si le tag n'existe pas encore, il est créé sur `HEAD` : `main` doit alors être propre et synchronisée avec `origin`.
- Le mot de passe est demandé par `apksigner` : il ne passe jamais par le script ni par un fichier.
- `DMGYM_KEY_ALIAS` est facultatif si la clé est seule dans le magasin.
- Le script refuse de publier si la release GitHub existe déjà : il faut d'abord incrémenter `versionCode` et `versionName`.
- Options : `--skip-checks` (sans `./gradlew check`), `--draft`, `--notes-file FICHIER` (par défaut : les commits depuis le tag précédent), `--yes` (sans confirmation). Une version contenant `-` (ex. `1.2.0-rc1`) est publiée en préversion.
- Prérequis : `gh` authentifié (`gh auth login`), JDK 21 (`JAVA_HOME`, par défaut `/usr/lib/jvm/java-21-openjdk`), build-tools 36.0.0.
- Le script n'installe rien sur un appareil.

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

## 5. Passage de l'app debug à l'APK signé, sans perdre ses données (D68, ADR 0015 §6)

Contexte : l'app debug (`io.github.qnicoud.dmgym`) et l'APK signé avec ta clé ont le **même identifiant** mais des **signatures différentes**. Android refuse la mise à jour par-dessus : il faut désinstaller la première, ce qui **efface toutes les sessions**. La sauvegarde JSON est donc le seul moyen de les garder. Le test `BackupRoundTripTest` joue ce scénario (base v3 avec des sessions, sauvegarde, base vide, import, contenu identique).

1. **Vérifier la version de l'app debug.** Si elle n'est pas en v1.1, la mettre à jour (cela conserve les données, même clé debug) : `./gradlew installDebug`. Puis Réglages, À propos : la version affichée doit être `1.1.0`.
2. **Sauvegarder.** Réglages, Données, « Sauvegarder mes sessions ». Enregistrer le fichier `dm-gym-sauvegarde-AAAA-MM-JJ.json` dans Documents ou Téléchargements. Le vérifier : le nom est daté du jour, la taille n'est pas nulle (de l'ordre de quelques Ko par dizaine de sessions) et le nombre de sessions annoncé correspond à l'Historique (compter celles qui sont terminées ou ont du contenu : les brouillons vides ne sont pas exportés).
3. **Copier le fichier hors du téléphone** (câble : `adb pull /sdcard/Documents/dm-gym-sauvegarde-AAAA-MM-JJ.json .`, ou partage vers un autre appareil). Vérifier la copie : `head -c 200` doit montrer `{"format":"dmgym-sessions"...`, et la taille doit être identique.
4. **Désinstaller l'app debug.** **C'est le seul moment irréversible** : tout ce qui n'est pas dans la sauvegarde disparaît (sessions et réglages). Ne le faire qu'après avoir vérifié les étapes 2 et 3, et avoir la copie hors du téléphone. `adb uninstall io.github.qnicoud.dmgym`, ou Paramètres, Applications, DM Gym, Désinstaller.
5. **Construire et signer la release** avec ta clé : section 2 ci-dessus (`assembleRelease`, `zipalign`, `apksigner sign`, `apksigner verify --print-certs`).
6. **Installer l'APK signé** : `adb install dmgym-1.1.0.apk`. Réglages, À propos : `1.1.0`. L'Historique est vide, c'est normal.
7. **Importer.** Réglages, Données, « Importer des sessions », « Choisir un fichier », sélectionner la sauvegarde. Vérifier l'aperçu : « N sessions trouvées » avec N égal au nombre noté à l'étape 2, et la plage de dates attendue. Confirmer l'ajout, puis contrôler l'Historique (nombre, quelques sessions ouvertes, bijoux conservés) et les statistiques. En cas d'erreur de fichier, rien n'est ajouté : recommencer. Réimporter le même fichier ajoute 0 session.
8. **Refaire à la main ce qui n'est pas transféré** (D70 : la sauvegarde ne contient que les sessions) : thème, durées par exercice, options (vibration, sans minuteur, One-Page dans la séance du jour) et **rappels** (réactiver, autoriser les notifications, choisir jours et heure). L'étiquette « Nouveau » et la séance du jour se recalculent d'après l'historique importé.

Une fois l'étape 7 validée, la sauvegarde peut être gardée ailleurs : elle reste importable par les versions futures (les fichiers témoins `src/test/resources/backup/v1` le garantissent).
