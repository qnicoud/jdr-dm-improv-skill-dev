# Mesure de la détection de mots-clés (L4)

Moteur v1 (`IdentityStemmer`, aucun `fuzzy` dans les tables) mesuré sur le corpus doré
`app/src/test/resources/detection/corpus.json` (1375 cas, voir `detection-corpus.md`), contre les vraies
tables `story_elements` et `sensory_words`. La mesure est produite par `GoldenCorpusTest`, qui écrit le détail
dans `app/build/reports/detection-report.txt`.

## Verdict

| | Mesure | Seuil (ADR 0008) | |
|---|---|---|---|
| Rappel | **0,859** (tp 566, fn 93) | 0,85 | atteint, de peu |
| Précision | **0,898** (fp 64) | 0,90 | **manqué de 0,002** |

Le test `golden corpus meets the thresholds` échoue donc sur la précision, et `./gradlew check` est rouge. Aucun
changement n'a été fait aux tables, aux seuils ni à l'algorithme. Il suffit de passer de 64 à 62 faux positifs
pour atteindre 0,90.

## Faux positifs : 64, tous du tag `homograph`

Ils sont répartis sur 61 entrées. Trois entrées en ont deux : `lettre-cachetee` (« à la lettre », « lettré »),
`morts-ne-rendent-pas-qu-ils` et `fume`. Exemples : « Il cuisine un ragoût » (cuisine), « bourse d'études »
(bourse), « Sa chemise est sale » pour *salé* (repli des accents), « règlement de comptes ».
Catégories les plus touchées : `story_elements/sensory` (13 fp), `trait` (12), `line` (9), `object` (9),
`place` (8) ; `sensory_words/taste` (5 fp, précision 0,857).

## Faux négatifs : 93, tous du tag `unlisted-inflection`

Flexions ou conjugaisons absentes des `keywords` (« tremblait », « boulangère », « minière »). Deux entrées en
ont deux : `parfum-fleurs-fanees` et `sol-collant-sous-semelles`. Catégories : `story_elements/sensory`
(25 fn, rappel 0,775), `line` (15), `trait` (13) ; `sensory_words/smell` (8 fn, rappel 0,771) et `touch` (8).

## Par tag

| Tag | Résultat |
|---|---|
| direct (377), inflection (97), accent (55), elision (14), case (10), negation (12), punctuation (1) | tous corrects |
| homograph (64) | 64 faux positifs |
| unlisted-inflection (93) | 93 faux négatifs |
| substring, synonym, typo, neutral | tous corrects (négatifs attendus) |

Le moteur est sans défaut sur ce qui est listé. Toute l'erreur vient des deux tags qui mesurent volontairement
ses limites : le repli des accents et les flexions non listées.

## Options chiffrées (à trancher)

1. **Retirer ou affiner les mots-clés trop larges (D44).** Si l'on traite les trois entrées à deux faux
   positifs (`lettre-cachetee`, `morts-ne-rendent-pas-qu-ils`, `fume`), cela supprime 6 fp et donne une précision
   de 566/(566+58) = **0,907**, à condition de ne perdre aucun positif (à vérifier sur le rappel).
2. **Abaisser le seuil de précision à 0,89 par ADR.** Il passe aujourd'hui (0,898) sans toucher au contenu,
   mais sans marge.
3. **Ajouter les flexions manquantes aux `keywords`.** Le rappel gagne en marge (0,859, soit 0,009 au-dessus du
   seuil) et la précision n'est pas touchée. Chaque flexion ajoutée peut corriger un des 93 faux négatifs
   (+0,0015 de rappel).
4. **Snowball.** Il ne règle pas les homographes dus au repli des accents. Non retenu tant que l'option 3
   suffit (D11).

Recommandation : option 1 sur ces trois entrées, puis option 3 pour reprendre de la marge sur le rappel.

## Détail complet

`app/build/reports/detection-report.txt` après `./gradlew testDebugUnitTest` : mesures par catégorie et par
tag, puis la liste des décisions erronées (`FP`/`FN`, id du cas, entrée, tag).

## Réglage des mots-clés (D44)

Décision D44 : garder les mots-clés existants, puis ajouter des variantes après la première mesure. Seuls les
`keywords` de `story_elements.json` et `sensory_words.json` ont changé (90 entrées). Le corpus, le code, les
seuils et `fuzzy` sont intacts. Chaque entrée reste à 4 mots-clés au plus.

| | Avant | Après |
|---|---|---|
| Rappel | 0,859 (tp 566, fn 93) | **0,983** (tp 648, fn 11) |
| Précision | 0,898 (fp 64) | **0,918** (fp 58) |

`./gradlew check koverVerify` est vert.

### Précision (faux positifs 64 → 58)

- `morts-ne-rendent-pas-qu-ils` : `morts, rendent` → `les morts, morts ne rendent, ne rendent pas` (2 fp en moins).
- `fume` : `fumé, fumée, fumet` → `fumé, fumés, fumet, goût de fumée` (« une fumée noire » ne déclenche plus).
- `peureux-point-trembler` : `tremble` → `tremblait` (le végétal « un tremble » ne déclenche plus ; « sa voix tremble » est perdu).
- `tu-appelles-courage-je-appelle-dette` : ajout de `courageuse` seulement (`courageux` produisait un faux positif).
- `velours-froid-humide-sous-doigts` : `veloutée, veloutés`, sans `velouté` (le potage).
- `je-vous-ai-sauves-pour-cela` : `sauvés, sauvées`, sans `sauvé` (« sauve » le verbe, après repli des accents).
- `gouttes-cire-brulante-main`, `brulant`, `piquant` : on n'a pas ajouté `brûle` ni `pique` (homographes du verbe et de l'arme).
- `granuleux` : `sable` → `sables` (« une robe sable » ne déclenche plus ; « du sable fin » est perdu).

### Rappel (faux négatifs 93 → 11)

Ajout de flexions naturelles (pluriel, féminin, participe, imparfait), par exemple : `mine-inondee` +`minière` ;
`jardin-statues` +`jardins` ; `horloger` +`horlogers` ; `bateau` +`bateaux` ; `tout-ca-faute-boulanger`
+`boulangère, boulangers, boulangères` ; `prenez-moins-manteau` +`manteaux` ; `tutoiement` des répliques
(`quelque chose à te montrer`, `ne pas te le dire`, `cherches`) ; les adjectifs des mots sensoriels
(`dorés, dorées`, `moirés`, `résineuses`, `sucrés, sucrées`, `poivrés`, `rugueuses`, `visqueuses`, `tiédir`…).
`paresseux-reveur` : `paresse` → `rêveuse` ; `tetu-comme-mule` : `têtus` → `entêtée` ; `gout-fer-bouche`,
`odeur-huile-chaude`, `gouttes-cire-brulante-main` : pluriel seul (3 mots-clés ou moins).

### Limites restantes

- 58 faux positifs d'homographe : ils tiennent à des mots-clés légitimes (`marché`, `port`, `carte`, `livre`,
  `bourse`…). Sans contexte ni désambiguïsation, les retirer ferait perdre plus de vrais positifs qu'ils n'en
  évitent. `lettre-cachetee` (`lettré`, « à la lettre ») et `sale` (`salé` replié en `sale`) restent tels
  quels : les modifier coûterait des positifs directs du corpus.
- 11 faux négatifs : conjugaisons ou dérivés isolés (« crépite », « grincement », « il se brûle », « pique »).

## Surlignage (L8)

Le surlignage passe par `OutputTransformation` sur le `TextFieldState` (`TextFieldBuffer.addStyle`, stable dans
Compose Foundation 1.12.1) : le repli `VisualTransformation` de l'ADR 0008 n'a pas été nécessaire. Le style
cumule gras, soulignement et fond `tertiaryContainer` (jamais la couleur seule). Le texte n'est pas modifié, donc
le curseur ne bouge pas. `HighlightTransformation` mémorise le dernier texte : un rendu sans frappe ne relance pas la
détection. La transformation est recréée quand les tirages changent (relance).

Mesure (`HighlightPerformanceTest`, JVM, vraie table `story_elements`, 5 tirages de Connect the Story, texte de
5 046 caractères, moyenne de 200 appels après échauffement) :

| Appel | Durée |
|---|---|
| `KeywordAnswerDetector.highlights` (surlignage, à chaque frappe) | **0,23 ms** |
| `KeywordAnswerDetector.detect` (cases, à chaque frappe) | **0,21 ms** |

Objectif de l'ADR 0008 : moins de 1 ms pour 5 000 caractères, atteint. Le test échoue au-delà de 2 ms. La mesure
sur appareil réel reste à faire au lot L12 (l'appareil est plus lent que le poste de développement).

Écart avec l'ADR : les cases sont recalculées de façon synchrone à chaque changement du champ (point d'entrée
`AnswerDetector` du lot L5), sans `debounce(300 ms)` ni `Dispatchers.Default`. À 0,2 ms par frappe, le débounce
n'apporterait rien et compliquerait la sortie de l'écran (la dernière frappe doit être déjà détectée avant
l'écriture en base).
