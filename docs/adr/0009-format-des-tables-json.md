# ADR 0009 — Format des tables JSON et identifiants stables

- Statut : acceptée (2026-10-16)
- Décisions de pilotage : D14 (`keywords` obligatoire), D21 (`difficulty` dès la v1), D22 (table de situations One-Page)
- Source : `docs/research/01-stack-technique.md` §6

## Contexte

Il y a 11 tables aléatoires en français (≥ 30 entrées chacune), rédigées par l'agent `redacteur-contenu` en parallèle du code. L'historique conserve le tirage : il doit rester relisible même si un texte est corrigé plus tard. Une table invalide doit faire échouer un test, pas planter chez l'utilisateur.

## Options

- Un schéma propre à chaque table, ou **une enveloppe commune** avec des champs facultatifs.
- Validation au build (tâche Gradle, JSON Schema), ou **par tests JVM**.
- Identifiant d'entrée : index de position (fragile) ou **id texte stable**.

## Décision

**Enveloppe commune** pour toutes les tables, décodée en **mode strict** (`Json` par défaut : une clé inconnue est une erreur) :

```json
{
  "schemaVersion": 1,
  "table": "story_elements",
  "locale": "fr",
  "entries": [
    {
      "id": "lanterne-fendue",
      "text": "une lanterne à la vitre fendue",
      "difficulty": 1,
      "category": "object",
      "keywords": ["lanterne", "lanternes", "falot"],
      "fuzzy": false,
      "tags": []
    }
  ]
}
```

```kotlin
@Serializable data class TableFile(
    val schemaVersion: Int,
    val table: String,
    val locale: String,
    val entries: List<TableEntry>,
)
@Serializable data class TableEntry(
    val id: String,
    val text: String,
    val difficulty: Int,                       // 1..3, obligatoire (D21)
    val category: String? = null,
    val keywords: List<String> = emptyList(),
    val fuzzy: Boolean = false,
    val tags: List<String> = emptyList(),
)
```

| Fichier | `category` | `keywords` | Remarques |
|---|---|---|---|
| `problems.json` | — | — | |
| `places.json` | — | facultatif | |
| `sensory_words.json` | **obligatoire** : `sight`, `hearing`, `smell`, `taste`, `touch` | **obligatoire** (1-4) | ≥ 6 entrées par sens |
| `core_images.json` | — | — | |
| `threading_questions.json` | **obligatoire** : `what`, `origin`, `who`, `link` | — | questions tirées dans l'ordre des catégories |
| `npc_roles.json` | — | — | `tags` : `rank:high` ou `rank:low` (option contre-stéréotype) |
| `status_shifts.json` | — | — | `tags` : `shift:up` ou `shift:down` |
| `surprise_situations.json` | — | — | |
| `story_elements.json` | **obligatoire** : `place`, `object`, `trait`, `sensory`, `line` | **obligatoire** (1-4) | **≥ 30 entrées par catégorie** |
| `one_page_situations.json` | — | — | 30 entrées (D22) |

Les valeurs de `category` et de `tags` sont des **codes anglais** stables. L'interface affiche des libellés tirés de `strings.xml`.

**Identifiants** : kebab-case ASCII `[a-z0-9]+(-[a-z0-9]+)*`, uniques dans la table, **jamais renommés ni réutilisés**. Supprimer une entrée est permis : l'historique garde une copie du texte (`textSnapshot`, voir `docs/architecture.md` §4). Corriger une coquille dans `text` ne change pas l'id.

**Validation par tests JVM** (`TablesContentTest`, qui lit `src/main/assets/tables/`) :
- présence des 10 fichiers attendus ; décodage strict ; `table` égal au nom du fichier ; `schemaVersion == 1` ; `locale == "fr"` ;
- ≥ 30 entrées (≥ 30 par catégorie pour `story_elements`, ≥ 6 par sens pour `sensory_words`) ;
- ids uniques et conformes au motif ; `difficulty ∈ 1..3` ; présence de chaque niveau de 1 à 3 dans chaque table ;
- pas de chaîne vide, pas d'espace en bordure, texte ≤ 200 caractères, **forme NFC** (`Normalizer.isNormalized`) ;
- pas de doublon après normalisation (même normaliseur que la détection) ;
- `category` dans la liste permise ; `keywords` de 1 à 4 là où ils sont obligatoires, chaque mot-clé non vide et sans doublon normalisé ; `fuzzy == true` seulement si au moins un mot-clé fait 8 lettres ou plus ;
- `tags` dans le vocabulaire permis de la table.

Le chargement à l'exécution lit `assets.open(path).bufferedReader().use { it.readText() }`, puis `decodeFromString`.

## Conséquences

- Ce format est le **contrat du `redacteur-contenu`**, qui peut commencer dès maintenant.
- Un fichier JSON Schema (draft 2020-12) pourra être ajouté plus tard pour l'autocomplétion. Il n'est pas exécuté par les tests en v1.
- Faire évoluer le format implique d'incrémenter `schemaVersion` et de mettre à jour cet ADR.
- Si une table est corrompue à l'exécution (cas impossible si la CI est verte), le chargeur lève une erreur explicite et l'exercice concerné affiche un message ; le reste de l'app fonctionne.

## Complément (2026-10-16, décision D45)

Option « contre-stéréotype » de Status Numbers : le statut tiré est choisi à l'opposé de l'étiquette `rank:*` du rôle (rang haut → statut bas, et inversement). Aucun champ ni tag supplémentaire dans le schéma.

## Complément (2026-10-17, recette rc1, critère 1.8)

Le statut tiré de Status Numbers suit désormais le rang social du rôle, lu dans son étiquette `rank:*` (`statusCards` dans `core/exercise/StatusBands.kt`) :

| Étiquette | Par défaut | Contre-stéréotype (D45) |
|---|---|---|
| `rank:high` | 7-10 | 1-3 |
| `rank:mid` | 4-7 | 1-3 ou 8-10 |
| `rank:low` | 1-4 | 8-10 |
| aucune | 1-10 | 1-10 |

Les 34 rôles de `npc_roles.json` portent déjà exactement une étiquette : 17 `rank:high` et 17 `rank:low`. Aucun `rank:mid` n'existe dans la table, la plage est prête si le rédacteur en ajoute. La table n'est pas modifiée. Le rappel « statut ≠ rang » reste affiché.
