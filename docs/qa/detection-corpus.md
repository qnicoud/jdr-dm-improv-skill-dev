# Corpus doré de détection des mots-clés

Livrable du lot L4 (ADR 0008 §3). Fichier : `app/src/test/resources/detection/corpus.json`.
Vérification : `python3 -I docs/qa/verify_corpus.py <racine-du-dépôt>`.

## Format

L'ADR 0008 ne fixe pas de format ; celui-ci est le format par défaut du brief :

```json
{"cases":[{"id":"story-0001","entryId":"auberge-bord-route","table":"story_elements",
           "text":"Les voyageurs se réchauffent dans l'auberge…","expected":true,"tag":"direct"}]}
```

- `id` unique (`story-NNNN` ou `sensory-NNNN`) ; `entryId` existe dans `table` (`story_elements` ou `sensory_words`).
- `expected` : l'élément `entryId` doit-il être détecté dans `text` ? Un cas ne teste qu'**une** entrée.
- La catégorie (`place`, `hearing`…) n'est pas dans le cas : elle se déduit de la table.
- `tag` : `direct`, `inflection` (forme fléchie présente dans les `keywords`), `elision`, `accent` (accents absents, ou accents repliés par la normalisation), `case`, `punctuation`, `negation` (attendu `true`, ADR 0008), `unlisted-inflection`, `synonym`, `substring`, `homograph`, `typo`, `neutral`.
- Pièges = tout sauf `direct` et `neutral`.

## Méthode

Phrases rédigées à la main entrée par entrée (un MJ qui improvise : 1 à 2 phrases). Pour chaque entrée : 2 à 4 positifs, 1 synonyme non listé, 1 à 2 faux amis ou homographes. Compléments générés : 60 fautes de frappe (permutation de deux lettres d'un mot-clé de 8 lettres ou plus, max 6 par catégorie) et des phrases neutres (négatifs purs) jusqu'à 95 cas par catégorie de `sensory_words`. Une apostrophe sur cinq est typographique (’).

Le script de vérification réimplémente le moteur de l'ADR (jetons `\p{L}`, normalisation NFD, suites de jetons contiguës, pas de fuzzy) et exige que l'attendu humain coïncide avec lui, **sauf** pour `unlisted-inflection` (attendu `true`, le moteur v1 rate) et `homograph` (attendu `false`, le moteur v1 se trompe). Ces deux tags mesurent volontairement les limites ; tout autre désaccord est une erreur de corpus.

## Comptes (1375 cas, 659 positifs)

| Catégorie | Cas | Pièges |
|---|---|---|
| story/place | 163 | 77 % |
| story/object | 152 | 76 % |
| story/trait | 192 | 74 % |
| story/sensory | 206 | 67 % |
| story/line | 187 | 69 % |
| sensory/sight, hearing, smell, taste, touch | 95 chacun | 36 à 44 % |

Par type : direct 377, substring 235, synonym 200, neutral 157, inflection 97, unlisted-inflection 93, homograph 64, typo 60, accent 55, elision 14, negation 12, case 10, punctuation 1. Pièges : 841/1375 (61 %), minimum 36 % par catégorie.

Écart au « environ 100 » : les catégories de `story_elements` sont à 150-200 cas, parce que ces entrées sont plus nombreuses (158 contre 43). Elision, casse, négation et ponctuation sont minces (surtout traités ailleurs : `accent` couvre les majuscules et accents manquants).

## Mesure de référence

Moteur v1 simulé (sans Snowball, sans fuzzy) : **rappel 0,859, précision 0,898** (tp 566, fp 64, fn 93). Le rappel passe de justesse ; la précision est juste sous le seuil de 0,90. Les 64 faux positifs sont tous des homographes (« cuisine » verbe, « bourse d'études », « sale » vs « salé »).

## Cas litigieux

- **Repli des accents** : « salé » = « sale », « lettré » = « lettre », « ciré » = « cire », « fumé » = « fume ». Ce sont des faux positifs structurels de la normalisation de l'ADR (tag `homograph`).
- **Fautes de frappe** : attendu `false` (fuzzy désactivé, aucune entrée n'a `fuzzy: true`). Si le fuzzy est activé pour des entrées, ces cas devront passer à `true`.
- **Négation** : « il n'y a pas de pont » reste `true` (ADR 0008).
- **Flexions non listées** (« tremblait », « boulangère », « minière ») : attendu `true` ; elles chiffrent le besoin de Snowball ou de mots-clés en plus (D11, D44).
- **Synonymes** (« taverne » pour auberge) : attendu `false`, par décision.
- **Mots-clés très larges des répliques** (« rendent », « cherchez », « attendait », « le roi ») : un homographe y est inévitable (tag `homograph`).
- **« rire » (trait)** : « riait » est `unlisted-inflection`.
