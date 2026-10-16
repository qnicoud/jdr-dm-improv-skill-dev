# DM Gym — Parcours, navigation et inventaire des écrans

Date : 2026-10-16. Auteur : ux-designer. Entrées : `SPEC.md` (rév. 2026-10-16), `docs/pilotage.md` (D1-D24 tranchées), `docs/research/02` et `03`.
Les décisions D1-D24 ne sont pas rediscutées. Les points nouveaux sont en `DÉCISION:` (section 8).
Documents liés : `design-system.md` (tokens, composants), `brief-maquettes.md` (écrans à maquetter).

## 1. Principes UX (chacun justifié)

1. **Écrire d'abord, régler ensuite.** Un exercice démarre en 2 touches depuis l'accueil, car la session dure 30 s à 10 min.
2. **La consigne et le chrono ne quittent jamais l'écran.** Zone épinglée en haut, champs défilants dessous, car le clavier mange la moitié de l'écran.
3. **Aucun dialogue, aucun blocage pendant l'exercice.** Retour système = brouillon enregistré ; fin du temps = « temps additionnel ». Un dialogue casse le flux et nourrit l'autocritique.
4. **Jamais de jugement.** Pas de rouge hors vraie erreur technique, pas de score, pas de « 6/8 insuffisant ». Les compteurs informent (« 6 idées »), ils ne notent pas.
5. **Chaque état se lit sans la couleur** : forme, icône ou texte en plus (WCAG 1.4.1).
6. **La relecture est la récompense.** Historique, bijoux et compteur de sessions mettent en valeur ce qui a été produit, pas la régularité.
7. **Ambiance JdR en surface, lisibilité en profondeur.** Le décor (palette, titres, icônes) ne touche jamais la zone de saisie.

## 2. Vocabulaire (à respecter dans tous les textes)

| Terme | Sens | Remarque |
|---|---|---|
| **Exercice** | Un des 7 types (Crazy Eights, …) | |
| **Session** | Un exercice réalisé et enregistré (ligne d'historique) | SPEC : « nombre de sessions » |
| **Séance du jour** | Suggestion de 3 exercices | Ne pas confondre avec « session » (voir DÉCISION 5) |
| **Tirage** | Contenu aléatoire de l'exercice (problème, lieu, 5 éléments…) | |
| **Relance** | Nouveau tirage ; compté dans la session | |
| **Temps additionnel** | Après la fin du minuteur, saisie toujours possible | Jamais « temps écoulé / échec » |
| **Bilan** | Écran facultatif d'auto-évaluation après « Terminer » | |
| **Bijou** | Session marquée « à garder » | |
| **Brouillon** | Session commencée, non terminée | |

## 3. Architecture de l'information et navigation

```
Premier lancement (une fois)
└─ Accueil  ─────────────── barre de navigation (3 destinations)
   ├─ [Onglet 1] S'entraîner (Accueil)
   │    ├─ Carte « Reprendre » (si brouillon)
   │    ├─ Séance du jour → Aperçu → exercice 1 → Bilan → exercice 2 → … → Fin de séance
   │    └─ Liste des 7 exercices → Exercice (Prêt → En cours → Bilan)
   ├─ [Onglet 2] Historique → Détail de session (→ Refaire l'exercice)
   └─ [Onglet 3] Progrès (statistiques)
   Réglages : icône engrenage dans la barre du haut de l'Accueil (pas un onglet)
```

Règles :
- **3 onglets** (Entraîner, Historique, Progrès) : M3 autorise 3 à 5 ; Réglages est rare, donc relégué en barre d'application (voir DÉCISION 10).
- **La barre de navigation disparaît** sur tous les écrans d'exercice et de bilan : plein écran concentré, plus de hauteur pour le clavier.
- **Retour système / flèche** sur un exercice en cours : retour à l'accueil, brouillon enregistré, snackbar « Brouillon enregistré » (3 s). Pas de confirmation.
- **Retour depuis le Bilan** : va au Détail de session (la session est déjà enregistrée) puis à l'accueil. Jamais de retour vers un exercice terminé.
- **Pile de navigation conservée par onglet** (état de scroll et filtres de l'Historique mémorisés).
- **Rotation** : aucun état perdu (texte, minuteur, case cochée, scroll).
- **Mode paysage téléphone avec clavier** : la zone épinglée se réduit à une ligne (minuteur + bouton « Consigne ») ; le mode plein écran du clavier (extract UI) est désactivé.
- **Tablette / ≥ 600 dp** : Historique en liste + détail côte à côte ; Exercice en deux colonnes (consigne et minuteur à gauche, champs à droite).

## 4. Parcours

Notation des états : **V** vide, **C** chargement, **E** en cours, **T** temps additionnel, **S** sans minuteur, **X** erreur de table.

### 4.1 Premier lancement

| | |
|---|---|
| Déclencheur | Première ouverture, aucune session en base |
| But | Comprendre en 10 s à quoi sert l'app, lancer un exercice sans configuration |
| Étapes | 1. **S01 Bienvenue** (un seul écran, pas de carrousel). 2a. « Essayer un premier exercice » → S04 Constraint Box en état Prêt (5 min, le plus court). 2b. « Voir tous les exercices » → S02 Accueil. |
| Permissions | Aucune demandée. La ligne « Tout reste sur ton téléphone. Pas de compte. » le dit. |
| États | V : accueil avec compteur « 0 session », séance du jour déjà proposée, Historique et Progrès en état vide encourageant. Pas d'état de chargement visible (< 300 ms ; le contenu JSON est lu à l'ouverture d'un exercice). |
| Erreur | Aucune possible à ce stade. |
| Justification | Un seul écran évite l'abandon ; Constraint Box a la plus courte durée avec saisie, donc le premier succès est rapide. |

### 4.2 Séance du jour (suggérée, chaque exercice remplaçable)

| | |
|---|---|
| Déclencheur | Carte « Séance du jour » sur S02 |
| Étapes | 1. **S03 Aperçu** : date, 3 exercices (nom, durée, discipline), durée totale (≈ 14 min), « Remplacer » sur chaque ligne. 2. « Remplacer » → feuille S03b : liste des autres exercices (One-Page inclus, indiqué « Préparation »), choix en 1 touche, la ligne se met à jour. 3. « Commencer » → exercice 1 (S04 Prêt) avec un fil « Séance du jour · 1 sur 3 » dans la barre du haut. 4. Après « Terminer » : S06 Bilan, bouton principal « Exercice suivant » (au lieu de « Terminé »). 5. Après l'exercice 3 : **S07 Fin de séance** (récap, compteur). |
| Remplacement | Mémorisé pour la journée : revenir sur S03 ne retire pas le choix. |
| Sauter | « Passer cet exercice » en haut de S04 Prêt et dans S03 ; l'exercice reste « Non fait ». |
| Arrêter en cours | Retour système : brouillon enregistré, retour à S03 qui montre l'avancement (Fait / En cours / À faire). |
| États | V : jamais vide (3 exercices toujours proposés). Déjà faite : S03 montre « Séance terminée » + « Refaire un exercice ». Lendemain : nouvelle suggestion (graine = date locale). |
| Erreur | Si la table d'un exercice est illisible, la ligne reste mais S04 affiche l'état X (voir 4.3). |
| Justification | Imposer un ordre ou une validation serait une obligation ; la SPEC demande une suggestion (D20). |

### 4.3 Exercice libre (le cœur de l'app)

Phases d'un exercice, identiques pour les 7 (sauf mention) :

1. **Prêt (S04)** : consigne courte, tirage en carte, réglages rapides (durée, sans minuteur, options propres à l'exercice), bouton principal « Commencer ». Le clavier n'est pas ouvert. On peut relancer, changer la durée, lire.
2. **En cours (S05)** : le chrono tourne, la consigne et le tirage sont épinglés, le clavier est ouvert sur le premier champ. « Terminer » en barre du haut.
3. **Temps additionnel** : voir 4.4.
4. **Terminé** : « Terminer » → S06 Bilan. La session est déjà enregistrée.

Déroulé :

| Étape | Utilisateur | Système |
|---|---|---|
| 1 | Touche un exercice dans S02 | Ouvre S04. Lit la table et tire. Animation de retournement de la carte (≤ 300 ms, instantané si animations réduites). |
| 2 | (facultatif) Relance, change la durée, active « Sans minuteur » | Compteur de relances +1 ; durée sauvegardée comme valeur de cet exercice (réglable aussi dans Réglages). |
| 3 | Touche « Commencer » | Minuteur démarre (horodatage), écran maintenu allumé, clavier s'ouvre, champ 1 focalisé, vibration légère de départ désactivée par défaut. |
| 4 | Écrit | Sauvegarde automatique toutes les ~3 s et à chaque changement de champ ; témoin discret « Enregistré ». Détection de mots (Constraint Box, Connect the Story) recalculée pendant la frappe. |
| 5 | Touche « Terminer » (ou le temps passe, voir 4.4) | Ferme le clavier, ouvre S06. |

États de l'exercice :

| État | Ce que voit l'utilisateur |
|---|---|
| **C** | Squelette de la carte de tirage, 1 seconde au plus. Aucun écran blanc. |
| **V** (champs vides) | Champs avec numéro et suggestion discrète (« Ta première idée, même mauvaise »). Aucune erreur pour champ vide. |
| **E** | Minuteur décroissant, compteur de progression propre à l'exercice (« 3/8 »). |
| **T** | Voir 4.4. |
| **S** | Puce « Sans minuteur » à la place du chrono ; aucune échéance ; le temps réel passé est quand même enregistré. Toucher la puce propose « Lancer un minuteur » (durée par défaut). |
| **X** | Voir ci-dessous. |
| Pause | Texte « En pause » + icône ; saisie toujours possible (voir DÉCISION 1). |

**Erreur de table (X)** : fichier manquant, JSON invalide ou table vide. La carte de tirage est remplacée par une carte d'erreur : titre « Les tirages de cet exercice sont indisponibles », texte « Rien n'est perdu : tes sessions sont intactes. », actions « Réessayer » et « Retour ». Si l'exercice accepte un sujet libre : « Écrire mon propre sujet » (DÉCISION 12). Le ton reste neutre et sans jargon ; le détail technique est dans Réglages > À propos > Diagnostic.

**Reprise d'un brouillon** : sur S02, une carte « Reprendre » en haut (exercice, dernier extrait de texte, temps écoulé). Elle ouvre S05 là où on s'était arrêté ; si le minuteur est passé, on reprend en temps additionnel. Voir DÉCISION 9 pour la durée de vie.

**Variantes par exercice** (détails dans l'inventaire, section 6) :

| Exercice | Prêt | En cours |
|---|---|---|
| Crazy Eights | Problème tiré | 8 champs numérotés, « x/8 » |
| Constraint Box | Lieu + 3 mots sensoriels | 1 zone de texte, mots surlignés, puces cochables |
| Core Image | Image centrale + nombre de questions | 1 question à la fois, champ court, « Question 3 sur 6 » |
| Status Numbers | Carte 1-10 + rôle, options (contre-stéréotype, bascule) | Pas de clavier : jeu à voix haute ; bandeau de bascule |
| Decision Point | Choix du mode : vidéo ou situation tirée | 3 champs, 30 s, case « Je ne ferais jamais ça » par réponse |
| Connect the Story | 5 éléments | 1 zone de texte, 5 éléments épinglés et cochables |
| One-Page | Mode : ma campagne / situation tirée ; minuteur désactivé par défaut | Formulaire PNJ, motivation, liens |

### 4.4 Fin de minuteur (entrée en temps additionnel)

Séquence à 0:00, sans interrompre la saisie :

1. Le minuteur **change de forme, de texte et de couleur** : la barre de progression vide devient une barre pleine à rayures, le texte passe de « 0:00 » à « Temps additionnel +0:00 », l'icône sablier devient « + ». Conteneur : tertiaire (laiton), pas rouge.
2. **Vibration courte unique** (si activée dans Réglages), **bip discret** (SoundPool, uniquement en mode sonnerie normal).
3. **Annonce TalkBack unique** (polite) : « Temps écoulé. Tu peux continuer à écrire. »
4. Le bouton « Terminer » passe de tonal à plein : invitation douce, pas d'injonction.
5. Le clavier, le focus, la position du curseur et le défilement ne changent pas.
6. Le décompte positif continue ; la valeur est enregistrée (« 8 min + 1 min 12 s »).

Jamais : dialogue, écran de blocage, message « trop tard », texte rouge, perte de focus.
Paliers avant la fin : une annonce unique « Il reste 1 minute » (visuelle : icône sablier plein + texte « Dernière minute » ; TalkBack une fois) pour les durées ≥ 3 min. Pas de palier pour Decision Point (30 s).
Si l'app était en arrière-plan à la fin : au retour, l'exercice est en temps additionnel, sans vibration différée ni son. Pas de notification (hors v1).

### 4.5 Auto-évaluation et « bijou »

| | |
|---|---|
| Déclencheur | « Terminer » sur S05 |
| Écran | **S06 Bilan** : récap neutre d'une ligne (« 6 idées · 8 min 12 s »), puis trois blocs facultatifs, chacun sautable. |
| 1. Niveau | Sélecteur 3 niveaux : « Brouillon », « Solide », « Prêt pour la table » (icône + texte). Touche = sélection ; seconde touche = désélection. |
| 2. Grille | 1 à 3 questions Oui/Non propres à l'exercice (tableau en 6, S06), repliées par défaut, 10 s. |
| 3. Bijou | Bouton à bascule « Garder comme bijou » (gemme vide/pleine + texte « Bijou »). |
| Sorties | « Terminé » (ou « Exercice suivant » en séance du jour) enregistre. « Passer » enregistre aussi sans évaluation. Quitter par retour système : même effet. Rien n'est perdu, rien n'est obligatoire. |
| Après | Séance libre : retour à S02 avec snackbar « Session enregistrée » + action « Voir ». Si c'était la première session de la journée : le compteur progresse, sans fanfare. |
| Modifiable | L'évaluation et le bijou se changent plus tard dans S09. |
| Justification | Écran distinct plutôt que feuille modale : le clavier est fermé, on peut respirer ; facultatif et rapide pour ne pas devenir une corvée. |

### 4.6 Historique et détail

1. **S08 Historique** : liste antéchronologique groupée par jour (« Aujourd'hui », « Hier », « 12 octobre »), chaque ligne = icône d'exercice, nom, heure, aperçu (1-2 lignes), badges (bijou, niveau). Filtres en puces : « Tous », les 7 exercices (défilement horizontal), « Bijoux ». V : « Ton carnet est vide. Fais un premier exercice : il apparaîtra ici. » + bouton « Choisir un exercice ». Filtre sans résultat : « Aucune session pour ce filtre. » + « Effacer le filtre ».
2. **S09 Détail** : en-tête (exercice, date, durée dont temps additionnel, nombre de relances), tirage, texte(s) saisi(s) en lecture confortable, évaluation et bijou modifiables, actions « Refaire avec un nouveau tirage », « Partager le texte » (DÉCISION 15), « Supprimer » (avec annulation par snackbar, pas de dialogue).
3. Le texte reste modifiable depuis le détail ? **Non en v1** : le détail est une trace, pas un éditeur (évite de réécrire après coup). Voir DÉCISION 13b.
4. Session « inachevée » : badge « Inachevée », texte conservé.

### 4.7 Statistiques (« Progrès »)

Ordre fixe, du plus encourageant au plus secondaire :
1. **Compteur de sessions** en très grand (« 47 sessions »), avec « dont 6 cette semaine ».
2. **Série de jours** avec jour de grâce : « Série : 5 jours », sous-texte « Jour de grâce disponible » ou « Jour de grâce utilisé cette semaine ». Rangée des 7 derniers jours (3 symboles : pratiqué, jour de grâce, rien).
3. **Meilleure série** (conservée même après une pause).
4. **Répartition par exercice** : barres horizontales avec le nombre à droite ; l'exercice le moins pratiqué n'est pas pointé du doigt.
5. **Bijoux** (nombre + lien vers l'historique filtré) et **temps passé** cumulé.
V (0 session) : « Tes progrès apparaîtront après ta première session. » et aucun zéro écrasant : les blocs sont masqués.
Série rompue : « Nouvelle série : 1 jour. Meilleure série : 12 jours. » Jamais de texte « perdue », « raté », « tu as cassé ».
Règles : le jour bascule à minuit local (D16). Un « jour pratiqué » = au moins une session avec contenu (DÉCISION 8).

### 4.8 Réglages

Sections (S11) :
- **Minuteur** : vibration à la fin (oui par défaut), bip (oui, ne sonne qu'en mode sonnerie), durée par défaut par exercice (7 lignes, stepper + « Sans minuteur »), rythme Crazy Eights (vibration chaque minute, non par défaut ; DÉCISION 14).
- **Apparence** : thème Clair / Sombre / Système (défaut Système) ; « Réduire les animations » (défaut : suit le système).
- **Données** : « Exporter mon historique » (fichier texte via le sélecteur système), « Supprimer tout l'historique » (confirmation par saisie du mot ou appui long : seul dialogue de l'app, car destructif et hors exercice).
- **À propos** : version, licences (polices, bip), sources des exercices (Johnstone, GV, Edariad), diagnostic des tables.
Pas de notifications (v1.1). Pas de compte.
Chaque réglage s'applique immédiatement, sans bouton « Enregistrer ».

## 5. Inventaire des écrans (vue d'ensemble)

| ID | Écran | Accès | Navigation basse | États à maquetter |
|---|---|---|---|---|
| S01 | Bienvenue | 1er lancement | non | normal |
| S02 | Accueil | Onglet 1 | oui | V (0 session), normal, brouillon à reprendre, séance du jour faite |
| S03 | Aperçu séance du jour | Carte S02 | non | normal, partielle, terminée ; S03b feuille Remplacer |
| S04 | Exercice, Prêt | Liste S02 / S03 | non | C, normal, S (sans minuteur), X (erreur de table), feuille Durée |
| S05 | Exercice, En cours | S04 | non | V, E, dernière minute, T, S, pause, clavier ouvert |
| S06 | Bilan | S05 | non | normal, avec grille ouverte, après sélection |
| S07 | Fin de séance du jour | S06 (3e) | non | normal |
| S08 | Historique | Onglet 2 | oui | V, normal, filtre sans résultat |
| S09 | Détail de session | S08 | non | normal, inachevée, avec bijou |
| S10 | Progrès | Onglet 3 | oui | V, normal, série rompue |
| S11 | Réglages | Engrenage S02 | non | normal ; sous-écran Durées |
| S12 | À propos / Licences | S11 | non | normal |

Le minuteur et les champs sont des composants (design-system.md), pas des écrans.

## 6. Écrans en détail

Format : Contenu, hiérarchie, actions, états, clavier, accessibilité. Les textes entre guillemets sont les libellés (à placer dans `strings.xml`).

### S01 Bienvenue
- **Contenu (haut en bas)** : icône d'app/haltère-dé, titre « Bienvenue à la salle » (headlineLarge), phrase « Des exercices courts pour muscler ton improvisation de MJ. », trois lignes icône + texte : « Une contrainte », « Un chrono », « Une trace de ce que tu écris » ; ligne discrète « Tout reste sur ton téléphone. Pas de compte. »
- **Actions** : primaire « Essayer un premier exercice (5 min) » ; secondaire « Voir tous les exercices ».
- **États** : unique.
- **Clavier** : sans objet. **A11y** : ordre de lecture titre, 3 lignes, boutons ; titre marqué heading.

### S02 Accueil
- **Hiérarchie** : (1) barre « DM Gym » + engrenage ; (2) bandeau compteur « 47 sessions · série 5 jours » (compact, lien vers S10) ; (3) carte « Reprendre » si brouillon ; (4) carte « Séance du jour » ; (5) liste « Tous les exercices » (7 cartes d'exercice) ; (6) barre de navigation.
- **Carte Séance du jour** : titre, date, 3 puces d'exercice (icône + nom), « ≈ 14 min », bouton « Commencer » ; une coche « Fait » sur chaque exercice déjà réalisé.
- **Carte d'exercice** : icône, nom, discipline d'origine (« design thinking »), durée (« 8 min » ou « Sans minuteur »), chevron. One-Page porte l'étiquette « Préparation ».
- **États** : V (« 0 session » ; la série est masquée), normal, brouillon, séance faite (la carte devient « Séance du jour terminée · Refaire un exercice »).
- **Interactions** : toucher une carte ouvre S04. Défilement vertical ; la barre du haut se réduit au défilement (scroll behavior M3).
- **A11y** : chaque carte d'exercice est un seul élément focalisable, description « Crazy Eights, design thinking, 8 minutes. Double tape pour ouvrir. » ; le bandeau compteur se lit « 47 sessions, série de 5 jours ».

### S03 Aperçu de la séance du jour (+ S03b)
- **Contenu** : titre « Séance du 16 octobre », durée totale, 3 lignes (numéro, icône, nom, discipline, durée, état Fait / En cours / À faire, action « Remplacer »), bouton « Commencer » (devient « Continuer » si partielle), lien « Passer cet exercice » par ligne.
- **S03b (feuille modale basse)** : « Remplacer par… » liste des exercices non présents ; touche = remplace et ferme. Bouton « Annuler » inutile : glisser ou toucher à l'extérieur.
- **Clavier** : sans objet. **A11y** : « Remplacer » a une description « Remplacer Constraint Box » ; focus rendu à la ligne après fermeture de la feuille.

### S04 Exercice, Prêt
- **Hiérarchie** : barre du haut (retour, nom de l'exercice, durée en puce ; en séance : « 1 sur 3 ») ; consigne en 1-2 phrases ; **carte de tirage** ; réglages rapides ; bouton « Commencer » fixé en bas (au-dessus de la zone gestuelle).
- **Réglages rapides** : puce « 8 min » (ouvre une feuille : stepper 1-60 min, bouton « Sans minuteur », « Rétablir la durée par défaut ») ; options propres à l'exercice (Status : contre-stéréotype, bascule ; Decision Point : mode ; One-Page : mode).
- **Relance** : bouton « Relancer le tirage » sous la carte, compteur « 0 relance » devenant « 1 relance ». Désactivé pendant l'animation de retournement.
- **États** : C (squelette de carte), normal, S (puce « Sans minuteur »), X (carte d'erreur, voir 4.3).
- **Clavier** : fermé. Exception : sujet libre en état X ou mode « ma campagne » (One-Page) : champ focalisé à la demande, pas automatiquement.
- **A11y** : à l'arrivée, focus sur le titre ; le tirage s'annonce une fois (« Tirage : … »). Après relance, annonce polite du nouveau tirage. Le bouton « Commencer » est atteignable en 3 balayages maximum depuis la carte.

### S05 Exercice, En cours (structure commune)
- **Zones, de haut en bas** :
  1. Barre du haut : retour, nom court, indicateur « Enregistré », **« Terminer »** (bouton tonal ; plein en temps additionnel). En haut pour ne jamais être masqué par le clavier.
  2. **Zone épinglée** : minuteur + consigne/tirage compact. Hauteur max 35 % de la fenêtre visible (clavier compris).
  3. **Zone défilante** : champs ou zone de texte. Le champ focalisé reste visible au-dessus du clavier.
  4. Clavier système, avec action IME adaptée.
- **Minuteur** : états Prêt/En cours/Dernière minute/Temps additionnel/Sans minuteur/Pause (design-system §5.3).
- **Pause** : bouton secondaire dans la zone du minuteur ; en pause, texte « En pause », saisie permise, durée de pause non comptée.
- **Écran maintenu allumé** pendant S05 uniquement.
- **Retour système** : voir §3.
- **A11y minuteur** : un seul élément, description « Minuteur : 4 minutes 30 restantes », mise à jour à la demande ; aucun live region seconde par seconde ; annonces ponctuelles aux jalons (début, 1 minute, fin).

#### S05-1 Crazy Eights
- **Épinglé** : problème (« Les joueurs ignorent l'accroche que tu avais préparée. Comment les ramener vers l'aventure ? »), minuteur, puce « 5/8 » + 8 segments (rempli = plein, vide = contour).
- **Champs** : 8 champs numérotés, 1-3 lignes qui s'agrandissent. Action IME **Suivant** passe au champ suivant ; sur le 8e : **Terminé** ferme le clavier (ne termine pas l'exercice).
- **Défilement** : le champ actif se cale juste sous la zone épinglée.
- **États** : V (« Idée 1 » placeholder), E, T. Pas de message pour « 6/8 ».
- **Rappel** : option de vibration chaque minute (Réglages).

#### S05-2 Constraint Box
- **Épinglé** : lieu (« Auberge de relais au bord d'un gué en crue »), 3 puces de mots imposés avec icône du sens (ouïe, odorat, toucher…) et case (cochée = coche + « utilisé »), minuteur.
- **Saisie** : une zone de texte large, 18 sp. Mots imposés surlignés dans le texte (fond tertiaire + soulignement), détection recalculée, **le cochage manuel fait foi** (D10, D13).
- **Clavier** : action IME retour à la ligne (texte libre) ; pas de « Suivant ».
- **Variantes** : si un seul domaine en v1, pas de sélecteur de domaine (DÉCISION 13).

#### S05-3 Core Image Threading
- **Épinglé** : image centrale (« Une clé rouillée à laquelle il manque une dent »), minuteur, « Question 3 sur 6 » avec 6 points (courant = point plein plus grand).
- **Zone** : question en grand (titleLarge), un champ de réponse (1 à 3 lignes), boutons « Précédente » (texte) et « Question suivante » (tonal) en bas, au-dessus du clavier.
- **Clavier** : action IME **Suivant** = question suivante. Les réponses précédentes restent accessibles via « Précédente » et sont listées dans le détail.
- **Dernière question** : le bouton devient « Terminer ». « Passer cette question » est un lien texte discret.

#### S05-4 Status Numbers
- **Pas de saisie pendant le jeu** : le clavier reste fermé ; l'écran sert de rappel.
- **Contenu** : carte à jouer (valeur en grand + libellé « Statut 8 sur 10 · haut »), rôle (« Percepteuse des taxes du port »), minuteur, repliable « Rappel » (statut ≠ rang ; haut : contrôle la conversation, regard direct ; bas : hésite, se coupe, évite le regard).
- **Bascule** (si activée) : à 40-60 % du temps, un **bandeau de bascule** apparaît sous le minuteur (« Bascule : on vient de lui apprendre que son supérieur est dans la salle. »), vibration courte, annonce TalkBack unique ; reste affiché jusqu'à la fin.
- **Action** : « Scène terminée » en bas (grand bouton). Les notes facultatives sont saisies dans S06.
- **Option « à voix basse / mimé »** : rappel de texte dans Prêt, pas d'écran supplémentaire.

#### S05-5 Decision Point Drill
- **Prêt, mode Vidéo** : consigne « Mets ta vidéo en pause quand un joueur te surprend, puis lance le chrono. Entraîne-toi à ce que ta première idée soit meilleure. » ; gros bouton « Pause faite : lancer 30 s ».
- **Prêt, mode Situation** : carte de situation tirée (« Un joueur annonce qu'il brûle la bibliothèque du temple. »).
- **En cours** : situation (si mode Situation), minuteur (30 s : affichage « 0:30 » en secondes entières), 3 champs courts numérotés, chacun avec une **case « Je ne ferais jamais ça »**, hauteur de case 48 dp.
- **Clavier** : IME Suivant d'un champ à l'autre, Terminé sur le 3e.
- **T** : très probable (30 s est court) ; même traitement qu'ailleurs, jamais punitif.
- **Bilan** : si aucune case « jamais » cochée, une phrase neutre : « Tu peux cocher ici une réponse que tu ne ferais jamais. »

#### S05-6 Connect the Story
- **Épinglé** : panneau des **5 éléments** (lieu, objet, trait de personnage, détail sensoriel, réplique) avec case par élément, minuteur, compteur « 3 sur 5 reliés ». Deux modes : **déployé** (5 lignes complètes, avant de commencer et à la demande) et **compact** (5 puces icône + première partie, hors clavier ouvert), bascule par chevron. Quand le clavier s'ouvre : compact automatiquement.
- **Cases** : cochées automatiquement quand le mot-clé est détecté (étiquette « auto »), cochables à la main ; **le geste manuel l'emporte** jusqu'à ce que l'utilisateur le change. La réplique n'est pas détectée (cochée à la main par défaut).
- **Zone** : un texte long, mots détectés surlignés.
- **Clavier** : retour à la ligne normal.

#### S05-7 One-Page Design
- **Prêt** : mode « Ma campagne » (champ « Décris la situation ») ou « Situation tirée » (carte). Minuteur : **désactivé par défaut** (« Sans minuteur », option « Conseillé : 15 min »).
- **En cours** : formulaire structuré scrollable : Situation (rappel en haut, épinglé réduit), liste de PNJ (cartes : nom, motivation, lien avec un autre PNJ), bouton « Ajouter un PNJ » (plafond 5, DÉCISION 13), champ « Ce qui se passe si les PJ ne font rien » (DÉCISION 13).
- **Clavier** : IME Suivant à travers les champs d'une carte PNJ ; la carte du PNJ actif reste visible.
- **v2** : schéma de relations, hors périmètre.

### S06 Bilan
- **Hiérarchie** : (1) titre « Session enregistrée » (neutre) ; (2) récap d'une ligne ; (3) **Sélecteur de niveau** ; (4) grille « Quelques questions » repliée ; (5) bouton **Bijou** ; (6) pour Status Numbers : champ « Notes (facultatif) » ; (7) actions « Terminé » / « Exercice suivant » ; lien « Passer ».
- **États** : normal ; grille ouverte ; avec session de séance du jour.
- **Grilles par exercice** :

| Exercice | Questions Oui/Non |
|---|---|
| Crazy Eights | « Ai-je dépassé mes 3 premières idées ? » ; « Une idée m'a surpris ? » |
| Constraint Box | « Ai-je évité le visuel facile ? » ; « Pourrais-je le dire à voix haute tel quel ? » |
| Core Image | « Un lien m'a surpris ? » ; « Ai-je une phrase à dire à ma table ? » |
| Status Numbers | « Mon statut se lisait-il sans mots ? » ; « Ai-je évité le cliché ? » |
| Decision Point | « Ma première idée était-elle jouable ? » ; « Ai-je osé une réponse « jamais » ? » |
| Connect the Story | « La connexion semblait-elle prévue ? » ; « Ai-je relié un élément inattendu ? » |
| One-Page | « Tout tient en une page ? » ; « Je peux jouer ça demain ? » |

- **Clavier** : fermé. **A11y** : le sélecteur est un groupe de 3 boutons radio ; « Aucun niveau sélectionné » annoncé par défaut ; le bijou annonce « Bijou, activé / désactivé ».

### S07 Fin de séance du jour
- Titre « Séance terminée », 3 lignes (exercice, durée, niveau si donné), compteur « 48 sessions », série « 6 jours », bouton « Retour à l'accueil », lien « Refaire un exercice ». Pas de confettis ; une petite animation de gemme (ignorée si animations réduites).

### S08 Historique
- **Contenu et états** : voir 4.6. **Clavier** : sans objet. **Interactions** : toucher ouvre S09 ; appui long ou glissement : pas de suppression en liste (évite les erreurs) ; suppression depuis S09.
- **A11y** : ligne = un élément, lecture « Constraint Box, hier 18 h 12, 3 minutes, bijou, Solide. Aperçu : … ».

### S09 Détail de session
- **Hiérarchie** : en-tête (icône, exercice, date, durée), badges (relances, temps additionnel, inachevée), tirage (carte), texte(s) saisi(s) avec mots imposés surlignés + cases d'élément en lecture, niveau + bijou modifiables, actions.
- **Actions** : « Refaire avec un nouveau tirage » (bouton principal), « Partager le texte » (icône), « Supprimer » (menu ; annulation par snackbar).
- **États** : normal, inachevée, avec/sans évaluation (zone d'évaluation montre « Pas d'évaluation » discrètement).
- **Taille de police à 200 %** : le texte saisi passe en colonne unique, pas de troncature.

### S10 Progrès
- Voir 4.7. **Interactions** : toucher « bijoux » ouvre S08 filtré ; toucher une barre d'exercice ouvre S08 filtré sur cet exercice. Les graphiques ont une alternative textuelle (liste) lisible par TalkBack.

### S11 Réglages / S12 À propos
- Voir 4.8. Les listes de réglages utilisent des lignes M3 avec commutateurs (Switch avec icône coche pour ne pas dépendre de la couleur).

## 7. Accessibilité transversale (rappel, détail dans design-system.md §7)
- Cibles ≥ 48 dp, texte ≥ 12 sp, corps ≥ 16 sp, saisie 18 sp, test à 200 %.
- Pas de live region seconde par seconde.
- Animations réduites : retournement de carte, progression du chrono, confettis remplacés par fondu ou rien.
- Ordre de focus logique : barre du haut, minuteur, consigne/tirage, champs, bouton.

## 8. Décisions (tranchées le 2026-10-16)

> L'utilisateur a retenu **toutes les recommandations** (D28–D42 dans l'artefact de pilotage). Correspondance : DÉCISION 1→D28, 2→D29, 3→D30, 4→D31, 5→D32, 6→D33 (les deux niveaux en base, bijou par session à l'écran en v1), 7→D34, 8→D35, 9→D36, 10→D37, 11→D38, 12→D39, 13→D40, 14→D41, 15→D42.

DÉCISION 1: pause du minuteur — (a) oui partout, (b) non, (c) uniquement exercices ≥ 3 min — recommandation (a) : le minuteur par horodatage la rend triviale, et un appel ou un arrêt de bus ne doit pas fausser la session.
DÉCISION 2: moment du démarrage du chrono — (a) toucher « Commencer », (b) à l'ouverture de l'exercice, (c) à la première frappe — recommandation (a) : on lit le tirage sans pression, puis le clavier s'ouvre en même temps que le temps.
DÉCISION 3: limite de relances — (a) compteur informatif sans plafond, (b) plafond 3, (c) relance gratuite avant « Commencer » seulement — recommandation (a) : le compteur suffit, et il garde le chrono et le texte si la relance a lieu en cours d'exercice.
DÉCISION 4: valeurs par défaut non fixées par la SPEC — durée de Status Numbers (3 ou 5 min), nombre de questions de Core Image (6 ou 8), minuteur « conseillé » de One-Page (15 min) — recommandation : 3 min, 6 questions, 15 min (proposé, à confirmer par le chercheur).
DÉCISION 5: libellé du compteur principal — (a) « sessions », (b) « séances », (c) « exercices faits » — recommandation (a), cohérent avec la SPEC et sans collision avec « Séance du jour ».
DÉCISION 6: granularité du bijou — (a) session entière en v1, (b) réponse individuelle — recommandation (a), (b) en v1.1.
DÉCISION 7: libellés des 3 niveaux — (a) Brouillon / Solide / Prêt pour la table, (b) À retravailler / Correct / Réussi, (c) émojis — recommandation (a) : formulés comme des états du produit, pas comme un verdict sur soi.
DÉCISION 8: règle du jour de grâce et définition du « jour pratiqué » — (a) 1 jour de grâce rechargé chaque lundi, jour pratiqué = 1 session avec contenu, (b) 1 jour de grâce glissant sur 7 jours, (c) autre — recommandation (a), simple à expliquer ; à valider avec l'architecte pour le calcul.
DÉCISION 9: durée de vie de la carte « Reprendre » — (a) 24 h puis session « inachevée » en historique, (b) indéfinie, (c) jamais enregistrée si vide — recommandation (a), les brouillons non vides ne sont jamais supprimés.
DÉCISION 10: nombre d'onglets — (a) 3 + Réglages en barre du haut, (b) 4 avec Réglages — recommandation (a).
DÉCISION 11: polices embarquées (Alegreya + Atkinson Hyperlegible) ou polices système — recommandation : embarquées (licences OFL, quelques centaines de Ko), repli sur serif/sans-serif système.
DÉCISION 12: mode dégradé quand la table est illisible : « Écrire mon propre sujet » — (a) oui pour exercices à tirage texte (Crazy Eights, Core Image, One-Page), (b) non — recommandation (a).
DÉCISION 13: contraintes One-Page et domaines Constraint Box — (a) plafond 5 PNJ, compteur souple « 80 caractères » par motivation, champ « si les PJ ne font rien », un seul domaine (« Lieu ») sans sélecteur en v1 ; (b) formulaire libre — recommandation (a) car la contrainte est la valeur de l'exercice ; 13b : texte modifiable depuis l'historique — non en v1.
DÉCISION 14: rythme Crazy Eights (vibration chaque minute) — (a) réglage désactivé par défaut, (b) activé, (c) absent — recommandation (a).
DÉCISION 15: partage du texte depuis S09 — (a) feuille de partage Android (sans permission), (b) copier seulement, (c) aucun — recommandation (a).

## 9. Décisions issues des maquettes (2026-10-16)

- D46 : Progrès, quand le jour de grâce a servi dans la semaine, le texte devient « Jour de grâce utilisé cette semaine · rechargé lundi » (au lieu de « disponible »).
- D47 : à 200 % avec clavier, 3 lignes de saisie visibles sont acceptées ; à vérifier sur appareil réel (lot L12).
- D48 : le panneau des 5 éléments de Connect the Story peut dépasser 35 % de l'écran **uniquement déplié à la demande** ; il est replié par défaut dès que le clavier s'ouvre. Exception documentée.
