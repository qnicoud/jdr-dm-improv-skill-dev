# DM Gym — Brief pour le maquettiste

Date : 2026-10-16. Auteur : ux-designer. Livrable attendu : maquettes HTML haute fidélité dans `docs/mockups/`.
À lire avant : `design-system.md` (tokens, composants, §8 notes de fabrication), `parcours.md` (écrans S01-S12, états).

## Consignes générales

- **Cadre** : téléphone 360 × 800 dp, portrait. Pour les écrans « En cours », dessiner un clavier de 280 dp (bloc neutre étiqueté « Clavier ») : fenêtre visible = 520 dp.
- **Thèmes** : tous les écrans en clair ; les écrans marqués (S) aussi en sombre (classe `.dark`).
- **Tokens** : reprendre exactement les hex de `design-system.md` §2 en variables CSS.
- **Polices** : Alegreya (titres), Atkinson Hyperlegible (corps), repli `Georgia` / `system-ui`. Saisie 18 sp, minimum 12 sp.
- **Cibles** : 48 dp minimum, écart 8 dp ; les afficher en mode « annotations » (option) avec contours pointillés.
- **Pas de couleur seule** : respecter le tableau §7.3 (formes, icônes, textes).
- **Interactivité minimale** : bascule clair/sombre ; sur la planche du minuteur, boutons pour passer d'un état à l'autre ; sur Connect the Story, le chevron plie/déplie. Le reste peut être statique.
- **Textes** : exactement ceux fournis (français). Pas de lorem ipsum.
- **Annotations** : un panneau à droite (hors cadre téléphone) listant pour chaque écran les rôles de couleur, les tailles et les notes d'accessibilité.
- **Version grande police** : pour 3 écrans (priorités 2, 3 et 5), ajouter une variante à **200 %** (sp ×2) en portrait avec clavier, pour valider que minuteur, consigne pliée et « Terminer » restent visibles.
- Les durées et textes sont des exemples ; ne pas imposer de logique.

## Écrans à maquetter, dans l'ordre

### 1. Accueil (S02) — clair (S)
États à montrer : (a) **normal** avec brouillon à reprendre ; (b) **vide** (0 session, premier jour).

Contenu (a) :
- Barre « DM Gym » + engrenage.
- Bandeau : « 47 sessions · série 5 jours ».
- Carte **Reprendre** : « Constraint Box · brouillon · il y a 12 min », extrait « Le bois du comptoir est tiède sous la paume… », bouton « Reprendre ».
- Carte **Séance du jour** : « Séance du 16 octobre », trois puces « Crazy Eights » (Fait), « Constraint Box », « Decision Point Drill » ; « ≈ 14 min » ; bouton « Commencer ».
- Liste « Tous les exercices » (7 cartes) :
  - Crazy Eights, design thinking, 8 min
  - Constraint Box, jazz, 5 min
  - Core Image Threading, musique, 10 min
  - Status Numbers, théâtre d'impro, 3 min
  - Decision Point Drill, science du sport, 30 s
  - Connect the Story, écriture, 10 min
  - One-Page Design, game design, Sans minuteur, étiquette « Préparation »
- Barre de navigation : S'entraîner (actif), Historique, Progrès.

Contenu (b) : bandeau « 0 session » seul ; pas de carte Reprendre ; séance du jour avec trois exercices à faire ; texte d'accueil « Choisis un exercice, ça prend cinq minutes. »

À vérifier : le compteur est le premier chiffre de l'écran, la série secondaire.

### 2. Séance du jour (S03 + S03b) — clair (S)
États : (a) **aperçu** avec Crazy Eights Fait, Constraint Box En cours, Decision Point À faire ; (b) **feuille « Remplacer par… »** ouverte sur Decision Point.

Contenu :
- Titre « Séance du 16 octobre », « 3 exercices · ≈ 14 min ».
- Lignes avec numéro, icône, nom, discipline, durée, état (icône + mot) et « Remplacer » ; « Passer cet exercice » en lien.
- Feuille : « Remplacer par… » ; Core Image Threading (10 min), Status Numbers (3 min), Connect the Story (10 min), One-Page Design (Préparation).
- Bouton « Continuer » en bas.

Variante 200 % : lignes sur 2-3 lignes sans troncature.

### 3. Constraint Box en cours (S05-2) — clair et sombre (S)
États : (a) **en cours** avec clavier, mots 1 et 2 utilisés ; (b) **Prêt** (avant « Commencer ») avec le tirage et la durée.

Contenu (a) :
- Barre : retour, « Constraint Box », « Enregistré », bouton « Terminer ».
- Zone épinglée : minuteur « 3:42 » ; carte compacte « LIEU · Auberge de relais au bord d'un gué en crue » ; 3 puces : **tiède** (toucher, case cochée « auto »), **grésillement** (ouïe, cochée), **moisi** (odorat, non cochée).
- Texte (surlignage laiton sur « tiède » et « grésillement » + soulignement) :
  « Le bois du comptoir est tiède sous la paume, comme si quelqu'un venait de s'y appuyer. Dans l'âtre, un grésillement de graisse sur la braise couvre le bruit de la crue. Personne ne lève les yeux quand j'entre, » (curseur à la fin).
- Clavier.

Contenu (b) : consigne « Décris ce lieu en n'utilisant que ces trois mots comme ancrages sensoriels. », carte de tirage avec « 0 relance » et bouton « Relancer le tirage », puce « 5 min », bouton « Commencer ».

À vérifier : la zone épinglée ne dépasse pas 35 % de la fenêtre visible ; surlignage lisible en sombre.

### 4. Crazy Eights en cours (S05-1) — clair (S)
États : (a) **en cours** 5/8 avec clavier ; (b) **vide** (aucun champ rempli) sans clavier.

Contenu (a) :
- Épinglé : minuteur « 2:18 » ; carte compacte « PROBLÈME · Les joueurs ignorent complètement l'accroche que tu avais préparée. Comment les ramener vers l'aventure ? » ; puce « 5/8 » + 8 segments (5 pleins).
- Champs numérotés visibles : 4 et 5 remplis, 6 en focus, vides ensuite :
  1. « Un PNJ vient leur demander de l'aide directement »
  2. « La bibliothèque brûle pendant qu'ils discutent »
  3. « Le roi les convoque pour leur confier une enquête »
  4. « Ils retrouvent une lettre de leur ancien mentor »
  5. « La carte qu'ils ont volée n'est pas celle qu'ils croyaient »
  6. (vide, focus) placeholder « Idée 6 »
- Clavier avec action « Suivant ».

Contenu (b) : 8 champs vides avec placeholder « Idée n », compteur « 0/8 », message discret « Ta première idée, même mauvaise. »

À vérifier : le champ focalisé reste juste sous la zone épinglée ; aucune alerte pour un champ vide.

### 5. Planche des états du minuteur (composant) — clair et sombre (S)
Une seule page avec six lignes (étiquetées) montrant le composant dans chaque état :
1. Prêt : « Prêt · 8:00 »
2. En cours : « 4:30 », barre pleine qui se vide
3. Dernière minute : « 0:58 · Dernière minute », sablier plein, barre épaisse
4. **Temps additionnel** : « +0:42 · Temps additionnel », icône « + », barre à rayures, conteneur laiton
5. En pause : « 2:10 · En pause »
6. Sans minuteur : « Sans minuteur » (icône minuteur barré)

Ajouter : une transition animée (fondu 200 ms) entre 3 et 4 déclenchée par un bouton, avec un interrupteur « Animations réduites » qui la rend instantanée. Annoter : une vibration, un bip, une annonce TalkBack « Temps écoulé. Tu peux continuer à écrire. »

Ajouter une copie en **niveaux de gris** (filtre CSS) prouvant que les six états restent distincts.

### 6. Connect the Story en cours (S05-6) — clair (S)
États : (a) **panneau déployé** avant clavier, 3 sur 5 reliés ; (b) **panneau compact** clavier ouvert ; (c) variante 200 %.

Contenu :
- Éléments :
  1. Lieu : « Un phare désaffecté » — coché (auto)
  2. Objet : « une cuillère en argent tordue » — coché (auto)
  3. Trait : « ment toujours par politesse » — non coché
  4. Détail sensoriel : « odeur de lessive chaude » — coché (à la main)
  5. Réplique : « Tu n'aurais pas dû revenir avant la marée. » — non coché
- Minuteur « 6:05 » ; compteur « 3 sur 5 reliés ».
- Texte (mots surlignés : phare, cuillère) :
  « Au phare désaffecté, la gardienne essuie une cuillère en argent tordue comme si c'était un instrument de précision. Elle m'a souri trop vite, avec cette courtoisie… »
- En compact : 5 puces (icône + début du texte + case), minuteur, clavier.
- Indiquer en annotation : coché auto = étiquette « auto » ; verrou manuel = cadenas.

À vérifier : les cinq éléments restent atteignables d'un pouce ; un élément coché par erreur se décoche en un toucher.

### 7. Status Numbers en cours (S05-4) — clair et sombre (S)
États : (a) **en cours, avant bascule** ; (b) **bandeau de bascule** affiché.

Contenu :
- Barre : « Status Numbers », « Terminer » ; minuteur « 2:05 ».
- Carte à jouer : valeur **8** en grand dans deux coins, libellé « Statut 8 sur 10 · haut ».
- Rôle : « Percepteuse des taxes du port ».
- Options actives (puces) : « Contre-stéréotype » (non), « Bascule » (oui).
- Repliable « Rappel » ouvert : « Le statut n'est pas le rang social : c'est la façon dont le personnage s'attend à être traité. » ; « Statut haut : contrôle la conversation, regard direct, suppose la coopération. » ; « Statut bas : hésite, se coupe, évite le regard. »
- (b) Bandeau laiton : « Bascule : on vient de lui apprendre que son supérieur est dans la salle. »
- Gros bouton « Scène terminée ». Pas de clavier.

À vérifier : la valeur de la carte n'est pas portée par la seule couleur ; le texte du statut est toujours écrit.

### 8. Bilan (S06) — clair et sombre (S)
États : (a) **initial** (rien sélectionné) ; (b) **rempli** : niveau « Solide » choisi, grille ouverte, bijou actif.

Contenu :
- Titre « Session enregistrée » ; récap « 6 idées · 8 min 12 s · 1 relance ».
- « Comment était cette session ? (facultatif) » : trois boutons Brouillon / Solide / Prêt pour la table.
- Repliable « Quelques questions » : « Ai-je dépassé mes 3 premières idées ? » Oui/Non ; « Une idée m'a surpris ? » Oui/Non.
- Bouton « Garder comme bijou ».
- Actions : « Terminé » (principal), « Passer » (lien). Variante séance du jour : « Exercice suivant (2 sur 3) ».

À vérifier : aucun ton évaluatif, aucune couleur d'alerte ; le sélecteur reste lisible en pile verticale à 200 %.

### 9. Historique + détail (S08 + S09) — clair et sombre (S)
États : (a) **liste** avec filtre « Bijoux » désactivé ; (b) **liste vide** ; (c) **détail** d'une session Constraint Box.

Contenu (a) :
- Puces de filtre : Tous (actif), Bijoux, Crazy Eights, Constraint Box, Core Image…
- Groupe « Aujourd'hui » : Crazy Eights · 14 h 05 · « Un PNJ vient leur demander de l'aide… » · gemme · Solide.
- Groupe « Hier » : Constraint Box · 18 h 12 · « Le bois du comptoir est tiède sous la paume… » · Prêt pour la table ; Status Numbers · 12 h 40 · « Percepteuse des taxes · statut 8 » · pas de niveau.
- Groupe « 12 octobre » : Connect the Story · 21 h 03 · « Au phare désaffecté, la gardienne… » · Brouillon · Inachevée.
Contenu (b) : « Ton carnet est vide. Fais un premier exercice : il apparaîtra ici. » + bouton « Choisir un exercice ».
Contenu (c) : en-tête « Constraint Box · hier 18 h 12 · 5 min + 0 min 42 · 0 relance » ; tirage ; texte complet avec surlignages et puces en lecture ; niveau et bijou modifiables ; boutons « Refaire avec un nouveau tirage », partage, menu (Supprimer).

À vérifier : l'aperçu ne tronque pas les infos importantes à 200 % ; badges lisibles sans couleur.

### 10. Progrès (S10) — clair (S)
États : (a) **normal** ; (b) **série rompue** ; (c) **vide**.

Contenu (a) :
- « 47 sessions » (le plus grand), « dont 6 cette semaine ».
- Série : « Série : 5 jours », « Jour de grâce disponible », rangée de 7 jours L M M J V S D avec 5 disques pleins, 1 cercle pointillé (jour de grâce), aujourd'hui entouré.
- « Meilleure série : 12 jours ».
- Répartition par exercice (barres + nombre) : Crazy Eights 12, Constraint Box 9, Connect the Story 8, Status Numbers 7, Core Image 6, Decision Point 4, One-Page 1.
- « 9 bijoux » avec lien ; « 5 h 12 d'écriture ».
Contenu (b) : « Nouvelle série : 1 jour » ; « Meilleure série : 12 jours » ; ton neutre, aucun texte de perte.
Contenu (c) : « Tes progrès apparaîtront après ta première session. » (aucun zéro).

À vérifier : le compteur de sessions est plus visible que la série ; la rangée de jours se comprend en niveaux de gris.

## Bonus (si capacité, après les 10)

- **Premier lancement (S01)** : texte et boutons de `parcours.md` §6.
- **Decision Point en cours (S05-5)** : 30 s, 3 champs avec case « Je ne ferais jamais ça », situation « Un joueur annonce qu'il brûle la bibliothèque du temple. », minuteur « 0:18 », puis variante temps additionnel « +0:07 ».
- **Core Image en cours (S05-3)** : image « Une clé rouillée à laquelle il manque une dent », « Question 3 sur 6 » : « Quels PNJ sont liés à cette clé ? », champ court, boutons Précédente / Question suivante.
- **Erreur de table (S04 X)** : carte d'erreur « Les tirages de cet exercice sont indisponibles — Rien n'est perdu : tes sessions sont intactes. » + Réessayer / Retour.
- **Réglages (S11)** : sections Minuteur, Apparence, Données, À propos.
- **Fin de séance (S07)**.

## Critères d'acceptation

1. Les 10 écrans existent avec les états demandés, en clair et en sombre quand indiqué.
2. Tokens strictement ceux de `design-system.md` ; aucun texte hors français.
3. Zone épinglée visible avec clavier ouvert sur les écrans 3, 4, 6.
4. Planche du minuteur : six états distincts, y compris en niveaux de gris.
5. Les trois variantes à 200 % montrent minuteur, consigne et « Terminer » visibles.
6. Aucune couleur d'erreur hors carte d'erreur et suppression ; aucun ton évaluatif.
