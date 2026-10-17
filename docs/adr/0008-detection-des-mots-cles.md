# ADR 0008 — Détection des mots-clés (Constraint Box, Connect the Story)

- Statut : acceptée (2026-10-16)
- Décisions de pilotage : D10 (principe), D11 (racinisation), D12 (fautes de frappe), D13 (cases cochées), D14 (`keywords` obligatoire)
- Sources : `docs/research/01-stack-technique.md` §5, `docs/research/02-pedagogie-exercices.md` §1.2 et §1.6

## Contexte

Deux exercices doivent repérer dans un texte libre en français des éléments imposés : les 3 mots sensoriels de Constraint Box et les 5 éléments de Connect the Story. Le français fléchit beaucoup (accents, pluriels, féminins, conjugaisons), et les faux positifs sont fréquents. Le pilotage classe ce risque comme **élevé**.

## Options

- Principe : détection automatique avec cochage manuel / cochage manuel seul.
- Racinisation : Snowball / règles maison / aucune (mots-clés seuls).
- Fautes de frappe : activée / désactivée / par entrée.
- Cases : recalcul pur / verrouillage / recalcul avec bascule manuelle.

## Décision

1. **La détection ne fait que pré-cocher, et le cochage manuel fait foi.** Chaque élément a deux états : `auto` (recalculé) et `manual: Boolean?`. L'état affiché vaut `manual ?: auto`. Dès que l'utilisateur touche une case, `manual` est fixé et le recalcul ne la modifie plus (D13). Les deux valeurs sont enregistrées en base.
2. **Moteur pur** `core/detection`, sans dépendance externe :
   - **Découpage en jetons** sur le texte d'origine avec l'expression `\p{L}[\p{L}\p{M}]*`, qui garde les positions. Les apostrophes (' et ’) et les traits d'union séparent les jetons, ce qui gère les élisions *l'*, *d'*, *qu'*.
   - **Normalisation de chaque jeton** : `lowercase(Locale.ROOT)` → NFD → retrait de `\p{Mn}` → œ→oe, æ→ae. Il n'y a pas de table de correspondance des positions : les plages de surlignage pointent directement dans le texte d'origine.
   - **Mots-clés du rédacteur** : chaque entrée détectable porte de 1 à 4 `keywords` (obligatoires dans `sensory_words.json` et `story_elements.json`). Un mot-clé peut compter plusieurs jetons (« arc-en-ciel », « vieille femme ») : il correspond alors à une suite contiguë de jetons.
   - **Correspondance exacte** des jetons normalisés par défaut, à travers une interface `Stemmer` dont l'implémentation v1 est `IdentityStemmer`. On recopiera Snowball français (classes BSD, licence vérifiée) derrière cette interface **seulement si** le corpus doré mesure un rappel insuffisant (D11). On n'ajoute ni Lucene ni l'artefact de 2014.
   - **Tolérance aux fautes** : désactivée par défaut. Elle s'active par entrée avec `"fuzzy": true` et ne s'applique qu'aux jetons du mot-clé de **8 lettres ou plus**, avec une distance de Damerau-Levenshtein ≤ 1 (D12).
   - Résultat : `DetectionResult(matches: Map<TargetId, List<IntRange>>)`. Un élément est détecté s'il a au moins une correspondance.
3. **Corpus doré obligatoire** : `src/test/resources/detection/corpus.json`, environ 100 phrases par type d'élément. Il contient les positifs attendus et des pièges (homographes, mot inclus dans un autre, négations). Un test paramétré calcule rappel et précision et **échoue sous les seuils** : rappel ≥ 0,85, précision ≥ 0,90. Ces seuils pourront être ajustés par un ADR après la première mesure. Les négations (« pas de lanterne ») restent des positifs acceptés : le cochage manuel corrige.
4. **Intégration à l'interface** :
   - Le surlignage passe par `TextFieldState` + `OutputTransformation` avec `addStyle(SpanStyle(fontWeight = Bold, textDecoration = Underline, background = …))`. On n'utilise jamais la couleur seule. Le moteur est appelé de façon synchrone et mémorisé sur le dernier texte. Objectif : moins de 1 ms pour 5 000 caractères. **Repli** : `VisualTransformation` + `OffsetMapping.Identity` sur un champ `TextFieldValue`, si `addStyle` n'est pas disponible ou trop lent sur Compose 1.12.1.
   - Les cases sont recalculées par `snapshotFlow { state.text }` avec un `debounce(300 ms)`, sur `Dispatchers.Default`.
   - La liste de cases hors du champ porte l'information pour TalkBack, puisque les styles ne sont pas annoncés.

## Conséquences

- La qualité de la détection dépend surtout des `keywords` rédigés. Le brief du `redacteur-contenu` doit le préciser : choisir les formes fléchies les plus probables (« lanterne », « lanternes », « lampe ») plutôt que des synonymes rares.
- Le corpus est un livrable du `testeur` (lot 4), à rédiger en parallèle du moteur.
- Brancher Snowball ne demande qu'une implémentation de `Stemmer`, appliquée des deux côtés (texte et mots-clés).

## Complément (2026-10-16, décision D44)

Les répliques (`story_elements`, catégorie réplique) gardent leurs `keywords` en suites de mots. Après la première mesure du corpus doré (lot L8), on ajoute des variantes aux entrées dont le rappel est insuffisant, plutôt que d'assouplir l'algorithme.

## Complément (2026-10-17, recette rc1, critères 1.5 et 1.12, amende D13)

Le cochage manuel fait toujours foi, **sauf** après un décochage : une **nouvelle occurrence** du mot le recoche automatiquement. Une nouvelle occurrence est un nombre d'occurrences détectées supérieur à celui du moment du décochage.

- `ElementCheck` mémorise le nombre d'occurrences du dernier recalcul (`occurrences`) et celui du décochage (`manualAt`). Ces deux valeurs ne sont pas enregistrées : après une reprise de séance, `manualAt` est inconnu et prend le compte du premier recalcul.
- Décocher puis continuer à taper sans nouvelle occurrence laisse la case décochée. Effacer du texte ne recoche jamais : le compte baisse, il ne dépasse pas la référence.
- Cas limite (revue R1a, assumé) : si le texte est vidé puis retapé jusqu'au nombre d'occurrences mémorisé au décochage (`manualAt`), le mot n'est pas recoché. C'est cohérent avec la règle « strictement plus d'occurrences qu'au décochage ». Un test de réducteur le fige.
- Un recochage supprime l'état manuel (`manual = null`) ; la case suit de nouveau la détection.
- Le cochage manuel n'existe que pendant l'exercice : l'écran Prêt affiche les mots et éléments en lecture seule.

## Complément (2026-10-18, v1.1) : détection par langue

Voir l'ADR 0016 §4 :
- les cibles viennent des tables de la **langue de la session** ;
- un `DetectionProfile` par langue (`IdentityStemmer` pour `fr` et `en`) ;
- un corpus doré par langue (`src/test/resources/detection/<lang>/corpus.json`), avec les mêmes seuils ;
- une langue n'est livrée que si son corpus est vert.
