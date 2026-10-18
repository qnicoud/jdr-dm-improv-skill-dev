# ADR 0015 — Sauvegarde réimportable et import des sessions

- Statut : proposée (2026-10-18), à valider avec les décisions D54+ de la v1.1
- Lien : ADR 0004 (migrations, jamais de perte), ADR 0010 (aucune permission, pas de cloud), ADR 0011 (série calculée), ADR 0016 (langue de session)
- Entrées : `docs/pilotage.md` « v1.1 — périmètre » point 3, `docs/ux/parcours-v1.1.md` §3 (S11 Données, écran S13 et décisions V11-9 à V11-12, V11-14)

## Contexte

L'export actuel (`SessionExportFormatter`) produit un **texte lisible** en français. Il n'est pas réimportable : les libellés sont traduits, les codes perdus, et certaines données manquent (cases de détection, options, graine). L'utilisateur doit pourtant pouvoir passer de l'installation de développement à l'APK signée, qui ont le même `applicationId` mais pas la même signature (la désinstallation efface la base), ou changer de téléphone, sans perdre son historique.

Le fichier importé est une **entrée non fiable** : il a pu être choisi par erreur, tronqué, modifié à la main, ou produit par une version plus récente de l'app.

## Options

### Format

1. Le JSON **remplace** l'export texte. Écartée : l'utilisateur perdrait la lecture et le partage simples (UX V11-9).
2. **Deux exports distincts** : le texte lisible (inchangé) et une **sauvegarde JSON versionnée**. Retenue.
3. Une archive ZIP qui contient les deux. Écartée : il faudrait décompresser une entrée non fiable (bombe de décompression), pour un gain nul.
4. Une copie brute de `dmgym.db`. Écartée : elle est liée au schéma Room, illisible, et impossible à fusionner sans doublon.

### Identité d'une session (anti-doublon)

1. `sessions.id` : écartée. L'identifiant est autogénéré localement, donc deux installations produisent les mêmes `id` (1, 2, 3…) pour des sessions différentes.
2. Nouvelle colonne `uid` (UUID) : unique par construction, mais elle demande une migration avec un remplissage `randomblob` pour l'existant, un index unique, et elle double la notion d'identité.
3. **Clé naturelle `(exerciseId, startedAtEpochMillis)`** : l'instant de début est à la milliseconde, il est écrit une seule fois et conservé à l'import. Deux sessions du même exercice commencées à la même milliseconde sont impossibles en pratique pour un utilisateur seul. L'index `sessions(exerciseId, startedAtEpochMillis)` existe déjà. Elle reste stable d'une chaîne d'imports à l'autre (A → B → C).

### Conflit (même identité des deux côtés)

- **L'existante gagne** (UX V11-10) / la plus récente (`lastActivityAtEpochMillis`) gagne / fusion champ par champ. « Rien n'est remplacé » est une promesse simple et sans perte.

## Décision

### 1. Format `dmgym-sessions`, version 1

Le fichier est un JSON UTF-8 compact, au nom ASCII daté (`dm-gym-sauvegarde-AAAA-MM-JJ.json`), de type MIME `application/json`. Il est **neutre en langue** : codes et identifiants, et les textes d'origine tels qu'ils ont été tirés ou saisis.

```json
{
  "format": "dmgym-sessions",
  "formatVersion": 1,
  "exportedAt": "2026-10-18T14:03:00Z",
  "appVersion": "1.1.0",
  "sessions": [
    {
      "exerciseId": "crazy_eights",
      "status": "COMPLETED",
      "startedAtEpochMillis": 1760790000000,
      "lastActivityAtEpochMillis": 1760790480000,
      "endedAtEpochMillis": 1760790492000,
      "zoneId": "Europe/Paris",
      "localEpochDay": 20379,
      "plannedDurationSec": 480,
      "activeDurationSec": 470,
      "overtimeSec": 12,
      "difficulty": 1,
      "options": { "situation_mode": true },
      "selfRating": 3,
      "isGem": false,
      "fromDaily": true,
      "drawSeed": 123456789,
      "contentLocale": "fr",
      "draws": [
        { "slotKey": "problem", "position": 0, "tableId": "problems", "entryId": "accroche-taverne",
          "textSnapshot": "…", "autoDetected": null, "manualChecked": null, "category": null }
      ],
      "answers": [ { "fieldKey": "idea1", "groupIndex": 0, "text": "…", "flagged": false, "isGem": false } ],
      "rubric": [ { "questionKey": "beyond_first_ideas", "value": true } ]
    }
  ]
}
```

- Le champ `options` est l'objet décodé d'`optionsJson`, pour qu'il soit validé comme le reste au lieu d'être une chaîne opaque.
- `contentLocale` est **facultatif** à l'import (absent = `"fr"`) : une sauvegarde faite avant le lot de langue reste valide (ADR 0016).
- **Sessions exportées** : la même règle que pour le texte, c'est-à-dire les terminées et les inachevées ou en cours qui ont du contenu. Les brouillons vides ne sont pas exportés.
- Les **réglages ne sont pas inclus** (UX V11-14) ; l'interface le dit.
- L'export JSON est construit par une fonction pure `BackupEncoder.encode(details, exportedAt, appVersion): String`, dans `core/backup`. Le fichier est écrit par `ContentResolver.writeText` (sélecteur `ACTION_CREATE_DOCUMENT`, comme l'export texte). Après l'écriture, l'écran affiche le nom du fichier et le nombre de sessions (UX §3.1).

### 2. Versions du format et compatibilité

- `formatVersion` est **indépendant** de la version du schéma Room. Il n'augmente que si le contenu du fichier change : un champ ajouté, retiré ou renommé, ou une sémantique modifiée.
- Le code garde une **classe de transfert (DTO) figée par version** (`BackupV1`, plus tard `BackupV2`…) et une fonction `BackupVn.toDomain()`. L'app écrit toujours la dernière version et **lit toutes les versions antérieures** : un export v1 reste importable par une app qui écrit en v2 (UX §3.3 : « version ancienne importable silencieusement »).
- `formatVersion` supérieur à la version prise en charge → erreur « version plus récente », rien n'est importé.
- Décodage **strict** (`ignoreUnknownKeys = false`, `isLenient = false`) : une clé inconnue dans une version connue est une erreur. C'est pour cela que tout ajout de champ impose d'augmenter `formatVersion`.
- **Fichiers témoins** : `src/test/resources/backup/v1/*.json`, valides et invalides. Ils ne sont **jamais modifiés** une fois commités. Un test garantit qu'ils restent importables par toutes les versions futures, sur le modèle des schémas Room exportés.

### 3. Lecture bornée et validation stricte (entrée non fiable)

La lecture se fait en deux temps, sans rien écrire avant que tout soit validé.

1. **Lecture bornée** (`platform/DocumentReader`) : `ContentResolver.openInputStream(uri)` dans un flux qui **compte les octets et s'interrompt au-delà de 8 Mio**. On ne se fie pas à `OpenableColumns.SIZE`, que certains fournisseurs ne donnent pas. Le texte est décodé en UTF-8 strict : une séquence invalide est une erreur.
   - Ordre de grandeur : une session pèse de 1 à 5 Ko, et trois sessions par jour pendant trois ans donnent environ 10 Mo.
   - 8 Mio de JSON restent décodables en mémoire sur un appareil d'entrée de gamme, ce que le lot mesurera sur un fichier synthétique de 7 Mio.
   - Au-delà de cette taille, l'erreur est « fichier trop gros » (UX §3.3).
2. **Reconnaissance** : avant le décodage, l'en-tête du texte lisible (`DM Gym - export de mes sessions`, ou son équivalent traduit) produit l'erreur « export à lire » (UX §3.3).
3. **Décodage** de l'enveloppe (`format`, `formatVersion`), puis du DTO de la version.
4. **Validation** (`BackupValidator`, pure). Toute violation fait **refuser le fichier entier** avec un motif typé (`ImportError`), sauf le dernier cas de la liste :
   - `format == "dmgym-sessions"` ;
   - au plus 20 000 sessions, 64 tirages, 256 réponses et 16 entrées de grille par session ;
   - clés (`exerciseId`, `slotKey`, `fieldKey`, `questionKey`, clés d'`options`) conformes à `[a-z0-9_:.-]{1,64}` ; `entryId` à `[A-Za-z0-9_-]{1,64}` ; `tableId` nul ou parmi les noms de `TableId` ;
   - textes : `text` ≤ 20 000 caractères, `textSnapshot` ≤ 1 000, sans caractère de contrôle autre que `\n` et `\t` ;
   - `status` dans l'énumération ; `selfRating` nul ou dans 1..3 ; `difficulty` dans 1..3 ; durées ≥ 0 et ≤ 24 h ;
   - `startedAtEpochMillis` entre le 2026-01-01 et maintenant + 1 jour ; `ended ≥ started` ; `lastActivity ≥ started` ;
   - `zoneId` valide (`ZoneId.of`), sinon le fichier est refusé ; `localEpochDay` à ± 1 jour du jour de `startedAt` dans `zoneId` ;
   - `contentLocale` parmi les langues de contenu connues (ADR 0016), ou absent ;
   - pas de doublon `(slotKey, position)`, `(fieldKey, groupIndex)` ou `questionKey` dans une même session ;
   - **`exerciseId` inconnu de cette version** : seul cas qui n'invalide pas le fichier. La session est **ignorée et comptée** (« 2 d'un exercice inconnu seront ignorées », UX §3.2), parce qu'une sauvegarde d'une version plus récente peut contenir un exercice ajouté depuis.
5. **Plan d'import** (`ImportPlanner`, pur) : à partir des sessions valides et de l'ensemble des identités déjà en base, il produit `ImportPlan(toInsert, alreadyPresent, unknownExercise, dateRange)`. Un doublon **à l'intérieur du fichier** est compté une seule fois. C'est ce plan qu'affiche l'aperçu S13.

Sécurité : il n'y a aucune désérialisation polymorphe ni réflexion. Les requêtes Room sont paramétrées. Les textes sont rendus en texte brut dans Compose (aucun HTML ni lien interprété). `tableId` et `entryId` ne servent que de clés de recherche. Aucun chemin de fichier n'est tiré du contenu. Toutes les exceptions de lecture et de décodage (`SerializationException`, `IllegalArgumentException`, `IOException`, `CharacterCodingException`, `SecurityException` du fournisseur) sont converties en `ImportError`, et aucun message technique n'est montré à l'utilisateur.

### 4. Écriture transactionnelle, idempotente, annulable

- Nouveau port `SessionArchive.importSessions(plan): ImportResult(insertedIds)`, implémenté dans `RoomHistoryRepository` par une méthode de DAO `@Transaction`, le même mécanisme que `saveSnapshot`.
  - **Dans la transaction**, les identités déjà présentes sont relues. On évite ainsi une course avec une session créée entre l'aperçu et la confirmation.
  - Chaque nouvelle session est insérée et reçoit un **nouvel `id` local**. Ses tirages, réponses et grille sont insérés avec cet `id`.
  - C'est **tout ou rien** : la moindre erreur annule la transaction et rien n'est ajouté (UX §3.2).
- **Idempotence** : réimporter le même fichier ajoute 0 session.
- Les champs `localEpochDay`, `zoneId`, `startedAt` et `lastActivityAt` sont conservés tels quels. La série, les jours pratiqués et les compteurs **sont calculés à la lecture** (ADR 0011) : rien à recalculer, les `Flow` réémettent.
- Une session importée `IN_PROGRESS` avec contenu suit les règles habituelles : elle est reprenable si sa dernière activité date de moins de 24 h, inachevée au-delà.
- **Annuler cet import** (UX V11-11) : `SessionArchive.deleteSessions(insertedIds)`, dans une transaction. C'est possible tant que l'écran S13 est ouvert : les `insertedIds` vivent dans le `ViewModel`, pas dans le `SavedStateHandle`. Après la mort du processus, l'annulation n'est plus proposée.
- Ni le décodage, ni la validation, ni la transaction ne s'exécutent sur le thread principal (`Dispatchers.Default` pour le décodage, `ioDispatcher` pour la base). L'état « Lecture du fichier… » n'apparaît qu'au-delà de 300 ms.

### 5. Sélecteur de fichier

- `ActivityResultContracts.OpenDocument()` avec les types `application/json`, `application/octet-stream` et `text/plain`. Certains fournisseurs (Téléchargements, gestionnaires tiers) déclarent mal le type, et la validation tranche de toute façon.
- On ne prend **aucune permission persistante** (`takePersistableUriPermission`) : le fichier est lu une fois. Aucune permission de stockage n'est nécessaire.

### 6. Passage de la rc1 à l'APK signée (UX V11-12)

La rc1 ne sait pas produire de sauvegarde JSON. La procédure retenue, qui ne demande pas d'analyser le texte lisible :

1. installer une build **debug v1.1** par-dessus l'installation de développement (même clé debug, données conservées) ;
2. sauvegarder ;
3. installer l'APK signée, ce qui oblige à désinstaller l'ancienne puisque l'`applicationId` est identique ;
4. importer.

Elle est documentée dans `docs/qa/release.md` et dans l'écran S13. L'import du texte lisible n'est pas développé : le texte est incomplet, et l'analyser serait fragile.

## Conséquences

- Aucune dépendance nouvelle : kotlinx.serialization est déjà là.
- Aucune migration Room pour l'import lui-même. La langue de session (`contentLocale`) relève de l'ADR 0016.
- Toute évolution future du contenu d'une session (nouvelle colonne) doit être répercutée dans le format de sauvegarde : nouvelle `formatVersion`, nouveau DTO, nouveaux fichiers témoins. La liste de contrôle de l'ADR 0004 est complétée en ce sens.
- Tests :
  - JVM :
    - aller-retour `encode` → `decode` → égalité des `SessionDetail` (hors `id`) ;
    - chaque règle de validation (un fichier témoin invalide par règle) ;
    - `exerciseId` inconnu ignoré ;
    - doublons internes ; version trop récente ; texte lisible reconnu ; JSON tronqué ; UTF-8 invalide ;
    - fichiers témoins v1 toujours importables ;
    - `ImportPlanner` (déjà présentes, plage de dates) ;
    - performance indicative : 5 000 sessions décodées et validées en moins de 2 s sur la JVM.
  - Robolectric :
    - transaction tout ou rien : une erreur injectée au milieu laisse la base inchangée ;
    - idempotence ;
    - annulation ;
    - course : une session insérée entre l'aperçu et la confirmation n'est pas dupliquée ;
    - lecteur borné : un flux de 8 Mio + 1 octet est refusé.
  - Compose : les états A, B, C, C′, D et E de S13.
  - Manuel : aller-retour réel debug → APK signée sur le téléphone de l'utilisateur.
- Revue `security-reviewer` obligatoire sur ce lot, puisque c'est la première entrée externe de l'app.

## Décisions de l'utilisateur (2026-10-18)

- D64 : deux lignes dans Réglages (« Exporter pour lire », « Sauvegarder mes sessions »). D65 : identité = (exercice, instant de début), sans migration. D66 : la session déjà présente gagne. D67 : bouton « Annuler l'import » sur l'écran de résultat. D68 : passage à l'APK signé = installer la v1.1 de développement par-dessus, sauvegarder, puis changer d'app et importer ; pas d'import du texte lisible. D70 : sauvegarde des sessions seulement, pas des réglages.


## Complément (revue de sécurité, 2026-10-18)

La borne de lecture est abaissée de 16 à **8 Mio** (pic mémoire de plusieurs copies du texte), et un `OutOfMemoryError` pendant la lecture ou le décodage est affiché comme « fichier trop volumineux ». Une session `IN_PROGRESS` importée est rendue périmée, donc jamais reprenable ; une session invalide est écartée au lieu de faire refuser tout le fichier.
