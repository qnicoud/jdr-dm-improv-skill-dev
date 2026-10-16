# 03 — Existant et UX : applications proches, patterns, positionnement

Date : 2026-10-16. Auteur : chercheur. Légende : **[Fait]** sourcé, **[Pratique]** pratique répandue, **[Avis]** opinion du chercheur.

## Résumé

- Les outils MJ existants sont des **générateurs** (tables, noms, PNJ, oracles) ou des **gestionnaires** (combats, campagne). Aucun trouvé ne propose un **entraînement chronométré avec trace de pratique**.
- Les apps d'impro et les minuteurs d'écriture couvrent chacun une moitié (prompts ou sprint), jamais le lien avec le MJ de JdR.
- Les séries (streaks) motivent mais angoissent : préférer jours de grâce, pas de remise à zéro punitive, message centré sur le chemin parcouru.
- Un minuteur qui se tait à la fin et laisse écrire respecte la spec ; les live regions ne conviennent pas à un compte à rebours seconde par seconde.
- Positionnement proposé : « salle de sport du MJ », pas un énième générateur.

**Limite méthodologique** : la recherche web disponible n'a donné ni avis utilisateurs du Play Store ni notes Android fiables. Les avis ci-dessous sont donc rares et marqués comme tels. Vérifier les fiches Play Store à la main avant de citer des notes.

## 1. Constats : applications existantes

| Outil | Fonction | Modèle éco. | Hors ligne | Forces | Manques |
|---|---|---|---|---|---|
| [Adventuresmith](https://github.com/stevesea/adventuresmith) (Android) | Générateurs aléatoires (noms, sorts, objets, monstres, PJ) | Gratuit, sans pub, GPL [Fait] | Non confirmé | Libre, attribution des sources par générateur | Pas d'entraînement ni de minuteur ; contenu sous licences tierces |
| [RPG Spark](https://www.appbrain.com/appstore/rpg-spark-dice-generators/ios-6758527880) | 200+ tables (auberges, rues, boutiques), dés, jeu en solo | Non précisé | « 100 % hors ligne » selon la fiche | Volume de tables | Fiche iOS : disponibilité Android à vérifier. Aucun suivi de progression |
| [Mythic GME Mobile](https://apps.apple.com/us/app/mythic-gme-mobile/id6726999147) (iOS) | Compagnon de l'oracle Mythic : table du destin, facteur de chaos, 50 tables (160+ en extension) | 2,99 $ + achats intégrés, sans abonnement [Fait] | Oui, conçu pour | 4,8/5 sur 32 notes (petit échantillon) | Ne remplace pas le livre ; outil de jeu, pas d'entraînement |
| [Game Master's Toolkit 5e](https://apps.apple.com/app/id1528248704) (iOS) | « Quick Generator » (noms, auberges, rumeurs…) | Non précisé | Non précisé | Générateur rapide en jeu | Idem : générer ≠ s'entraîner |
| Game Master 5e / D&D Beyond ([listing](https://portal.kettleandfire.com/dungeons-and-dragons-android/)) | Rencontres, bestiaire, campagne ; D&D Beyond : outils officiels, dés | D&D Beyond : contenu sur abonnement | Non précisé | Écosystème complet | Hors sujet pour l'improvisation ; source secondaire de faible qualité |
| [Improv Prompts](https://impronivers.itch.io/improv-prompts) (Android, itch.io) | 10 catégories de suggestions, catégories personnalisables | Téléchargement itch.io | Non précisé | Catégories éditables | Pas de minuteur ni de trace |
| [Improov](https://apps.apple.com/app/id6740037535), [Improv_e](https://apps.apple.com/us/app/improv-e/id6756239145), [Improv Tools](https://apps.apple.com/pl/app/improv-tools/id6477751204) (iOS) | Bibliothèques de jeux d'impro, scènes de départ, minuteur, rappel de règles | Gratuit + achats ; Improv Tools : non précisé | Non précisé | Catalogue d'exercices ; Improov ajoute annuaire de troupes | Pensés pour des groupes sur scène, pas pour la pratique solo |
| [ImPrompt](https://apps.apple.com/lc/app/imprompt/id1661950764) (iOS, 0,99 $), [Suggestions](https://apps.apple.com/us/app/id1661950764) (1,99 $, 4000+ prompts) | Prompts rapides, dont JdR (ImPrompt) | Achat unique | Non précisé | Rapidité | Un tirage, aucune structure d'exercice |
| [Write/Sprint](https://apps.apple.com/bb/app/write-sprint/id1558353979) (iOS), [QwikSprint](https://webmail.hunted.space/product/qwiksprint) (Android) | Minuteur de sprint d'écriture, mots écrits, badges de séries (QwikSprint), prompts | QwikSprint : « sans compte, sans pub » (page promotionnelle, à prendre avec recul) | Non précisé | Format sprint + série | Écriture romanesque ; pas de contrainte aléatoire structurée |
| [750 Words](https://www.writingcenter.pitt.edu/node/255) (web) | Écriture quotidienne, suivi de fréquence et de rythme | Non précisé | Non | Pratique quotidienne, statistiques | Web, en ligne, pas de contrainte |

Autres constats :
- **[Fait]** Aucun résultat de recherche ne montre une app combinant exercices d'impro/design/jazz adaptés au MJ, minuteur et historique. Absence de preuve, pas preuve d'absence : vérifier avec une recherche Play Store (« DM practice », « improv GM », « entraînement MJ »).
- **[Pratique]** Le créneau « générateurs hors ligne » est occupé (gratuit/libre, ou achat unique à 1–3 $). Se positionner là serait un mauvais pari. **[Avis]**
- **[Avis]** Les générateurs servent *pendant* la partie ; DM Gym sert *hors* partie. Les deux sont complémentaires, pas concurrents.
- Source inspirante du paradigme : [Impro de Keith Johnstone](https://www.labyrinthbooks.com/impro-improvisation-and-the-theatre/) est découpé en Statut, Spontanéité, Narration, Masques ; l'exercice 4 de la spec en est issu.

## 2. Constats : patterns UX

### 2.1 Séries et statistiques sans culpabilité
- **[Fait]** Un [article Smashing Magazine (fév. 2026)](https://smashingmagazine.com/2026/02/designing-streak-system-ux-psychology/) recommande : jours de repos/gel volontaire, courte période de grâce après l'échéance, dégradation progressive plutôt que remise à zéro. Il oppose habitude saine et compulsion (anxiété, estime de soi liée au chiffre) et propose un test éthique : le produit tire-t-il profit de l'anxiété qu'il crée ? Il rappelle que tout produit n'a pas besoin d'une série.
- **[Fait, source secondaire]** Duolingo a introduit des « Weekend Amulets » : +4 % de retour à une semaine et −5 % de séries perdues pour les utilisateurs concernés ([uxmag](https://uxmag.com/articles/the-psychology-of-hot-streak-game-design)). Chiffres issus de la société, non indépendants.
- **[Fait]** [Yu-kai Chou](https://yukaichou.com/?p=38864) critique la culpabilisation (mascotte triste après un oubli) et défend un design où le retour est récompensé.
- **[Avis]** Pour une app sans compte, sans notification serveur et sans monétisation, la pression de rétention n'a aucune justification économique. On peut être plus doux que Duolingo.

### 2.2 Minuteur et fin non brutale
- **[Fait]** [WCAG 2.2.1](https://www.w3.org/WAI/WCAG22/Understanding/timing-adjustable.html) (niveau A) : une limite de temps doit pouvoir être désactivée, ajustée (jusqu'à 10× la valeur par défaut) ou prolongée avec au moins 20 s pour agir. Les exceptions (temps réel, essentiel) existent ; **[Avis]** un exercice dont la contrainte de temps est le principe peut relever de « essentiel », mais l'option « durée réglable / sans minuteur » reste la solution sûre.
- **[Fait]** La [doc Compose sur la sémantique](https://developer.android.com/develop/ui/compose/semantics) recommande `liveRegion` en mode Polite, Assertive avec parcimonie, et déconseille les live regions pour les contenus très fréquents comme un compte à rebours (lecture via la version française, à confirmer sur la version anglaise).
- **[Pratique]** Fin en douceur : le minuteur passe en « temps additionnel » (compteur positif discret), changement de couleur *et* de forme/texte, vibration courte unique, pas de blocage du champ. Cela correspond déjà à la spec (« la saisie reste possible »).
- **[Pratique]** Avertissements intermédiaires (mi-parcours, dernière minute) annoncés une seule fois, pas chaque seconde.

### 2.3 Saisie rapide sous pression
- **[Pratique]** Champ focalisé à l'ouverture, clavier déjà levé, passage au champ suivant via l'action IME « Suivant » (Crazy Eights : 8 champs), aucune boîte de dialogue pendant le chrono, bouton « Terminer » toujours visible.
- **[Fait]** Pour que le clavier ne masque pas le champ : Android recommande `adjustResize` et, en Compose, `fitInside(WindowInsetsRulers.Ime.current)` ou `imePadding()` sur le conteneur défilant ([doc edge-to-edge](https://developer.android.com/agents/skills/system/edge-to-edge/skill) ; discussions [Kotlinlang Slack](https://slack-chats.kotlinlang.org/t/32817306/hi-all-has-anyone-solved-the-issue-where-the-keyboard-covers), 2022–2023, anciennes : à revérifier sur la version de Compose retenue).

### 2.4 Journal de pratique
- **[Pratique]** Liste chronologique avec aperçu (exercice, date, première ligne), filtre par exercice, détail éditable. Sauvegarde automatique en continu (SPEC) : afficher un état « enregistré » discret.
- **[Avis]** Ajouter une auto-évaluation optionnelle en un geste (3 émojis ou « à retravailler ») plutôt qu'un score : utile pour relire, sans transformer la pratique en notation.

### 2.5 Tirage présenté comme une carte
- **[Pratique]** Une carte (Card M3) avec catégorie en surtitre, contenu en grand, animation de retournement courte, bouton « Relancer » à pouce. Pour Status Numbers, la carte à jouer (As–10) est une métaphore naturelle.
- **[Avis]** Désactiver ou réduire l'animation si « Supprimer les animations » est actif ; prévoir un compteur de relances pour éviter la recherche du tirage parfait (contraire à la contrainte).
- **[Avis]** Les contraintes affichées doivent rester visibles pendant l'écriture (bandeau épinglé), sinon l'utilisateur doit quitter le champ.

## 3. Constats : saisie de texte long sur mobile
- **[Fait]** Ne pas se fier à la couleur seule pour les états (mots imposés surlignés, cases cochées) : ajouter forme, texte ou haptique ([principes d'accessibilité Android](https://developer.android.com/guide/topics/ui/accessibility/principles)).
- **[Pratique, non vérifié dans les sources lues]** Cibles tactiles ≥ 48 dp, contraste ≥ 4,5:1 pour le texte courant, tailles en `sp` pour respecter la taille de police système. À confirmer dans la [documentation d'accessibilité Android](https://developer.android.com/guide/topics/ui/accessibility/apps).
- **[Pratique]** Écrire sur 5–10 minutes avec un clavier virtuel fatigue : prévoir dictée vocale (fonction système, sans permission réseau côté app), correction automatique laissée au clavier, pas de limite de caractères, texte en 16 sp minimum, interligne confortable.
- **[Fait]** Piège technique : l'ancien `TextField` dans une colonne défilante pose des problèmes de défilement ([Slack Kotlin](https://slack-chats.kotlinlang.org/t/10503028/i-m-aware-of-imepadding-but-i-m-finding-it-insufficient-to-w)) ; privilégier `BasicTextField`/nouveaux états de texte et tester sur petit écran avec clavier.
- **[Avis]** Ne pas réinitialiser le texte à la rotation ni à la mise en arrière-plan : sauvegarde de brouillon dans Room toutes les quelques secondes.
- **[Avis, risque]** Surlignage des mots imposés et détection des 5 éléments par mots-clés en français (flexions, accents) : prévoir la normalisation (casse, accents) et un cochage manuel de secours.

## 4. Positionnement

**Proposition** : « La salle de sport du MJ » — des séances de 5 à 10 minutes, hors partie, avec une contrainte, un chrono et une trace. Pas un générateur de contenu pour la table, pas un outil de campagne.

Différenciation par rapport à l'existant :
1. Entraînement structuré (7 exercices tirés de disciplines différentes) face à de simples générateurs.
2. Journal personnel relisible, 100 % local, sans compte ni pub.
3. Sortie douce de la pression (fin du chrono non bloquante, séries bienveillantes).
4. Contenu original en français (créneau peu servi parmi les apps relevées ; **[Avis]**, non vérifié sur le Play Store).

Risques : durée de rétention faible sans notification ni compte ; valeur perçue difficile à expliquer en une phrase ; dépendance à la qualité des tables.

## Recommandations

### Indispensable
1. **Fin de minuteur douce** : bascule en temps additionnel, signal unique (vibration + son discret), jamais de verrouillage ni de dialogue. Annonces accessibles ponctuelles (mi-parcours, minute finale), pas de live region seconde par seconde.
2. **Durée réglable et option « sans minuteur »** pour chaque exercice (conformité WCAG 2.2.1 et inclusion).
3. **Séries bienveillantes ou absentes** : afficher d'abord « sessions cette semaine / depuis le début », la série en secondaire, avec jour de repos par semaine et sans remise à zéro punitive ni message de reproche.
4. **Écriture sans friction** : clavier levé à l'ouverture, contraintes épinglées, bouton « Terminer » visible, brouillon sauvegardé en continu, rien qui se perde en rotation ou retour arrière.

### Utile
5. **Tirage en carte** avec relance comptée ; animation réduite si l'utilisateur supprime les animations ; état des cartes jamais signalé par la couleur seule.
6. **Journal** : aperçu + filtre par exercice, auto-évaluation optionnelle en un geste, bouton « refaire cet exercice avec un nouveau tirage ».
7. **Dictée vocale** documentée dans l'écran de saisie (indice vers la fonction du clavier), sans développement spécifique.

### Optionnel
8. Export texte d'une session (partage Android) — reste hors ligne et sans compte.
9. Mini bilan hebdomadaire local (sans notification) à l'ouverture de l'app.

## Questions ouvertes

DÉCISION: Faut-il une série de jours consécutifs (spec) et sous quelle forme ? — (a) série classique, (b) série avec 1 jour de repos/semaine et sans reset punitif, (c) pas de série, seulement un compteur « sessions cette semaine » — recommandation : (b), avec (c) affiché en premier.

DÉCISION: Rappels de pratique par notification locale ? — (a) aucun, (b) rappel optionnel désactivé par défaut, hebdomadaire — recommandation : (b) ; demande la permission POST_NOTIFICATIONS (API 33+), à valider avec l'architecte vis-à-vis de la contrainte « zéro permission réseau » (non concernée, mais à documenter).

DÉCISION: Comment gérer WCAG 2.2.1 pour les exercices dont la contrainte est le temps ? — (a) durée réglable ×10 max + option sans minuteur, (b) durée fixe revendiquée comme essentielle — recommandation : (a).

DÉCISION: Auto-évaluation dans le journal ? — (a) aucune, (b) 3 niveaux optionnels, (c) note chiffrée — recommandation : (b).

DÉCISION: Vérification marché Play Store avant de figer le positionnement ? — (a) oui, 30 min de recherche manuelle (notes, avis, pub, hors ligne), (b) non — recommandation : (a), car la recherche web n'a pas fourni d'avis Android fiables.
