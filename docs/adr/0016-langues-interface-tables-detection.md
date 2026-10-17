# ADR 0016 — Langues : interface, tables de contenu et détection par langue

- Statut : proposée (2026-10-18). La décision « quelle langue livrer en v1.1 » est ouverte (voir la fin de l'ADR).
- Amende : ADR 0004 (schéma v3), ADR 0008 (détection par langue), ADR 0009 (tables par langue), ADR 0013 (pas de clé de langue dans DataStore)
- Entrées : `docs/pilotage.md` « v1.1 — périmètre » point 4, `docs/ux/parcours-v1.1.md` §4 (V11-1, V11-2, comportement des sessions passées, longueurs)

## Contexte

L'app est écrite en français : 343 chaînes dans `values/strings.xml`, 10 tables (environ 560 entrées) et 3 nouvelles tables en v1.1, une détection de mots-clés réglée sur le français avec son corpus doré, et des formats de date fixés en dur (`Locale.FRENCH` dans `HistoryFormat`, `DailyFormat` et `RecentDaysRow`). On trouve aussi du texte français dans le code : l'export lisible (`SessionExportFormatter`) et le mot de confirmation `EFFACER`.

L'utilisateur demande une gestion de la langue. La seule deuxième langue crédible est l'**anglais** : la communauté JdR et les sources pédagogiques sont anglophones (UX §4.1). Le contenu des tables est du texte créatif, avec des mots-clés propres à chaque langue : il se **réécrit** plus qu'il ne se traduit.

Principe UX non négociable : **la langue est un seul commutateur**. L'interface, les tirages et la détection changent ensemble, ou rien ne change. Une langue à moitié livrée (interface anglaise, tirages français) est pire qu'une seule langue.

## Options

### Périmètre livré en v1.1

| Option | Contenu | Coût estimé (lots d'agents) |
|---|---|---|
| **A. Architecture seule, français seul** | Ressources, tables et détection paramétrées par langue ; langue de session stockée ; formats via la locale ; **aucun sélecteur** | 1 lot `dev-android` (V5), environ 1 jour |
| B. Architecture + anglais complet | A + `values-en` (environ 360 chaînes, plurals compris) + 13 tables réécrites en anglais avec leurs mots-clés + corpus doré anglais (environ 500 cas) + recette | A + 4 à 5 lots (`redacteur-contenu` ×2 ou 3, `testeur` ×1, `dev-android` ×1), plus une relecture native de l'anglais |
| C. Architecture + interface anglaise seule | A + `values-en` ; tirages en français | A + 1 lot, mais **contredit le principe du commutateur unique** |

### Sélecteur de langue (quand une 2e langue est livrée)

1. **API de langue par app du framework** (`LocaleManager.setApplicationLocales`, Android 13+), avec `localeConfig` généré par AGP (`androidResources.generateLocaleConfig = true` + `res/resources.properties` qui contient `unqualifiedResLocale=fr`). Le sélecteur du système (Réglages Android > Applis > Langue) et la feuille de l'app sont synchronisés. Le système enregistre le choix et recrée l'activité. **En dessous d'Android 13**, la ligne « Langue » est masquée : l'app suit la langue du téléphone.
2. **AppCompat** (`AppCompatDelegate.setApplicationLocales` + service `AppLocalesMetadataHolderService` avec `autoStoreLocales`) : un même comportement de l'API 26 à 37. Coût : une dépendance `androidx.appcompat` (version à vérifier par le `chercheur`), `MainActivity` qui devient `AppCompatActivity` et des thèmes XML qui dérivent de `Theme.AppCompat`, y compris le thème de démarrage (`postSplashScreenTheme`).
3. Une solution maison (langue dans les préférences + `attachBaseContext` + contexte d'application enveloppé) : écartée. Elle est fragile, doit être lue de façon synchrone au démarrage et oublie facilement les notifications et les widgets système.

## Décision

### 1. Notion de langue

- `core/i18n/ContentLanguage` : énumération des **langues de contenu** livrées. Au départ, `FR("fr")` seul ; `EN("en")` s'ajoute avec l'option B. `ContentLanguage.DEFAULT = FR`.
- `ContentLanguage.resolve(locales: List<String>): ContentLanguage` : la première langue du téléphone (ou de l'app) prise en charge, sinon `FR`. C'est le repli silencieux de l'UX §4.2.
- Port `LanguageProvider { val current: StateFlow<ContentLanguage> }`. Il est implémenté dans `platform/` à partir de la configuration de l'`Application` (`ConfigurationCompat.getLocales`), mise à jour par `registerComponentCallbacks.onConfigurationChanged`. Aucune clé DataStore : sur Android 13+, le choix est conservé par le système.
- **Interface et contenu ont toujours la même langue.** Cette langue vaut celle de l'interface si son contenu est livré, sinon le français.
- Ressources par défaut : `values/` **reste le français** (langue de référence, SPEC « français par défaut »), et une 2e langue s'ajoute dans `values-<lang>/`. Conséquence : un téléphone réglé en allemand affiche le français. Si l'anglais est livré et qu'on préfère l'anglais comme repli international, il faudra déplacer le français dans `values-fr/` et l'anglais dans `values/`. C'est un changement mécanique, à décider le jour où l'anglais est livré.

### 2. Tables par langue (amende l'ADR 0009)

- Arborescence : `assets/tables/<lang>/<table>.json`, par exemple `assets/tables/fr/problems.json`. Les 10 fichiers actuels sont **déplacés** (`git mv`) dans `tables/fr/`, sans autre modification.
- L'enveloppe ne change pas (`schemaVersion` 1). Le champ `locale` doit être égal au nom du dossier.
- **Identifiants stables entre langues** : une entrée garde le même `id`, la même `category`, les mêmes `tags` et la même `difficulty` dans toutes les langues. Seuls `text`, `keywords` et `fuzzy` changent. C'est ce qui permet les statistiques par entrée, le complément de catégorie des anciens tirages (`withCategories`) et l'étiquette `rank:*` de Status Numbers, quelle que soit la langue.
- `TableSource.load(id, language)` lit `tables/<lang>/<table>.json`. `TableRepository` met en cache par `(language, TableId)`.
- **Pas de repli entrée par entrée.** Une langue de contenu n'existe dans `ContentLanguage` que si **toutes** ses tables sont présentes et passent les tests. À l'exécution, une table absente ou invalide dans une langue livrée suit la règle de l'ADR 0009 (erreur explicite, mode « sujet libre »), sans mélanger les langues.
- `TablesContentTest` est paramétré par langue. Il ajoute un **test de parité** : pour chaque table, les ensembles d'`id` sont identiques d'une langue à l'autre, ainsi que les `category`, `tags` et `difficulty` de chaque `id`.

### 3. Langue de chaque session (amende l'ADR 0004 : schéma v3)

- Nouvelle colonne `sessions.contentLocale TEXT` nullable (`null` = `"fr"`, la valeur de toutes les sessions antérieures). Elle est ajoutée par `AutoMigration(2 → 3)`, avec le schéma `3.json` exporté et un `RoomMigrationTest` 1 → 2 → 3 sur des données réelles. `SessionRecord.contentLocale` et `BackupV1.contentLocale` la portent (ADR 0015).
- **La langue d'une séance est fixée au premier tirage** de l'écran Prêt. Elle est conservée dans `ExerciseSession`, et le `SavedStateHandle` la garde pour les sessions pas encore démarrées. Les relances, les événements (bascule de Status Numbers) et la détection utilisent **cette** langue, même si l'utilisateur change la langue de l'app en cours de route.
- Une session reprise (brouillon) est reprise dans sa langue d'origine, avec l'interface dans la langue courante (UX §4.3).
- L'historique affiche `textSnapshot` (langue d'origine, jamais traduit). Les libellés (noms d'exercice, champs, catégories) suivent l'interface. Les blocs de contenu portent `Modifier.semantics { … LocaleList(contentLocale) }`, pour que TalkBack les lise avec la bonne voix (UX §4.3).

### 4. Détection par langue (amende l'ADR 0008)

- `KeywordDetector` est déjà neutre : le découpage `\p{L}[\p{L}\p{M}]*` sépare les élisions françaises (*l'*, *qu'*) comme les contractions anglaises (*don't* → *don*, *t* ; *dragon's* → *dragon*, *s*), et les mots-clés passent par le même découpage. La normalisation (minuscules `Locale.ROOT`, NFD, retrait des diacritiques, œ/æ) convient au français et à l'anglais.
- Les réglages propres à une langue passent par `DetectionProfile(language): stemmer` : `IdentityStemmer` pour `fr` et `en`. Snowball (français ou anglais) ne se branche derrière `Stemmer` que si le corpus de la langue le justifie (règle D11 inchangée).
- `KeywordAnswerDetector` construit ses cibles depuis les tables de la **langue de la session**.
- **Corpus doré par langue** : `src/test/resources/detection/<lang>/corpus.json` (le corpus actuel est déplacé dans `fr/`). Les seuils sont les mêmes (rappel ≥ 0,85, précision ≥ 0,90). Une langue ne peut pas être ajoutée à `ContentLanguage` tant que son corpus n'est pas vert. Le rapport de mesure va dans `docs/qa/detection.md`, une section par langue.

### 5. Interface, formats et textes hors `strings.xml`

- **Aucun `Locale.FRENCH` ni motif de date en dur.** Les formateurs (`HistoryFormat`, `DailyFormat`, `RecentDaysRow`, `SessionExportFormatter`) reçoivent la `Locale` en paramètre : ce sont des fonctions pures, testées par locale. L'UI la lit dans `LocalConfiguration.current.locales[0]`.
  - Les motifs viennent de squelettes : `DateFormat.getBestDateTimePattern(locale, "dMMMM")`, et `"jm"` pour l'heure, qui respecte le réglage 12/24 h du téléphone (`DateFormat.is24HourFormat`).
  - Les noms de jours passent par `DayOfWeek.getDisplayName(style, locale)`.
- **Quantités** : tout texte qui contient un nombre passe par `<plurals>` (le français et l'anglais n'ont pas la même règle pour 0). On active les vérifications Lint `PluralsCandidate`, `MissingQuantity` et `StringFormatInvalid`.
- **Export lisible** : ses textes fixes (en-tête, « terminée », « Jouée en premier »…) passent dans `strings.xml` par `ExportLabels`, qui devient `ExportTexts`. L'export suit la langue de l'interface. La sauvegarde JSON reste neutre (ADR 0015).
- **Mot de confirmation** `EFFACER` : il devient la ressource `erase_confirm_word`. Le mot accepté est celui de la langue courante.
- **Notification** (ADR 0014) : le nom du canal et les textes viennent des ressources, et le canal est recréé à chaque démarrage pour suivre la langue.
- Nom de l'app : « DM Gym » dans toutes les langues.

### 6. Sélecteur (si une 2e langue est livrée)

- **Option 1 retenue : `LocaleManager` du framework (API 33+)**, sans AppCompat. La ligne « Langue » (Réglages > Apparence, feuille avec autonymes, UX §4.2) n'est affichée **que sur Android 13+ et seulement si `ContentLanguage.entries.size > 1`**. En dessous d'Android 13, l'app suit la langue du téléphone.
- Ce choix s'écarte en partie de UX V11-2 (« même comportement sur toutes les versions »). L'écart touche les appareils sous Android 13, qui suivent quand même la langue de leur téléphone. Il évite une dépendance et la migration des thèmes vers AppCompat. Si l'utilisateur tient à un comportement identique sur toutes les versions, l'option 2 (AppCompat) remplace la seule implémentation du sélecteur, sans toucher au reste de cet ADR.
- `localeConfig` est généré par AGP à partir des dossiers `values-*`. Sa présence est vérifiée dans le manifeste fusionné par un test de build. La disponibilité de `generateLocaleConfig` sur AGP 9.4 sera confirmée au lot.
- Changer de langue recrée l'activité. Les `ViewModel`, les `TextFieldState` et le minuteur survivent : rien n'est perdu pendant un exercice. Le changement ne peut de toute façon se faire que depuis Réglages ou le système, jamais pendant l'exercice.

### 7. Tests (impact)

- JVM :
  - `ContentLanguage.resolve` ;
  - `TablesContentTest` par langue, avec le test de parité ;
  - corpus par langue ;
  - formateurs de date et de durée en `fr` et en `en` (y compris 12/24 h) ;
  - **test de complétude des traductions** : chaque clé de `values/strings.xml` existe dans chaque `values-<lang>/`, et ses arguments de format (`%1$s`, `%2$d`) ont le même nombre et le même type.
- Robolectric : `AllExercisesSmokeTest` et les tests Compose des écrans principaux, exécutés aussi sous `@Config(qualifiers = "en")` quand l'anglais existe.
- Migration Room 2 → 3 avec `contentLocale`.
- Lint : `MissingTranslation` et `ExtraTranslation` en erreur dès qu'une 2e langue existe.
- Manuel : pseudo-locale `en-XA` (`pseudoLocalesEnabled` en debug), à 200 %, sur un écran de 360 × 640 dp (UX §4.4). Critère : le minuteur et « Terminer » restent visibles avec le clavier ouvert.

## Recommandation pour la v1.1

**Option A** (architecture seule, français seul, aucun sélecteur visible), alignée sur UX V11-1. L'anglais (option B) est découpé dans `docs/architecture.md` §16 en lots **E1 à E4, prêts à lancer et non bloquants** : la v1.1 sort sans eux s'ils ne sont pas verts. Ils deviennent la v1.2 (ou une v1.1.x) dès que l'utilisateur le décide.

## Conséquences

- Le lot d'architecture (V5) touche les tables, la détection, l'historique (schéma v3), les formateurs et les réglages. Il doit passer **après** le découpage d'`ExerciseViewModel` (V0), et le lot fusionné en second, entre V5 et C1 (nouvelles tables), déplace celles-ci dans `tables/fr/`.
- L'option B a un vrai coût de contenu : environ 750 entrées à réécrire avec leurs mots-clés, et un corpus de 500 cas. Elle demande aussi la relecture d'un anglophone : le texte créatif et les mots-clés en anglais ne peuvent pas être validés par l'utilisateur seul, ce qui est un risque de qualité.
- Le brief du `redacteur-contenu` devra être étendu à l'anglais si B est retenue : son rôle actuel est limité au français.
